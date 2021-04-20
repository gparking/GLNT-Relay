package kr.co.glnt.relay.service;

import kr.co.glnt.relay.dto.FacilityInfo;
import lombok.extern.slf4j.Slf4j;

// INFRONT, INBACK, OUTFRONT: 정방향에 사용
// ININFRONT, ININBACK, OUTOUTFRONT: 양방향에 사용
@Slf4j
public enum BreakerFactory {
    INFRONT, INBACK, OUTFRONT,
    IN_OUTINFRONT, IN_OUTINBACK, IN_OUTOUTFRONT;

    public static Breaker getInstance(FacilityInfo facilityInfo) {
        String type = facilityInfo.generateGateLprType();
        log.info(">>>> Create {}, path: {}", facilityInfo.generateGateLprType(), facilityInfo.getImagePath());
        switch (BreakerFactory.valueOf(type)) {
            case INFRONT:
            case IN_OUTINFRONT:
                return new Entrance(facilityInfo);
            case INBACK:
            case IN_OUTINBACK:
                return new EntranceBack(facilityInfo);
            case OUTFRONT:
            case IN_OUTOUTFRONT:
                return new Exit(facilityInfo);
            default:
                return null;
        }
    }
}
