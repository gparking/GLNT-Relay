package kr.co.glnt.relay.breaker.web;

import kr.co.glnt.relay.breaker.dto.DisplayMessage;
import kr.co.glnt.relay.breaker.dto.FacilityInfo;
import kr.co.glnt.relay.breaker.exception.GlntBadRequestException;
import kr.co.glnt.relay.common.config.ServerConfig;
import kr.co.glnt.relay.tcp.GlntNettyClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Objects;

@Slf4j
@RestController
public class ReceiveController {

    private final GlntNettyClient client;
    private final ServerConfig serverConfig;

    public ReceiveController(GlntNettyClient client, ServerConfig serverConfig) {
        this.client = client;
        this.serverConfig = serverConfig;
    }

    /**
     * display 메세지 표시
     * @param message
     */
    @PostMapping("/v1/display/show")
    public void showDisplay(@RequestBody DisplayMessage message) throws GlntBadRequestException {
        FacilityInfo facilityInfo = serverConfig.findByFacilitiesId(message.getFacilityId());
        List<String> messageList = message.generateMessageList();
        messageList.forEach( msg -> {
            client.sendMessage(facilityInfo.getHost(), msg);
        });
    }


    /**
     * 게이트 차단기 명령
     */
    @GetMapping("/v1/breaker/{facilityId}/{command}")
    public void breakerBarOpenTask(@PathVariable("facilityId") String facilityId,
                                   @PathVariable("command") String command) throws GlntBadRequestException {
        FacilityInfo facilityInfo = serverConfig.findByFacilitiesId(facilityId);
        Map<String, String> commandMap = serverConfig.getBreakerCommand();
        String result = commandMap.get(command);
        if (Objects.isNull(result))
            throw new GlntBadRequestException("잘못된 명령어입니다.");

        client.sendMessage(facilityInfo.getHost(), String.format("0x02%s0x03", result));
    }
}
