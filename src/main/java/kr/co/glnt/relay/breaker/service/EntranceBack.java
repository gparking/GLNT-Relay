package kr.co.glnt.relay.breaker.service;

import kr.co.glnt.relay.breaker.dto.CarInfo;
import kr.co.glnt.relay.breaker.dto.EventInfo;
import kr.co.glnt.relay.breaker.dto.EventInfoGroup;
import kr.co.glnt.relay.breaker.dto.FacilityInfo;
import lombok.extern.slf4j.Slf4j;

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
            EventInfoGroup frontEventGroup = EventQueueManager.pollEntranceBackQueue();
            if (Objects.isNull(frontEventGroup)) {
                return;
            }
            CarInfo carInfo = ngisAPI.requestOCR(eventInfo.getFullPath());
            carInfo.setInDate(eventInfo.getCreatedTime());
            carInfo.setFullPath(eventInfo.getFullPath());
            carInfo.setFacilitiesId(facilityInfo.getFacilitiesId());

            gpmsAPI.requestEntranceCar(frontEventGroup.getKey(), carInfo);
        }
        catch (Exception e) {
            log.error("입차 - 후방 에러", e.getMessage());
        }
    }
}
