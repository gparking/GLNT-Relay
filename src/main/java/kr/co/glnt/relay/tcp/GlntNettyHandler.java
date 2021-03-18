package kr.co.glnt.relay.tcp;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.util.CharsetUtil;
import kr.co.glnt.relay.config.ApplicationContextProvider;
import kr.co.glnt.relay.config.ServerConfig;
import kr.co.glnt.relay.dto.FacilityInfo;
import kr.co.glnt.relay.dto.FacilityPayloadWrapper;
import kr.co.glnt.relay.dto.FacilityStatus;
import kr.co.glnt.relay.web.GpmsAPI;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.Charset;
import java.util.*;
import java.util.regex.Pattern;

@Slf4j
@ChannelHandler.Sharable
public class GlntNettyHandler extends SimpleChannelInboundHandler<ByteBuf> {
    private static final Pattern MESSAGE = Pattern.compile("[^a-zA-Z\\s]");

    private final ServerConfig config;
    private final ObjectMapper objectMapper;
    private final GpmsAPI gpmsAPI;

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


        // 서버가 구동되고 첫 연결시에는 barStatus 가 null 이므로 여기서 함수 종료
        if (Objects.isNull(facilityInfo.getBarStatus())) {
            return;
        }

        // 재연결시 barStatus 가 lock 일 경우 다시 lock 상태로 변경.
        if (facilityInfo.getBarStatus().equals("GATE UNLOCK OK")) {
            char stx = 0x02, etx = 0x03;
            String msg = String.format("%s%s%s", stx, "GATE UPLOCK", etx);
            ByteBuf byteBuf = Unpooled.copiedBuffer(msg, Charset.forName("ASCII"));
            ctx.channel().writeAndFlush(byteBuf);
        }
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
        if (facilityInfo.getFname().contains("차단기")) {
            receiveBreakerMessage(ctx.channel(), facilityInfo, message);
            return;
        }

        // 정산기 응답 메세지
        if (facilityInfo.getFname().contains("정산기")) {
            receivePayStationMessage(facilityInfo, message);
            return;
        }

        log.info(">>>> {}({}) 메세지 수신: {}", facilityInfo.getFname(), facilityInfo.getDtFacilitiesId(), message);
    }


    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        log.error("TCP ERR => remoteAddress : {}", ctx.channel().remoteAddress(), cause);
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

        // TODO: 차단기 리셋시 리셋 전 상태값을 가져와 uplock 일경우 uplock 으로 변경해주기.

        if (facilityInfo.getFname().equals("출구")) {
            exitBreakerTask(facilityInfo, msg);
        } else {
            // DetectOut 을 밟았을 경우
            if (message.contains("DET OUT")) {
                // 입차 대기 중인 차량이 있는지 확인 후 있을 경우
                if (facilityInfo.getOpenMessageQueue().size() > 1) {
                    ByteBuf byteBuf = Unpooled.copiedBuffer(facilityInfo.getOpenMessageQueue().poll(), Charset.forName("ASCII"));
                    channel.writeAndFlush(byteBuf);
                    log.info(">>>> {}({}) {}대 대기중", facilityInfo.getFname(), id, facilityInfo.getOpenMessageQueue().size());
                }
            }
        }

        // 액션이 완료 되었을때 gpms 로 상태정보 전송.
        if (msg.contains("OK")) {
            sendBreakerStatus(facilityInfo, msg);
        }

        facilityInfo.setBarStatus(msg);
    }

    // 출차 차단기 작업.
    private void exitBreakerTask(FacilityInfo facilityInfo, String msg) {
        new Thread(() -> {
            // 정상적으로 게이트가 올라갔을 경우 시설물 고장이 아님
            if (msg.contains("GATE UP OK")) {
                facilityInfo.setPassCount(0);
                return;
            }

            //  출차 디텍트를 밟았을때 (DET OUT GATE DOWN ACTION)
            if (msg.contains("DET OUT")) {
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
