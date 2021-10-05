package kr.co.glnt.relay.service;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import kr.co.glnt.relay.common.CmdStatus;
import kr.co.glnt.relay.config.ServerConfig;
import kr.co.glnt.relay.dto.DisplayMessage;
import kr.co.glnt.relay.dto.FacilityInfo;
import kr.co.glnt.relay.dto.FacilityPayloadWrapper;
import kr.co.glnt.relay.dto.FacilityStatus;
import kr.co.glnt.relay.tcp.GlntNettyClient;
import kr.co.glnt.relay.web.GpmsAPI;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.nio.charset.Charset;
import java.util.*;
@Slf4j
@Service
public class DisplayService {
    private final ServerConfig serverConfig;
    // 아래 고정 메세지 포맷 "![000/P0001/Y0408/%s%s!]"
    // 아래 흐르는 메세지 포맷 "![000/P0001/S1000/Y0408/E0606/%s%s!]"
    private List<String> messageFormat = Arrays.asList("", "![000/P0000/Y0004/%s%s!]", "![000/P0001/Y0408/%s%s!]");
    private Map<String, Timer> displayTimer;
    private int timerTime;
    private GlntNettyClient client;

    public DisplayService(ServerConfig serverConfig) {
        this.serverConfig = serverConfig;
        this.displayTimer = new HashMap<>();
    }

    public void setMessageFormat(String type) {
        switch (type) {
            case "fixed":
                messageFormat = Arrays.asList("", "![000/P0000/Y0004/%s%s!]", "![000/P0001/Y0408/%s%s!]");
                break;
            case "flow":
                messageFormat = Arrays.asList("", "![000/P0000/Y0004/%s%s!]", "![000/P0001/S1000/Y0408/E0606/%s%s!]");
                break;
        }
        log.info(">>>> {}로 메세지 타입 변경", type);
    }

    /**
     *  리셋을 안하는 타이머 만들기
     *
     */
    public void sendDisplayMessage(DisplayMessage message) {
        // 시설물 정보 가져오기.
        FacilityInfo facilityInfo = serverConfig.findByFacilitiesId("전광판", message.getDtFacilityId());


        facilityInfo.setFacilityMessage(message.getMessages());

        // 메세지 추출
        List<String> messageList = serverConfig.generateMessageList(message.getMessages());

        // 메세지 보내기
        sendMessage(facilityInfo, messageList);

        // 리셋 타이머 설정하기.
        startDisplayResetTimer(facilityInfo, message.getReset());

    }
//    // 전광판 메세지 리스트 생성하기.
//    public List<String> generateMessageList(List<DisplayMessage.DisplayMessageInfo> messages) {
//        Collections.sort(messages, (d1, d2) -> d1.getOrder() > d2.getOrder() ? 1 : -1);
//        List<String> messageList = new ArrayList<>();
//        for (int j = 0; j < messages.size(); j++) {
//            DisplayMessage.DisplayMessageInfo info = messages.get(j);
//            String message = String.format(messageFormat.get(info.getLine()), info.getColor(), info.getText());
//            messageList.add(message);
//        }
//        return messageList;
//    }
// 메세지 전송.
    public void sendMessage(FacilityInfo facilityInfo, List<String> messageList) {

        Map<String, Channel> channelMap = GlntNettyClient.getChannelMap();

        messageList.forEach(msg -> {
            log.info(">>>> {}({}) 메세지 전송: {}", facilityInfo.getFname(), facilityInfo.getDtFacilitiesId(), msg);

            String host = facilityInfo.generateHost();

            GlntNettyClient.sendMessage(host,msg,Charset.forName("euc-kr"));

        });
    }

    // 전광판 메세지 리셋 기능.
    public void startDisplayResetTimer(FacilityInfo facilityInfo, String reset) {
        if (displayTimer.containsKey(facilityInfo.getDtFacilitiesId())) {
            Timer timer = displayTimer.get(facilityInfo.getDtFacilitiesId());
            timer.cancel();
            timer = null;
        }

        long delay = 0;

        switch (reset){
            case "on":
                delay = 7 * 1000;
                break;
            case "off":
                facilityInfo.setCmdStatus(CmdStatus.EXIT_STANDBY);
                delay = 120 * 1000;
                break;
            default:
                break;
        }


        displayTimer.put(facilityInfo.getDtFacilitiesId(), resetDisplayTimer(facilityInfo, delay));
    }

    // 전광판 리셋.
    private Timer resetDisplayTimer(FacilityInfo facilityInfo, long delay) {
        // 새로운 타이머 생성
        Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            List<DisplayMessage.DisplayMessageInfo> messages = serverConfig.getDisplayResetMessage(facilityInfo);
            @Override
            public void run() {

                sendMessage(facilityInfo, serverConfig.generateMessageList(messages));
                displayTimer.remove(facilityInfo.getDtFacilitiesId());
                facilityInfo.setCmdStatus(CmdStatus.NORMAL);
            }
        }, delay);

        return timer;
    }


    public void displayActive(ChannelHandlerContext ctx, FacilityInfo info){

        List<DisplayMessage.DisplayMessageInfo> messages = null;

        if(CmdStatus.getCmdStatus(info) == CmdStatus.EXIT_STANDBY){
            messages = info.getFacilityMessage();
        }
        else if(CmdStatus.getCmdStatus(info) == CmdStatus.NORMAL){
            messages = serverConfig.getDisplayResetMessage(info);
        }

        Timer timer = new Timer();

        List<DisplayMessage.DisplayMessageInfo> finalMessage = messages;

        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                if(info.getCmdStatus().equals(CmdStatus.EXIT_STANDBY)){
                    if(!(info.getFacilityMessage().get(0).getText().equals(finalMessage.get(0).getText()))
                            && info.getFacilityMessage().get(1).getText().equals(finalMessage.get(1).getText())){

                        log.info("<!> reconnect message Error");

                        List<DisplayMessage.DisplayMessageInfo> messageInfos =  serverConfig.getDisplayResetMessage(info);

                        serverConfig.generateMessageList(messageInfos).forEach(msg ->  {
                            ByteBuf byteBuf = Unpooled.copiedBuffer(msg,Charset.forName("euc-kr"));
                            ctx.channel().writeAndFlush(byteBuf);
                        });
                    }
                }
            }
        },timerTime);

        serverConfig.generateMessageList(messages).forEach(msg -> {
            ByteBuf byteBuf = Unpooled.copiedBuffer(msg, Charset.forName("euc-kr"));
            ctx.channel().writeAndFlush(byteBuf);

            log.info(">>>> {}({}) 메세지 전송: {}", info.getFname(), info.getDtFacilitiesId(), msg);
        });
    }

    public void exitResetMessage(FacilityInfo facilityInfo){

        facilityInfo.setPassCount(0);

        List<FacilityInfo> facilityList = serverConfig.getFacilityList();

        FacilityInfo display = facilityList
                .stream()
                .filter(facility -> facility.getGateId().equals(facilityInfo.getGateId()))
                .filter(facilityGateId -> facilityGateId.getCategory().equals("DISPLAY"))
                .findFirst().get();


        List<DisplayMessage.DisplayMessageInfo> displayResetMessage = serverConfig.getDisplayResetMessage(facilityInfo);
        List<String> messageList = serverConfig.generateMessageList(displayResetMessage);


        sendMessage(display,messageList);

        display.setCmdStatus(CmdStatus.NORMAL);
    }









}