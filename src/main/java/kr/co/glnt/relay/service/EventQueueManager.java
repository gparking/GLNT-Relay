package kr.co.glnt.relay.service;

import kr.co.glnt.relay.dto.EventInfo;
import kr.co.glnt.relay.dto.EventInfoGroup;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

@Slf4j
public class EventQueueManager {
    // 입차 전방 이벤트를 처리하기 위해 필요한 큐
    private static Queue<EventInfoGroup> entranceFrontQueue = new LinkedList<>();

    // 입차 후방 이벤트에서 전방 이벤트를 알기위한 큐
    private static Queue<EventInfoGroup> entranceBackQueue = new LinkedList<>();

    // 출차 이벤트 큐
    private static Queue<EventInfoGroup> exitQueue = new LinkedList<>();


    private EventQueueManager() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * 새로운 입차차량 그룹을 생성 후 큐에 추가.
     */
    public static void addNewGroupToQueue(EventInfo eventInfo) {
        List<EventInfo> eventInfoList = new ArrayList<>();
        eventInfoList.add(eventInfo);
        EventInfoGroup group = new EventInfoGroup(eventInfoList);

        entranceFrontQueue.offer(group);
        entranceBackQueue.offer(group);
    }

    /**
     * 현재 입차중인 차량 정보를 그룹에 추가
     */
    public static void addElementToFrontGroup(EventInfo info) {
        EventInfoGroup group = entranceFrontQueue.peek();
        group.getEventList().add(info);
    }

    /**
     * 입차 전방 이벤트 그룹을 반환
     */
    public static EventInfoGroup pollEntranceFrontQueue() {
        return entranceFrontQueue.poll();
    }
    public static EventInfoGroup pollEntranceBackQueue() {
        return entranceBackQueue.poll();
    }


    /**
     * 새로운 출차차량 그룹을 생성 후 큐에 추가
     */
    public static void addNewGroupToExitQueue(EventInfo eventInfo) {
        List<EventInfo> eventInfoList = new ArrayList<>();
        eventInfoList.add(eventInfo);
        exitQueue.offer(new EventInfoGroup(eventInfoList));
    }

    /**
     * 현재 출차중인 차량정보를 그룹에 추가.
     */
    public static void addElementToExitGroup(EventInfo eventInfo) {
        exitQueue.peek()
                .getEventList()
                .add(eventInfo);
    }

    /**
     * 출차 큐에 쌓인 그룹을 반환
     */
    public static EventInfoGroup pollExitQueue() {
        return exitQueue.poll();
    }
}
