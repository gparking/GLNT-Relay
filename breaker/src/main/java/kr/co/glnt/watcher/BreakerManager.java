package kr.co.glnt.watcher;

import kr.co.glnt.dto.CarGroup;
import kr.co.glnt.dto.CarInfo;
import kr.co.glnt.dto.ParkinglotInfo;
import lombok.extern.slf4j.Slf4j;

import java.util.*;

/**
 * 입차 이벤트 처리를 하기 위해 만든 클래스 (추후 출차도 추가)
 */
@Slf4j
public enum BreakerManager {
    OUTFRONT, INBACK, INFRONT;

    public static Breaker getInstance(ParkinglotInfo parkinglotInfo) {
        String type = parkinglotInfo.getGateLprType();
        switch (BreakerManager.valueOf(type)) {
            case INFRONT:
                return new Entrance(parkinglotInfo);
            case INBACK:
                return new EntranceBack(parkinglotInfo);
            case OUTFRONT:
                return new Exit(parkinglotInfo);
            default:
                return null;
        }
    }

    // sync인데 왜 queue를 쓰니
    // previous CarGroup 을 하나 만들어서 queue 에서 poll 한 값과 비교.
    private static Queue<CarGroup> entranceFrontQueue = new LinkedList<>(); // 전방 LPR Event Queue
    private static Queue<CarGroup> entranceBackQueue = new LinkedList<>();  // 후방 LPR Event Queue


    public void addNewGroupInFrontQueue(CarInfo carInfo) {
        CarGroup currentCarGroup = new CarGroup(carInfo);
        entranceFrontQueue.offer(currentCarGroup);
        entranceBackQueue.offer(currentCarGroup);
    }

    public void addOcrInfoInGroup(CarInfo carInfo) {
        CarGroup carGroup = entranceFrontQueue.peek();
        if (Objects.nonNull(carGroup)) {
            if (!carGroup.isState()) {
                carGroup.addElement(carInfo);
            }
        }
    }

    public void addNewGroupInBackQueue() {
        CarGroup carGroup = entranceFrontQueue.poll();
        if (Objects.nonNull(carGroup)) {

        }
    }

    public CarGroup getCurrentCarGroup() {
        return entranceFrontQueue.peek();
    }


    public void loggingQueue() {
        log.info("current queue: {}", entranceFrontQueue);
    }

}
