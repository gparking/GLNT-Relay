package kr.co.glnt.relay.service;

import kr.co.glnt.relay.config.ServerConfig;
import kr.co.glnt.relay.dto.DisplayMessage;
import kr.co.glnt.relay.dto.FacilityInfo;
import kr.co.glnt.relay.tcp.GlntNettyClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.nio.charset.Charset;
import java.util.*;

@Slf4j
@Service
public class DisplayService {
    private final ServerConfig serverConfig;
    private final GlntNettyClient client;


    private Map<String, Timer> displayTimer;

    public DisplayService(ServerConfig serverConfig, GlntNettyClient client) {
        this.serverConfig = serverConfig;
        this.client = client;
        this.displayTimer = new HashMap<>();
    }



    public void sendDisplayMessage(DisplayMessage message) {
        // 시설물 정보 가져오기.
        FacilityInfo facilityInfo = serverConfig.findByFacilitiesId("전광판", message.getDtFacilityId());

        // 메세지 추출
        List<String> messageList = serverConfig.generateMessageList(message.getMessages());

        // 메세지 보내기
        sendMessage(facilityInfo, messageList);

        // 리셋 타이머 설정하기.
        startDisplayResetTimer(facilityInfo, message.getReset());

        List<String> messageList = new ArrayList<>();
        for (int j = 0; j < messages.size(); j++) {
            DisplayMessage.DisplayMessageInfo info = messages.get(j);

            String format = serverConfig.getDisplayFormat().get(info.getLine());

            String message = String.format(format, info.getColor(), info.getText());

            messageList.add(message);
        }

        return messageList;
    }

    // 메세지 전송.
    public void sendMessage(FacilityInfo facilityInfo, List<String> messageList) {
        messageList.forEach(msg -> {
                    log.info(">>>> {}({}) 메세지 전송: {}", facilityInfo.getFname(), facilityInfo.getDtFacilitiesId(), msg);
                    client.sendMessage(facilityInfo.generateHost(), msg, Charset.forName("euc-kr"));
                }
        );
    }

    // 전광판 메세지 리셋 기능.
    public void startDisplayResetTimer(FacilityInfo facilityInfo, String reset) {
        if (displayTimer.containsKey(facilityInfo.getDtFacilitiesId())) {
            Timer timer = displayTimer.get(facilityInfo.getDtFacilitiesId());
            timer.cancel();
            timer = null;
        }


        long delay = "on".equals(reset)
                ? 5 * 1000
                : 10 * 1000;

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
            }
        }, delay);

        return timer;
    }
}
