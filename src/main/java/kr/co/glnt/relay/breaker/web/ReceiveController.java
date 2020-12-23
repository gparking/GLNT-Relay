package kr.co.glnt.relay.breaker.web;

import kr.co.glnt.relay.breaker.dto.DisplayMessage;
import kr.co.glnt.relay.breaker.dto.FacilityInfo;
import kr.co.glnt.relay.breaker.exception.GlntBadRequestException;
import kr.co.glnt.relay.common.config.ServerConfig;
import kr.co.glnt.relay.common.dto.ResponseDTO;
import kr.co.glnt.relay.tcp.client.BreakerClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;

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
    public void showDisplay(@RequestBody DisplayMessage message) throws GlntBadRequestException {
        FacilityInfo facilityInfo = serverConfig.findByFacilitiesId(message.getFacilityId());
        List<String> messageList = message.generateMessageList();
        messageList.forEach( msg -> {
            log.info("display msg: {}", msg);
            client.sendMessage(facilityInfo.getIp(), msg);
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

        client.sendMessage(facilityInfo.getIp(), String.format("0x02%s0x03", result));
    }
}
