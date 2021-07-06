package kr.co.glnt.relay.tcp;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.util.CharsetUtil;
import kr.co.glnt.relay.common.BreakerActionTarget;
import kr.co.glnt.relay.common.CmdStatus;
import kr.co.glnt.relay.config.ServerConfig;
import kr.co.glnt.relay.dto.DisplayMessage;
import kr.co.glnt.relay.dto.FacilityInfo;
import kr.co.glnt.relay.dto.FacilityPayloadWrapper;
import kr.co.glnt.relay.dto.FacilityStatus;
import kr.co.glnt.relay.web.GpmsAPI;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.Charset;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@ChannelHandler.Sharable
public class GlntNettyHandler extends SimpleChannelInboundHandler<ByteBuf> {
    private static final Pattern MESSAGE = Pattern.compile("[^a-zA-Z\\s]");

    private final ServerConfig config;
    private final ObjectMapper objectMapper;
    private final GpmsAPI gpmsAPI;
    private BreakerActionTarget breakerActionTarget = BreakerActionTarget.NORMAL;




    public GlntNettyHandler(ObjectMapper objectMapper, ServerConfig config, GpmsAPI gpmsAPI) {
        this.config = config;
        this.objectMapper = objectMapper;
        this.gpmsAPI = gpmsAPI;
    }

    // TCP 연결 성공
    // GPMS 에 연결 메세지 전송.
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        FacilityInfo facilityInfo = config.findFacilityInfoByHost(ctx.channel().remoteAddress().toString().substring(1));
        log.info(">>>> {}({}) 연결 성공", facilityInfo.getFname(), facilityInfo.getDtFacilitiesId());
        gpmsAPI.sendFacilityHealth(FacilityPayloadWrapper.healthCheckPayload(
                Arrays.asList(FacilityStatus.reconnect(facilityInfo.getDtFacilitiesId()))
        ));

        // 전광판이 연결 성공시 리셋 메세지 전송.
        if (facilityInfo.getCategory().equals("DISPLAY"))
            displayActive(ctx, facilityInfo);


        if (facilityInfo.getCategory().equals("BREAKER")) {
            breakerActive(ctx);
        }

