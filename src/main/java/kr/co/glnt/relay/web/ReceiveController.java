package kr.co.glnt.relay.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.channel.Channel;
import kr.co.glnt.relay.config.ServerConfig;
import kr.co.glnt.relay.dto.DisplayMessage;
import kr.co.glnt.relay.dto.FacilityInfo;
import kr.co.glnt.relay.dto.FacilityInfoPayload;
import kr.co.glnt.relay.dto.PayStationInfo;
import kr.co.glnt.relay.exception.GlntBadRequestException;
import kr.co.glnt.relay.tcp.GlntNettyClient;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.Charset;
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
    private final GpmsAPI gpmsAPI;

    public ReceiveController(GlntNettyClient client, ServerConfig serverConfig,
                             SimpMessagingTemplate simpMessagingTemplate, ObjectMapper objectMapper, GpmsAPI gpmsAPI) {
        this.client = client;
        this.serverConfig = serverConfig;
        this.simpMessagingTemplate = simpMessagingTemplate;
        this.objectMapper = objectMapper;
        this.gpmsAPI = gpmsAPI;
    }

    @GetMapping("/v1/parkinglot/facility/refresh")
    public void facilityInfoRefresh() {
        List<FacilityInfo> list = gpmsAPI.getParkinglotData(new FacilityInfoPayload(serverConfig.getServerName()));
        log.info("list : {}", list);
        serverConfig.setFacilityList(list);
    }

    /**
     * display 메세지 표시
     * @param message
     */
    @PostMapping("/v1/display/show")
    public void showDisplay(@RequestBody DisplayMessage message) throws GlntBadRequestException {
        FacilityInfo facilityInfo = serverConfig.findByFacilitiesId(message.getFacilityId());
        List<String> messageList = message.generateMessageList();
        messageList.forEach( msg ->
            client.sendMessage(facilityInfo.generateHost(), msg, Charset.forName("euc-kr"))
        );
    }

    /**
     * 정산기 메세지 명령
     */
    @SneakyThrows
    @PostMapping("/v1/parkinglot/paystation")
    public void parkingCostCalculator(@RequestBody PayStationInfo payStationInfo) {
        log.info("payStationInfo : {}", objectMapper.writeValueAsString(payStationInfo));

        client.sendMessage(payStationInfo.getFacilityId(), objectMapper.writeValueAsString(payStationInfo.getData()), Charset.forName("ASCII"));
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

        char stx = 0x02;
        char etx = 0x03;
        client.sendMessage(facilityInfo.generateHost(), String.format("%s%s%s", stx, command, etx), Charset.forName("ASCII"));
    }


    @SneakyThrows
    @MessageMapping("/status-list")
    public void connection() {
        Map<String, Channel> channelMap = GlntNettyClient.getChannelMap();
        List<FacilityInfo> list = channelMap.values().stream()
                .map(channel -> {
                    boolean channelActive = channel.isActive();
                    String remote = channel.remoteAddress().toString();
                    String host = remote.substring(0, remote.indexOf('/')) + remote.substring(remote.indexOf(':'));
                    // TODO: 나중엔 remoteAddress 로 변경
                    FacilityInfo info = serverConfig.findFacilityInfoByHost(host);
                    info.setState(channelActive);
                    return info;
                }).collect(Collectors.toList());
        simpMessagingTemplate.convertAndSend("/subscribe/status-list", objectMapper.writeValueAsString(list));
    }
}
