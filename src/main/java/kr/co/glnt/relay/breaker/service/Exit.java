package kr.co.glnt.relay.breaker.service;


import com.fasterxml.jackson.databind.ObjectMapper;
import kr.co.glnt.relay.breaker.dto.CarInfo;
import kr.co.glnt.relay.breaker.dto.EventInfo;
import kr.co.glnt.relay.breaker.dto.EventInfoGroup;
import kr.co.glnt.relay.breaker.dto.FacilityInfo;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.stream.Collectors;

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


                // TODO: 출차 API Sync? Async? - Sync 가야지?
            }
        };
    }

}
