package kr.co.glnt.relay.service;

import kr.co.glnt.relay.dto.FacilityInfo;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public enum BreakerFactory {
    OUTOUTFRONT, ININBACK, ININFRONT, IN_OUTINFRONT, IN_OUTINBACK, IN_OUTOUTFRONT;

    public static Breaker getInstance(FacilityInfo facilityInfo) {
        String type = facilityInfo.generateGateLprType();
        switch (BreakerFactory.valueOf(type)) {
            case ININFRONT:
            case IN_OUTINFRONT:
                return new Entrance(facilityInfo);
            case ININBACK:
            case IN_OUTINBACK:
                return new EntranceBack(facilityInfo);
            case OUTOUTFRONT:
            case IN_OUTOUTFRONT:
                return new Exit(facilityInfo);
            default:
                return null;
        }
    }
}