        // 재연결시 barStatus 가 lock 일 경우 다시 lock 상태로 변경.
//        if (facilityInfo.getBarStatus().equals("GATE UNLOCK OK")) {
//            char stx = 0x02, etx = 0x03;
//            String msg = String.format("%s%s%s", stx, "GATE UPLOCK", etx);
//            ByteBuf byteBuf = Unpooled.copiedBuffer(msg, Charset.forName("ASCII"));
//            ctx.channel().writeAndFlush(byteBuf);
//        }
    }

    public void displayActive(ChannelHandlerContext ctx, FacilityInfo info) {

        List<DisplayMessage.DisplayMessageInfo> messages = null;


        if(CmdStatus.getCmdStatus(info) == CmdStatus.EXIT_STANDBY){
            messages = info.getFacilityMessage();
        }
        else if(CmdStatus.getCmdStatus(info) == CmdStatus.NORMAL){
             messages = config.getDisplayResetMessage(info);
        }

        config.generateMessageList(messages).forEach(msg -> {
            ByteBuf byteBuf = Unpooled.copiedBuffer(msg, Charset.forName("euc-kr"));
            ctx.channel().writeAndFlush(byteBuf);

            log.info(">>>> {}({}) 메세지 전송: {}", info.getFname(), info.getDtFacilitiesId(), msg);
        });

    }

    public void breakerActive(ChannelHandlerContext ctx) {
        char stx = 0x02, etx = 0x03;
        String msg = String.format("%s%s%s", stx, "STATUS", etx);
        ByteBuf byteBuf = Unpooled.copiedBuffer(msg, Charset.forName("ASCII"));
        ctx.writeAndFlush(byteBuf);
    }


    // 연결 종료
    // GPMS 에 연결 종료 메세지 전송.
    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        FacilityInfo facilityInfo = config.findFacilityInfoByHost(ctx.channel().remoteAddress().toString().substring(1));
        gpmsAPI.sendFacilityHealth(FacilityPayloadWrapper.healthCheckPayload(
                Arrays.asList(FacilityStatus.deviceDisconnect(facilityInfo.getDtFacilitiesId()))
        ));
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) throws Exception {
        String host = ctx.channel().remoteAddress().toString().substring(1);

        FacilityInfo facilityInfo = config.findFacilityInfoByHost(host);

        String message = byteBufToString(msg);

        // 차단기 응답 메세지
        if (facilityInfo.getCategory().equals("BREAKER")) {
            receiveBreakerMessage(ctx.channel(), facilityInfo, message);
            return;
        }

        // 정산기 응답 메세지
        if (facilityInfo.getCategory().equals("PAYSTATION")) {
            receivePayStationMessage(facilityInfo, message);
            return;
        }

        // 전광판 헬스체크 메세지
        if (message.startsWith("![0080")) {
            return;
        }

        log.info(">>>> {}({}) 메세지 수신: {}", facilityInfo.getFname(), facilityInfo.getDtFacilitiesId(), message);

    }


    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        log.error("<!> TCP ERR remoteAddress : {}, message: {}", ctx.channel().remoteAddress(), cause.getMessage());
    }

    // Netty ByteBuf convert to string
    private String byteBufToString(ByteBuf byteBuf) {
        ByteBuf readbyte = byteBuf;
        int length = readbyte.readableBytes();
        byte[] read = new byte[length];
        for(int i=0; i<length; i++) {
            read[i] = readbyte.getByte(i);
        }
        return new String(read, CharsetUtil.UTF_8);
    }

    // 차단기에서 메세지 수신
    public void receiveBreakerMessage(Channel channel, FacilityInfo facilityInfo, String message) {
        String id = facilityInfo.getDtFacilitiesId();
        String msg = MESSAGE.matcher(message).replaceAll("");

        log.info(">>>> {}({}) 메세지 수신: {}", facilityInfo.getFname(), id, msg);

        if (facilityInfo.getGateType().contains("OUT")) {
            exitBreakerTask(facilityInfo, msg);
        } else {
            // DetectOut 을 밟았을 경우
            if (message.contains("DET OUT")) {
                // 입차 대기 큐에서 하나를 빼고
                String rebound = facilityInfo.getOpenMessageQueue().poll();
                if (rebound != null) {
                    if (facilityInfo.getOpenMessageQueue().size() > 0) {
                        ByteBuf byteBuf = Unpooled.copiedBuffer(rebound, Charset.forName("ASCII"));
                        channel.writeAndFlush(byteBuf);
                        log.info(">>>> {}({}) {}대 대기중", facilityInfo.getFname(), id, facilityInfo.getOpenMessageQueue().size());
                    }
                }
            }
        }

        // 액션이 완료 되었을때 gpms 로 상태정보 전송.
        if (msg.contains("OK")) {
            statusBreaker(msg,facilityInfo);
        }

        facilityInfo.setBarStatus(msg);


    }


    private void statusBreaker(String msg, FacilityInfo facilityInfo) {

        // 프로그램에서 접근하는것
        if(breakerActionTarget == BreakerActionTarget.NORMAL) {
            sendBreakerStatus(facilityInfo, msg);
        }

        // 수동 스위치에서 UPLOCK
        if(msg.contains("EXUPLOCK")) {
            breakerActionTarget = BreakerActionTarget.EX;
            sendBreakerStatus(facilityInfo, msg);
        }

        // 스위치 UPLOCK
        if(msg.contains("SWUPLOCKOKGATE")) {
            breakerActionTarget = BreakerActionTarget.SW;
            sendBreakerStatus(facilityInfo, msg);
        }

        // UNLOCK 요청일때
        if (msg.contains("UNLOCK")) {
            breakerActionTarget = BreakerActionTarget.NORMAL;
            sendBreakerStatus(facilityInfo, msg);
        }


    }


    private void exitResetMessage(FacilityInfo facilityInfo, Charset charset){

        facilityInfo.setPassCount(0);

        List<FacilityInfo> facilityList = config.getFacilityList();

        FacilityInfo display = facilityList
                .stream()
                .filter(facility -> facility.getGateId().equals(facilityInfo.getGateId()))
                .filter(facilityGateId -> facilityGateId.getCategory().equals("DISPLAY"))
                .findFirst().get();

        List<DisplayMessage.DisplayMessageInfo> displayResetMessage = config.getDisplayResetMessage(facilityInfo);
        List<String> messageList = config.generateMessageList(displayResetMessage);

        messageList.forEach(message -> {
            sendMessage(display.generateHost(),message,charset);
        });

        display.setCmdStatus(CmdStatus.NORMAL);

    }

    private void sendMessage(String host, String message,Charset charset){

        Map<String, Channel> channelMap = GlntNettyClient.getChannelMap();

        if (!channelMap.containsKey(host)) {
            log.info("<!> channel is not found {}", host);
            return;
        }

        ByteBuf byteBuf = Unpooled.copiedBuffer(message, charset);
        Channel channel = channelMap.get(host);
        channel.writeAndFlush(byteBuf);
    }





    // 출차 차단기 작업.
    private void exitBreakerTask(FacilityInfo facilityInfo, String msg) {
        new Thread(() -> {
            // 정상적으로 게이트가 올라갔을 경우 시설물 고장이 아님
            if (msg.contains("GATE UP OK") || !(facilityInfo.getBarStatus().contains("GATE DOWN"))) {
                exitResetMessage(facilityInfo,Charset.forName("euc-kr"));
                return;
            }

            //  출차 디텍트를 밟았을때 (DET OUT GATE DOWN ACTION)
            if (msg.contains("DET OUT")) {
                exitResetMessage(facilityInfo,Charset.forName("euc-kr"));
                String barStatus = facilityInfo.getBarStatus();
                // 게이트 게이트 상태가 내려가져있는 상황일때.
                // 4대 이상 차량이 출차될 경우 알람 발생
                if (barStatus.equals("GATE DOWN OK")) {
                    int passCount = facilityInfo.getPassCount() + 1;
                    if (passCount > 3) {
                        List<FacilityStatus> alarmList = Arrays.asList(
                                FacilityStatus.gateBarDamageDoubt(facilityInfo.getDtFacilitiesId())
                        );
                        gpmsAPI.sendFacilityAlarm(FacilityPayloadWrapper.facilityAlarmPayload(alarmList));
                        passCount = 0;
                    }

                    facilityInfo.setPassCount(passCount);
                }
            }

        }).start();
    }

    public void sendBreakerStatus(FacilityInfo info, String msg) {
        String sendMsg = "";
        if (msg.contains("GATE UPLOCK OK")) {
            sendMsg = "UPLOCK";
        } else if (msg.contains("GATE UNLOCK OK")) {
            sendMsg = "UNLOCK";
        } else if (msg.contains("GATE UP OK")) {
            sendMsg = "UP";
        } else if (msg.contains("GATE DOWN OK")) {
            sendMsg = "DOWN";
        } else if (msg.contains("SCAN OK")) {
            sendMsg = "SCAN";
        } else if (msg.contains("UPLOCKOK")){
            sendMsg = "XUPLOCK";
        } else if (msg.contains("UNLOCKOK")){
            sendMsg = "UNLOCK";
        }


        if (!sendMsg.isEmpty()) {
            List<FacilityStatus> status = Arrays.asList(
                    FacilityStatus.breakerAction(info.getDtFacilitiesId(), sendMsg)
            );
            gpmsAPI.sendStatusNoti(FacilityPayloadWrapper.healthCheckPayload(status));
        }

    }

    // 정산기에서 메세지 수신
    @SneakyThrows
    public void receivePayStationMessage(FacilityInfo facilityInfo, String message) {
        Map<String, Object> receiveData = objectMapper.readValue(message, new TypeReference<Map<String, Object>>() {});
        String type = Objects.toString(receiveData.get("type"), "");

        switch (type) {
            case "vehicleListSearch": // 차량 목록 조회
                // GPMS 에 토스
                log.info(">>>> 정산기({}) 차량 목록 조회 메세지 수신: {}", facilityInfo.getDtFacilitiesId(), message);
                gpmsAPI.searchVehicle(facilityInfo.getDtFacilitiesId(), message);
                break;
            case "adjustmentRequest": // 정산 요청 응답
                log.info(">>>> 정산기({}) 정산 요청 메세지 응답 수신: {}", facilityInfo.getDtFacilitiesId(), message);
                gpmsAPI.sendPayment(facilityInfo.getDtFacilitiesId(), message);
                break;
            case "payment": // 결제 응답 (결제 결과)
                log.info(">>>> 정산기({}) 결제 응답 메세지 수신: {}", facilityInfo.getDtFacilitiesId(), message);
                gpmsAPI.sendPaymentResponse(facilityInfo.getDtFacilitiesId(), message);
                break;
            case "healthCheck":
                log.info(">>>> 정산기({}) 헬스체크 메세지 응답 수신: {}", facilityInfo.getDtFacilitiesId(), message);
                Map<String, String> contents = objectMapper.convertValue(receiveData.get("contents"), new TypeReference<Map<String, String>>(){});

                // 정산기 연결 상태
                String payStationStatus = Objects.toString(contents.get("paymentFailure"), "");

                // 카드 리더기 연결 상태
                String cardReaderStatus = Objects.toString(contents.get("icCardReaderFailure"), "");
                gpmsAPI.sendFacilityHealth(FacilityPayloadWrapper.healthCheckPayload(
                        Arrays.asList(
                                FacilityStatus.payStationStatus(facilityInfo.getDtFacilitiesId(), payStationStatus),
                                FacilityStatus.icCardReaderStatus(facilityInfo.getDtFacilitiesId(), cardReaderStatus)
                        )
                ));

                break;

            case "paymentFailure":
                log.info(">>>> 정산 실패: {}", message);
                break;
            default:
                log.info(">>>> 정산기 메세지 수신: {}", message);
                break;
        }
    }
}
