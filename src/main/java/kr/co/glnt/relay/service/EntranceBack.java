package kr.co.glnt.relay.service;

import kr.co.glnt.relay.common.CommonUtils;
import kr.co.glnt.relay.dto.CarInfo;
import kr.co.glnt.relay.dto.EventInfo;
import kr.co.glnt.relay.dto.EventInfoGroup;
import kr.co.glnt.relay.dto.FacilityInfo;
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
     * 0. 일단 다 보내는걸로 key 는 empty
     * @param eventInfo
     */
    @Override
    public void startProcessing(EventInfo eventInfo) {
        try {
            CarInfo carInfo = generatedCarInfo(eventInfo);
            if(carInfo.ocrValidate()) {
                gpmsAPI.requestEntranceCar("후방", "", generatedCarInfo(eventInfo));
            } else {
                if (Objects.nonNull(carInfo)) {
                    if (carInfo.getCode() == -1) {
                        log.info("인식도중 에러: {}", carInfo.getNumber());
                    }
                }

                CommonUtils.deleteImageFile(carInfo.getFullPath());
            }

        } catch (Exception e) {
            log.error("<!> 입차 - 후방 에러", e.getMessage());
            CommonUtils.deleteImageFile(eventInfo.getFullPath());
        }
    }
}
