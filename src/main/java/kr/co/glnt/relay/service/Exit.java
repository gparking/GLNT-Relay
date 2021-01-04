package kr.co.glnt.relay.service;


import kr.co.glnt.relay.dto.CarInfo;
import kr.co.glnt.relay.dto.EventInfo;
import kr.co.glnt.relay.dto.EventInfoGroup;
import kr.co.glnt.relay.dto.FacilityInfo;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

/**
 * 출차 이벤트를 담당하는 클래스.
 * LPR 하나
 */
@Slf4j
public class Exit extends Breaker {
    public Exit(FacilityInfo facilityInfo) {
        super(facilityInfo);
    }

    @Override
    public void startProcessing(EventInfo eventInfo) {
        long currentTime = eventInfo.getCreatedTime();
        synchronized (this) {
            if (isNewCarEnters(currentTime)) {
                lastRegTime = currentTime;
                EventQueueManager.addNewGroupToExitQueue(eventInfo);
                startTimer();
            }
            else {
                EventQueueManager.addElementToExitGroup(eventInfo);
            }
        }
    }


    private void startTimer() {
        new Timer().schedule(task(), TIMER_TIME);
    }

    protected TimerTask task() {
        return new TimerTask() {
            @Override
            public void run() {
                EventInfoGroup eventGroup = EventQueueManager.pollExitQueue();
                List<CarInfo> carInfos = getCurrentGroupEventList(eventGroup);

                CarInfo carInfo = getCarInfoToBeTransmit(carInfos);


                // TODO: 출차 api
            }
        };
    }

}
