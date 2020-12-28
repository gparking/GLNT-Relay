package kr.co.glnt.relay.breaker.service;

import kr.co.glnt.relay.breaker.dto.CarInfo;
import kr.co.glnt.relay.breaker.dto.EventInfo;
import kr.co.glnt.relay.breaker.dto.EventInfoGroup;
import kr.co.glnt.relay.breaker.dto.FacilityInfo;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Objects;

/**
 * 입차 후방이벤트를 담당하는 클래스.
 */
@Slf4j
public class EntranceBack extends Breaker {
    public EntranceBack(FacilityInfo facilityInfo) {
        super(facilityInfo);
    }


    /**
     * 후방 카메라는 하나만 있다고 가정하고 작업을 시작하지.
     *
     * 0. 일단 다 보내는걸로

     * @param eventInfo
     */
    @Override
    public void startProcessing(EventInfo eventInfo) {
        try {
            EventInfoGroup frontBackEventGroup = EventQueueManager.pollEntranceBackQueue();
            if (Objects.isNull(frontBackEventGroup)) {
                return;
            }
            List<CarInfo> carInfos = getCurrentGroupEventList(frontBackEventGroup);

            gpmsAPI.requestEntranceCar(frontBackEventGroup.getKey(), carInfos.get(0));
        }
        catch (Exception e) {
            log.error("입차 - 후방 에러", e.getMessage());
        }
    }
}
