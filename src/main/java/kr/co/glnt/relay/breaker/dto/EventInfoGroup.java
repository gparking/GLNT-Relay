package kr.co.glnt.relay.breaker.dto;

import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
public class EventInfoGroup {
    private String key;
    private final List<EventInfo> eventList;
    private CarInfo entranceFrontCar;   // api 에 사용된 차량


    public EventInfoGroup(List<EventInfo> eventList) {
        this.eventList = eventList;
        this.key = generateUUID();
    }

    private String generateUUID() {
        StringBuilder builder = new StringBuilder();
        return builder.append(UUID.randomUUID())
                .append(System.currentTimeMillis())
                .toString();
    }
}
