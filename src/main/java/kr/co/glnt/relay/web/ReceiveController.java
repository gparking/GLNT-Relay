package kr.co.glnt.relay.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.channel.Channel;
import kr.co.glnt.relay.dto.DisplayMessage;
import kr.co.glnt.relay.dto.FacilityInfo;
import kr.co.glnt.relay.exception.GlntBadRequestException;
import kr.co.glnt.relay.config.ServerConfig;
import kr.co.glnt.relay.tcp.GlntNettyClient;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;

import java.net.SocketAddress;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@RestController
public class ReceiveController {

    private final GlntNettyClient client;
    private final ServerConfig serverConfig;
    private final SimpMessagingTemplate simpMessagingTemplate;
    private final ObjectMapper objectMapper;

    public ReceiveController(GlntNettyClient client, ServerConfig serverConfig,
                             SimpMessagingTemplate simpMessagingTemplate, ObjectMapper objectMapper) {
        this.client = client;
        this.serverConfig = serverConfig;
        this.simpMessagingTemplate = simpMessagingTemplate;
        this.objectMapper = objectMapper;
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
            client.sendMessage(facilityInfo.generateHost(), msg);
        });
    }


    /**
     * 게이트 차단기 명령
     */
    @GetMapping("/v1/breaker/{facilityId}/{command}")
    public void breakerBarOpenTask(@PathVariable("facilityId") String facilityId,
                                   @PathVariable("command") String breakerCommand) throws GlntBadRequestException {
        FacilityInfo facilityInfo = serverConfig.findByFacilitiesId(facilityId);
        Map<String, String> commandMap = serverConfig.getBreakerCommand();
        String command = commandMap.get(breakerCommand);
        if (Objects.isNull(command))
            throw new GlntBadRequestException("잘못된 명령어입니다.");

        client.sendMessage(facilityInfo.generateHost(), String.format("0x02%s0x03", command));
    }


    // TODO: WebSocket 첫 연결 시 디바이스 상태 정보 알려줘야함
    @SneakyThrows
    @MessageMapping("/status-list")
    public void connection() {
        Map<String, Channel> channelMap = GlntNettyClient.getChannelMap();
        List<FacilityInfo> list = channelMap.values().stream()
                .map(channel -> {
                    boolean channelActive = channel.isActive();
                    FacilityInfo info = serverConfig.findFacilityInfoByPort(channel.remoteAddress().toString());
                    info.setState(channelActive);
                    return info;
                }).collect(Collectors.toList());
        simpMessagingTemplate.convertAndSend("/subscribe/status-list", objectMapper.writeValueAsString(list));
    }
}
