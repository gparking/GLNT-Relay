package kr.co.glnt.relay.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import kr.co.glnt.relay.config.ServerConfig;
import kr.co.glnt.relay.dto.*;
import kr.co.glnt.relay.exception.GlntBadRequestException;
import kr.co.glnt.relay.tcp.GlntNettyClient;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Slf4j
@RestController
public class ReceiveController {

    private final GlntNettyClient client;
    private final ServerConfig serverConfig;
    private final ObjectMapper objectMapper;
    private final GpmsAPI gpmsAPI;

    public ReceiveController(GlntNettyClient client, ServerConfig serverConfig,
                             ObjectMapper objectMapper, GpmsAPI gpmsAPI) {
        this.client = client;
        this.serverConfig = serverConfig;
        this.objectMapper = objectMapper;
        this.gpmsAPI = gpmsAPI;
    }

    // 서버 재시작.
    @GetMapping("/v1/parkinglot/facility/refresh")
    public void facilityInfoRefresh() {
        List<FacilityInfo> list = gpmsAPI.getParkinglotData(new FacilityInfoPayload(serverConfig.getServerKey()));
        log.info("list : {}", list);
        serverConfig.setFacilityList(list);
    }

    /**
     * display 메세지 표시
     *
     * @param message
     */
    @PostMapping("/v1/display/show")
    public void showDisplay(@RequestBody DisplayMessage message) {
        FacilityInfo facilityInfo = serverConfig.findByFacilitiesId(message.getFacilityId());
        List<String> messageList = message.generateMessageList();
        messageList.forEach(msg ->
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
                                   @PathVariable("command") String breakerCommand) {
        log.info("facilityInfo : {}", serverConfig.getFacilityList());
        log.info("facilityID : {}", facilityId);

        FacilityInfo facilityInfo = serverConfig.findByFacilitiesId(facilityId);
        Map<String, String> commandMap = serverConfig.getBreakerCommand();
        String command = commandMap.get(breakerCommand);
        if (Objects.isNull(command))
            throw new GlntBadRequestException("잘못된 명령어입니다.");

        char stx = 0x02;
        char etx = 0x03;
        client.sendMessage(facilityInfo.generateHost(), String.format("%s%s%s", stx, command, etx), Charset.forName("ASCII"));
    }


    // 연결된 전체 디바이스 상태정보 조회.
    @GetMapping("/v1/device/health")
    public ResponseEntity<ResponseDTO> fullDeviceStatusLookup() {
        return ResponseEntity.ok(
                new ResponseDTO(client.getChannelsStatus())
        );
    }

    // 시설물 아이디로 디바이스 상태정보 조회
    @GetMapping("/v1/device/health/{facilityID}")
    public ResponseEntity<ResponseDTO> deviceHealthCheck(@PathVariable("facilityID") String facilityID) {
        return ResponseEntity.ok(
                new ResponseDTO(client.getChannelStatus(facilityID))
        );
    }

    // LPR 전체 ping check
    @GetMapping("/v1/lpr/health")
    public ResponseEntity<ResponseDTO> fullLprStatusLookup() {
        return ResponseEntity.ok(
                new ResponseDTO((client.getFullLprStatus()))
        );
    }

    // 특정 LPR ping check
    @GetMapping("/v1/lpr/health/{facilityID}")
    public ResponseEntity<ResponseDTO> lprStatusLookup(@PathVariable("facilityID") String facilityID) {
        return ResponseEntity.ok(
                new ResponseDTO(Arrays.asList(client.getLrpStatus(facilityID)))
        );
    }
}
