package kr.co.glnt.relay.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.channel.Channel;
import kr.co.glnt.relay.dto.FacilityInfo;
import kr.co.glnt.relay.dto.FacilityPayloadWrapper;
import kr.co.glnt.relay.dto.FacilityStatus;
import kr.co.glnt.relay.web.GpmsAPI;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Map;
import java.util.Objects;

@Service
@Slf4j
public class PaymentService{

    private final ObjectMapper objectMapper;
    private final GpmsAPI gpmsAPI;

    public PaymentService(ObjectMapper objectMapper, GpmsAPI gpmsAPI) {
        this.objectMapper = objectMapper;
        this.gpmsAPI = gpmsAPI;
    }


    @SneakyThrows
    public void paymentProcessing(FacilityInfo facilityInfo, String message) {
        Map<String, Object> receiveData = objectMapper.readValue(message, new TypeReference<Map<String, Object>>() {});
        String type = Objects.toString(receiveData.get("type"), "");

        switch (type) {
            case "vehicleListSearch": // 차량 목록 조회
                // GPMS 에 토스
                log.info(">>>> 정산기({}) 차량 목록 조회 메세지 수신: {}", facilityInfo.getDtFacilitiesId(), message);
                gpmsAPI.searchVehicle(facilityInfo.getDtFacilitiesId(), message);
                break;
            case "adjustmentRequest": // 정산 요청 응답
                log.info(">>>> 정산기({}) 정산 요청 메세지 응답 수신: {}", facilityInfo.getDtFacilitiesId(), message);
                gpmsAPI.sendPayment(facilityInfo.getDtFacilitiesId(), message);
                break;
            case "payment": // 결제 응답 (결제 결과)
                log.info(">>>> 정산기({}) 결제 응답 메세지 수신: {}", facilityInfo.getDtFacilitiesId(), message);
                gpmsAPI.sendPaymentResponse(facilityInfo.getDtFacilitiesId(), message);
                break;
            case "healthCheck":
                log.info(">>>> 정산기({}) 헬스체크 메세지 응답 수신: {}", facilityInfo.getDtFacilitiesId(), message);
                Map<String, String> contents = objectMapper.convertValue(receiveData.get("contents"), new TypeReference<Map<String, String>>(){});

                // 정산기 연결 상태
                String payStationStatus = Objects.toString(contents.get("paymentFailure"), "");

                // 카드 리더기 연결 상태
                String cardReaderStatus = Objects.toString(contents.get("icCardReaderFailure"), "");
                String responseId = Objects.toString(receiveData.get("responseId"), "");

                gpmsAPI.sendFacilityHealth(FacilityPayloadWrapper.healthCheckPayload(
                        Arrays.asList(
                                FacilityStatus.payStationStatus(facilityInfo.getDtFacilitiesId(), payStationStatus),
                                FacilityStatus.icCardReaderStatus(facilityInfo.getDtFacilitiesId(), cardReaderStatus, responseId)
                        )
                ));
                break;
            case "adjustmentdataRequest":
                log.info(">>>> 정산기({}) 바코드 할인 메세지 응답 수신: {}", facilityInfo.getDtFacilitiesId(), message);
                //String requestData = objectMapper.writeValueAsString(receiveData);
                gpmsAPI.sendAdjustmentDataRequest(facilityInfo.getDtFacilitiesId(),message);
                break;

            case "paymentFailure":
                log.info(">>>> 정산 실패: {}", message);
                gpmsAPI.sendPaymentFailureResponse(facilityInfo.getDtFacilitiesId(), message);
                break;
            case "aliveCheck":
                log.info(">>> 정산기 Alive check");
            default:
                log.info(">>>> 정산기 메세지 수신: {}", message);
                break;
        }
    }
}
