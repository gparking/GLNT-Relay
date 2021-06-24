package kr.co.glnt.relay.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

@ToString
@Getter
@Builder
public class FacilityStatus {
    private String dtFacilitiesId;
    private String status;
    private String failureAlarm;


    public static FacilityStatus deviceHealth(String id, String health) {
        return FacilityStatus.builder()
                .dtFacilitiesId(id)
                .status(health).build();
    }

    // 장치 연결이 끊어졌을때 사용
    public static FacilityStatus deviceDisconnect(String facilityId) {
        return FacilityStatus.builder().dtFacilitiesId(facilityId).status("ERROR").build();
    }

    // 장치 연결되어있을때
    public static FacilityStatus reconnect(String facilityId) {
        return FacilityStatus.builder().dtFacilitiesId(facilityId).status("NORMAL").build();
    }



    // - 차단기가 Close 상태로 유지 중이며, 4대 이상 차량이 출차될 경우 알람 발생(1안)
    // - 차단기가 Close 상태로 유지 중이며, 4대 이상 차량이 정산요청할 경우 알람 발생(2안)
    public static FacilityStatus gateBarDamageDoubt(String facilityId) {
        return FacilityStatus.builder().dtFacilitiesId(facilityId).status("ERROR").failureAlarm("crossingGateBarDamageDoubt").build();
    }

    // 30분간 차단기 Open 되어 있을 경우 발생
    public static FacilityStatus gateLongTimeOpen(String facilityId) {
        return FacilityStatus.builder().dtFacilitiesId(facilityId).status("ERROR").failureAlarm("crossingGateLongTimeOpen").build();
    }

    // 정산기에서 헬스체크 정보를 받으면
    // 카드 리더기 상태정보값
    public static FacilityStatus icCardReaderStatus(String facilitiesId, String healthStatus) {
        return FacilityStatus.builder()
                .dtFacilitiesId(facilitiesId)
                .status(healthStatus.toUpperCase())
                .failureAlarm("icCardReaderFailure").build();
    }


    // 정산기에서 헬스체크 정보를 받으면
    // 정산기 상태정보값
    public static FacilityStatus payStationStatus(String facilitiesId, String healthStatus) {
        return FacilityStatus.builder()
                .dtFacilitiesId(facilitiesId)
                .status(healthStatus.toUpperCase())
                .failureAlarm("paymentFailure").build();
    }

    // 차단기 Action OK 보내기
    public static FacilityStatus breakerAction(String facilitiesId, String action) {
        return FacilityStatus.builder()
                .dtFacilitiesId(facilitiesId)
                .status(action)
                .build();
    }
}
