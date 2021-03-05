package kr.co.glnt.relay.web;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import kr.co.glnt.relay.dto.*;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

@Slf4j
@Component("gpmsAPI")
public class GpmsAPI {
    private final RestTemplate template;
    private final ObjectMapper objectMapper;

    public GpmsAPI(@Qualifier(value = "gpmsRestTemplate") RestTemplate template, ObjectMapper objectMapper) {
        this.template = template;
        this.objectMapper = objectMapper;
    }

    /**
     * 서버 시작 시 호출하는 서버정보 조회
     *
     * @param facilityInfoPayload 서버 정보 조회에 필요한 데이터.
     * @return ParkingFeature (서버정보)
     */
    @SneakyThrows
    @Retryable(backoff = @Backoff(delay = 0))
    public List<FacilityInfo> getParkinglotData(FacilityInfoPayload facilityInfoPayload) {
        ResponseEntity<ResponseDTO> response = template.postForEntity("/v1/parkinglot/facility/list", facilityInfoPayload, ResponseDTO.class);
        if (Objects.isNull(response)) return Collections.emptyList();
        HttpStatus status = HttpStatus.resolve(response.getStatusCodeValue());
        if (status == HttpStatus.OK) {
            ResponseDTO responseDTO = response.getBody();
            try {
                return objectMapper.convertValue(responseDTO.getData(), new TypeReference<List<FacilityInfo>>() {
                });
            } catch (Exception e) {
                log.error("주차장정보 데이터 변환 실패", e);
            }
        }
        return Collections.emptyList();
    }

    // Display init message 가져오기
    public ResponseDTO requestDisplayInitMessage() {
        return template.getForObject("/v1/relay/display/init/message", ResponseDTO.class);
    }


    // 입차 차량 정보 전송
    @Async
    public void requestEntranceCar(String key, CarInfo carInfo) {
        try {
            ParkInOutPayload payload = new ParkInOutPayload(key, carInfo);
            ResponseDTO response = template.postForObject("/v1/inout/parkin", payload, ResponseDTO.class);
            if (response.getCode() == HttpStatus.CREATED.value()) {
                deleteImageFile(carInfo);
            }
        } catch (Exception e) {
            log.error("입차 이미지 전송 실패", e);
        }
    }

    // 출차 차량 정보 전송
    @Async
    @Retryable(backoff = @Backoff(delay = 0))
    public void requestExitCar(String key, CarInfo carInfo) {
        try {
            ParkInOutPayload payload = new ParkInOutPayload(key, carInfo);
            ResponseDTO response = template.postForObject("/v1/inout/parkout", payload, ResponseDTO.class);
            if (response.getCode() == HttpStatus.CREATED.value()) {
                deleteImageFile(carInfo);
            }
        } catch (Exception e) {
            log.error("출차 이미지 전송 실패", e);
        }
    }


    // 장비 헬스체크
    @Async
    public void sendFacilityHealth(FacilityPayloadWrapper facilityStatusList) {
        template.postForObject("/v1/relay/health_check", facilityStatusList, ResponseDTO.class);
    }

    // 시설물 관련 알림
    @Async
    public void sendFacilityAlarm(FacilityPayloadWrapper facilityPayloadWrapper) {
        try {
            template.postForObject("/v1/relay/failure_alarm", facilityPayloadWrapper, ResponseDTO.class);
        } catch (Exception e) {
            log.error("시설물 관련 알림 전송 실패: ", e);
        }
    }

    // 장비 상태 정보
    @Async
    public void sendStatusNoti(Object object) {
        template.postForObject("/v1/relay/status_noti", object, ResponseDTO.class);
    }

    // 정산 완료
    public void sendPaymentResponse(String id, String data) {
        String url = String.format("/v1/relay/paystation/result/%s", id);
        template.postForEntity(url, data, ResponseDTO.class);
    }

    // 출차시 미인식 차량 번호 조회
    public void searchVehicle(String id, String data) {
        String url = String.format("/v1/relay/paystation/search/vehicle/%s", id);
        template.postForObject(url, data, ResponseDTO.class);
    }

    // 차량 번호 선택 후 정산 요청
    public void sendPayment(String id, String data) {
        String url = String.format("/v1/relay/paystation/request/adjustment/%s", id);
        template.postForObject(url, data, ResponseDTO.class);
    }



    // note: 공통 클래스 추출시 옮길것
    private void deleteImageFile(CarInfo carInfo) throws IOException {
        Path filePath = Paths.get(carInfo.getFullPath());
        if (filePath.toFile().exists()) {
            Files.delete(filePath);
        }
    }

}

