package kr.co.glnt.relay.breaker.service;

import kr.co.glnt.relay.breaker.dto.CarInfo;
import kr.co.glnt.relay.breaker.dto.EventInfo;
import kr.co.glnt.relay.breaker.dto.EventInfoGroup;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.stream.Collectors;

@Slf4j
public class EventQueueManager {
    // 입차 전방 이벤트를 처리하기 위해 필요한 큐
    private static Queue<EventInfoGroup> entranceFrontQueue = new LinkedList<>();
    // 입차 후방 이벤트에서 전방 이벤트를 알기위한 큐
    private static Queue<EventInfoGroup> entranceBackQueue = new LinkedList<>();

    public static void addNewGroupToQueue(EventInfo eventInfo) {
        List<EventInfo> eventInfoList = new ArrayList<>();
        eventInfoList.add(eventInfo);
        EventInfoGroup group = new EventInfoGroup(eventInfoList);

        entranceFrontQueue.offer(group);
        entranceBackQueue.offer(group);
    }

    public static void addElementToFrontGroup(EventInfo info) {
        EventInfoGroup group = entranceFrontQueue.peek();
        group.getEventList().add(info);
    }

    /**
     * 현재 처리중인 입차 전방 이벤트 그룹을 반환한다.
     * @return EventInfoGroup
     */
    public static EventInfoGroup pollEntranceFrontQueue() {
        return entranceFrontQueue.poll();
    }

    public static List<EventInfoGroup> findNumberInThreeGroups() {
        return entranceBackQueue.stream()
                .limit(3)
                .collect(Collectors.toList());
    }

    public static EventInfoGroup pollEntranceBackQueue() {
        return entranceBackQueue.poll();
    }
}
