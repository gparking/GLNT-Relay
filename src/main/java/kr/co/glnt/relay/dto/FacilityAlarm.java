package kr.co.glnt.relay.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class FacilityAlarm {
    private String facilityId;
    private String failureAlarm;


    // - 차단기가 Close 상태로 유지 중이며, 4대 이상 차량이 출차될 경우 알람 발생(1안)
    // - 차단기가 Close 상태로 유지 중이며, 4대 이상 차량이 정산요청할 경우 알람 발생(2안)"
    public static FacilityAlarm gateBarDamageDoubt(String facilityId) {
        return new FacilityAlarm(facilityId, "crossingGateBarDamageDoubt");
    }

    // 30분간 차단기 Open 되어 있을 경우 발생
    public static FacilityAlarm gateLongTimeOpen(String facilityId) {
        return new FacilityAlarm(facilityId, "crossingGateLongTimeOpen");
    }

}
