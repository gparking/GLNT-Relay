package kr.co.glnt.relay.service;

import kr.co.glnt.relay.dto.CarInfo;
import kr.co.glnt.relay.dto.EventInfo;
import kr.co.glnt.relay.dto.EventInfoGroup;
import kr.co.glnt.relay.dto.FacilityInfo;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

/**
 * 입차 전방 이벤트 처리를 담당하는 클래스.
 */
@Slf4j
public class Entrance extends Breaker {
    public Entrance(FacilityInfo facilityInfo) {
        super(facilityInfo);
    }

    @SneakyThrows
    @Override
    public void startProcessing(EventInfo eventInfo) {
        try {
            long currentTime = eventInfo.getCreatedTime();
            synchronized (this) {
                if (isNewCarEnters(currentTime)) {
                    lastRegTime = currentTime;
                    EventQueueManager.addNewGroupToQueue(eventInfo);
                    startTimer();
                }
                else {
                    EventQueueManager.addElementToFrontGroup(eventInfo);
                }
            }
        }
        catch (Exception e) {
            log.error("입차 - 전방 에러", e.getMessage());
        }
    }

    /**
     * 타이머 설정.
     */
    private void startTimer() {
        new Timer().schedule(task(), timerTime);
    }

    protected TimerTask task() {
        return new TimerTask() {
            @SneakyThrows
            @Override
            public void run() {
                EventInfoGroup eventGroup = EventQueueManager.pollEntranceFrontQueue();
                List<CarInfo> carInfos = getCurrentGroupEventList(eventGroup);
                CarInfo carInfo = getCarInfoToBeTransmit(carInfos);

                gpmsAPI.requestEntranceCar(eventGroup.getKey(), carInfo);
            }
        };
    }
}
