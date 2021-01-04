package kr.co.glnt.relay.service;

import kr.co.glnt.relay.dto.FacilityInfo;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public enum BreakerFactory {
    OUTFRONT, INBACK, INFRONT;

    public static Breaker getInstance(FacilityInfo facilityInfo) {
        String type = facilityInfo.generateGateLprType();
        switch (BreakerFactory.valueOf(type)) {
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
