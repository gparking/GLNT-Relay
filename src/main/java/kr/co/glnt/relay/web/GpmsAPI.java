package kr.co.glnt.relay.web;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import kr.co.glnt.relay.common.CommonUtils;
import kr.co.glnt.relay.dto.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

@Slf4j
@Component("gpmsAPI")
public class GpmsAPI {
    private final RestTemplate template;
    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    public GpmsAPI(@Qualifier(value = "gpmsRestTemplate") RestTemplate template, WebClient webClient, ObjectMapper objectMapper) {
        this.template = template;
        this.webClient = webClient;
        this.objectMapper = objectMapper;
    }

    /**
     * 서버 시작 시 호출하는 서버정보 조회
     *
     * @param facilityInfoPayload 서버 정보 조회에 필요한 데이터.
     * @return ParkingFeature (서버정보)
     */
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

    public ResponseDTO requestDisplayFormat() {
//        return template.getForObject("/v1/relay/display/info", ResponseDTO.class);
        return webClient.get()
                .uri("/v1/relay/display/info")
                .retrieve()
                .bodyToMono(ResponseDTO.class)
                .onErrorResume(err -> {
                    log.info("<!> display format request err : {}", err.getMessage());
                    return Mono.just(new ResponseDTO(err));
                })
                .block();
    }


    // 입차 차량 정보 전송
    public void requestEntranceCar(String type, String key, CarInfo carInfo) {
        log.info(">>>> {} 입차요청 - key: {}, dtFacilitiesid: {}, number: {}, path: {}",type, key, carInfo.getDtFacilitiesId(), carInfo.getNumber(), carInfo.getFullPath());

        ParkInOutPayload payload = new ParkInOutPayload(key, carInfo);

        log.info(">>>> gpms filesize: {}", new File(carInfo.getFullPath()).length());

//        try {
//            template.postForObject("/v1/inout/parkin", payload, ResponseDTO.class);
//            CommonUtils.deleteImageFile(carInfo.getFullPath());
//        } catch (Exception e) {
//            CommonUtils.deleteImageFile(carInfo.getFullPath());
//        }
        Mono<ResponseDTO> response = webClient.post()
                .uri("/v1/inout/parkin")
                .bodyValue(payload)
                .retrieve()
                .bodyToMono(ResponseDTO.class)
                .doOnError(err -> {
                    CommonUtils.deleteImageFile(carInfo.getFullPath());
                });

        response.subscribe(result -> {
            CommonUtils.deleteImageFile(carInfo.getFullPath());
        });
    }

    // 출차 차량 정보 전송
//    @Retryable(backoff = @Backoff(delay = 0))
    public void requestExitCar(String key, CarInfo carInfo) {
        log.info(">>>> 출차요청 - key: {}, dtFacilitiesid: {}, number: {}, path: {}", key, carInfo.getDtFacilitiesId(), carInfo.getNumber(), carInfo.getFullPath());
        ParkInOutPayload payload = new ParkInOutPayload(key, carInfo);
//        try {
//            template.postForObject("/v1/inout/parkout", payload, ResponseDTO.class);
//            CommonUtils.deleteImageFile(carInfo.getFullPath());
//
//        } catch (Exception e) {
//            CommonUtils.deleteImageFile(carInfo.getFullPath());
//        }
//
        Mono<ResponseDTO> response = webClient.post()
                .uri("/v1/inout/parkout")
                .bodyValue(payload)
                .retrieve()
                .bodyToMono(ResponseDTO.class)
                .doOnError(err -> {
                    log.error("<!> request parkout", err);
                    CommonUtils.deleteImageFile(carInfo.getFullPath());
                });


        response.subscribe(result -> {
            CommonUtils.deleteImageFile(carInfo.getFullPath());
        });
    }


    // 장비 헬스체크
    public void sendFacilityHealth(FacilityPayloadWrapper facilityStatusList) {
//        template.postForObject("/v1/relay/health_check", facilityStatusList, ResponseDTO.class);
        webClient.post()
                .uri("/v1/relay/health_check")
                .bodyValue(facilityStatusList)
                .retrieve()
                .toBodilessEntity()
                .onErrorResume(err -> {
                    log.info("<!> device heal check request err : {}", err.getMessage());
                    return Mono.empty();
                })
                .subscribe();
    }

    // 시설물 관련 알림
    public void sendFacilityAlarm(FacilityPayloadWrapper facilityPayloadWrapper) {
//        template.postForObject("/v1/relay/failure_alarm", facilityPayloadWrapper, ResponseDTO.class);
        webClient.post()
                .uri("/v1/relay/failure_alarm")
                .bodyValue(facilityPayloadWrapper)
                .retrieve()
                .toBodilessEntity().subscribe();
    }

    // 장비 상태 정보
    public void sendStatusNoti(FacilityPayloadWrapper object) {
//        template.postForObject("/v1/relay/status_noti", object, ResponseDTO.class);
        webClient.post()
                .uri("/v1/relay/status_noti")
                .bodyValue(object)
                .retrieve()
                .toBodilessEntity()
                .onErrorResume(err -> {
                    log.info("<!> device status notification err : {}", err.getMessage());
                    return Mono.empty();
                })
                .subscribe();
    }

    // 정산 완료
    public void sendPaymentResponse(String id, String data) {
//        template.postForObject("/v1/relay/paystation/result/"+id, data, ResponseDTO.class);
        webClient.post()
                .uri("/v1/relay/paystation/result/{id}", id)
                .bodyValue(data)
                .retrieve()
                .toBodilessEntity().subscribe();
    }

    // 출차시 미인식 차량 번호 조회
    public void searchVehicle(String id, String data) {
//        template.postForObject("/v1/relay/paystation/search/vehicle/"+id, data, ResponseDTO.class);
        webClient.post()
                .uri("/v1/relay/paystation/search/vehicle/{id}", id)
                .bodyValue(data)
                .retrieve()
                .toBodilessEntity().subscribe();
    }

    // 차량 번호 선택 후 정산 요청
    public void sendPayment(String id, String data) {
//        template.postForObject("/v1/relay/paystation/request/adjustment/"+id, data, ResponseDTO.class);
        webClient.post()
                .uri("/v1/relay/paystation/request/adjustment/{id}", id)
                .bodyValue(data)
                .retrieve()
                .toBodilessEntity().subscribe();
    }
}

