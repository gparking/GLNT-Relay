package kr.co.glnt.relay.breaker.web;

import kr.co.glnt.relay.breaker.dto.DisplayMessage;
import kr.co.glnt.relay.breaker.dto.FacilityInfo;
import kr.co.glnt.relay.common.config.ServerConfig;
import kr.co.glnt.relay.tcp.client.BreakerClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.NoSuchElementException;

@Slf4j
@RestController
public class ReceiveController {

    private final BreakerClient client;
    private final ServerConfig serverConfig;

    public ReceiveController(BreakerClient client, ServerConfig serverConfig) {
        this.client = client;
        this.serverConfig = serverConfig;
    }

    /**
     * display 메세지 표시
     * @param message
     */
    @PostMapping("/v1/display/show")
    public void showDisplay(@RequestBody DisplayMessage message) {
        FacilityInfo facilityInfo = serverConfig.getFacilityList()
                .stream()
                .filter(info->info.getFacilitiesId().equals(message.getFacilityId()))
                .findFirst()
                .orElseThrow(() -> new NoSuchElementException("시설 아이디와 동일 정보가 없습니다."));

        List<String> messageList = message.generateMessageList();
        messageList.forEach( msg -> {
            log.info("display msg: {}", msg);
            client.sendMessage(facilityInfo.getIp(), msg);
        });
    }
}
