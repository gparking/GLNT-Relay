package kr.co.glnt.relay.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import kr.co.glnt.relay.config.ServerConfig;
import kr.co.glnt.relay.dto.*;
import kr.co.glnt.relay.exception.GlntBadRequestException;
import kr.co.glnt.relay.service.DisplayService;
import kr.co.glnt.relay.tcp.GlntNettyClient;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.Charset;
import java.util.*;

@Slf4j
@RestController
public class ReceiveController {
    private final GlntNettyClient client;
    private final ServerConfig serverConfig;
    private final ObjectMapper objectMapper;
    private final GpmsAPI gpmsAPI;
    private final DisplayService displayService;



    public ReceiveController(GlntNettyClient client, ServerConfig serverConfig,
                             ObjectMapper objectMapper, GpmsAPI gpmsAPI, DisplayService displayService) {
        this.client = client;
        this.serverConfig = serverConfig;
        this.objectMapper = objectMapper;
        this.gpmsAPI = gpmsAPI;
        this.displayService = displayService;
    }

    // 설정 정보 리프레시
    @GetMapping("/v1/parkinglot/facility/refresh")
    public void facilityInfoRefresh() {
        List<FacilityInfo> list = gpmsAPI.getParkinglotData(new FacilityInfoPayload(serverConfig.getServerKey()));
        log.debug("list : {}", list);
        serverConfig.setFacilityList(list);
    }

    /**
     * display 메세지 표시
     *
     * @param message
     */
    @PostMapping("/v1/display/show")
    public void showDisplay(@RequestBody DisplayMessage message) {
        displayService.sendDisplayMessage(message);
    }



    /**
     * 정산기 메세지 명령
     */
    @SneakyThrows
    @PostMapping("/v1/parkinglot/paystation")
    public void parkingCostCalculator(@RequestBody PayStationInfo payStationInfo) {
        FacilityInfo facilityInfo = serverConfig.findByFacilitiesId("정산기", payStationInfo.getDtFacilityId());
        String msg = objectMapper.writeValueAsString(payStationInfo.getData());
        log.info(">>>> {}({}) 메세지 전송: {}", facilityInfo.getFname(), facilityInfo.getDtFacilitiesId(), msg);
        client.sendMessage(facilityInfo.generateHost(), msg, Charset.forName("UTF-8"));
    }

    /**
     * 게이트 차단기 명령
     */
    @GetMapping(value = {"/v1/breaker/{dtFacilityId}/{command}", "/v1/breaker/{dtFacilityId}/{command}/{manual}"})
    public void breakerBarOpenTask(@PathVariable("dtFacilityId") String dtFacilityId,
                                   @PathVariable("command") String breakerCommand,
                                   @PathVariable(required = false, name = "manual") String manual) {
        FacilityInfo facilityInfo = serverConfig.findByFacilitiesId("차단기", dtFacilityId);
        Map<String, String> commandMap = serverConfig.getBreakerCommand();
        String command = commandMap.get(breakerCommand);
        if (Objects.isNull(command))
            throw new GlntBadRequestException("<!> 잘못된 명령어입니다.");


        char stx = 0x02, etx = 0x03;
        String msg = String.format("%s%s%s", stx, command, etx);
        if (command.equals("GATE UP") && facilityInfo.getBarStatus().contains("GATE UP")) {
            if (Objects.isNull(manual)) {
                facilityInfo.addOpenMessage(msg);
            }
        }

        manual = manual != null ? "수동" : "자동";

        log.info(">>>> {}({}) {} 메세지: {}", facilityInfo.getFname(),dtFacilityId, manual, command);

        client.sendMessage(facilityInfo.generateHost(), msg, Charset.forName("ASCII"));
    }


    // 연결된 전체 디바이스 상태정보 조회.
    @GetMapping("/v1/device/health")
    public ResponseEntity<ResponseDTO> fullDeviceStatusLookup() {
        return ResponseEntity.ok(
                new ResponseDTO(client.getChannelsStatus())
        );
    }

    // 시설물 아이디로 디바이스 상태정보 조회
    @GetMapping("/v1/device/health/{dtFacilityId}")
    public ResponseEntity<ResponseDTO> deviceHealthCheck(@PathVariable("dtFacilityId") String dtFacilityId) {
        return ResponseEntity.ok(
                new ResponseDTO(client.getChannelStatus(dtFacilityId))
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
