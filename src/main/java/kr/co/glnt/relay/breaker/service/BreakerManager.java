package kr.co.glnt.relay.breaker.service;

import kr.co.glnt.relay.breaker.dto.FacilityInfo;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public enum BreakerManager {
    OUTFRONT, INBACK, INFRONT;

    public static Breaker getInstance(FacilityInfo facilityInfo) {
        String type = facilityInfo.getGateLprType();
        switch (BreakerManager.valueOf(type)) {
            case INFRONT:
                return new Entrance(facilityInfo);
            case INBACK:
                return new EntranceBack(facilityInfo);
            case OUTFRONT:
                return new Exit(facilityInfo);
            default:
                return null;
        }
    }
}
