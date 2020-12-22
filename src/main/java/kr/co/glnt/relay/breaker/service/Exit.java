package kr.co.glnt.relay.breaker.service;


import kr.co.glnt.relay.breaker.dto.EventInfo;
import kr.co.glnt.relay.breaker.dto.FacilityInfo;
import lombok.extern.slf4j.Slf4j;

// 출차
@Slf4j
public class Exit extends Breaker {

    public Exit(FacilityInfo facilityInfo) {
        super(facilityInfo);
    }

    @Override
    public void startProcessing(EventInfo eventInfo) {

    }
}
