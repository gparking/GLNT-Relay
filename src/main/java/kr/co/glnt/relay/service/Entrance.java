package kr.co.glnt.relay.service;

import kr.co.glnt.relay.common.CommonUtils;
import kr.co.glnt.relay.dto.CarInfo;
import kr.co.glnt.relay.dto.EventInfo;
import kr.co.glnt.relay.dto.EventInfoGroup;
import kr.co.glnt.relay.dto.FacilityInfo;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.util.*;

/**
 * 입차 전방 이벤트 처리를 담당하는 클래스.
 * 기보은 Main LPR 한대만 적용하고
 * 카메라 각도가 잘 안나올 경우
 * Sub LPR 한대를 추가하여
 * 한 게이트당 LPR 은 최대 두대라는 가정하에 진행
 * 여기서 게이트 구분은 상속받은 Breaker 의
 * 필드로 존재하는 FacilityInfo 안의 imagePath 로 한다.
 */
@Slf4j
public class Entrance extends Breaker {
    // 입차 전방 이벤트를 처리하기 위해 필요한 큐
    private Queue<EventInfoGroup> entranceFrontQueue = new LinkedList<>();

    public Entrance(FacilityInfo facilityInfo) {
        super(facilityInfo);
    }

    @SneakyThrows
    @Override
    public void startProcessing(EventInfo eventInfo) {
        try {
            long currentTime = eventInfo.getCreatedTime();

            CarInfo carInfo = generatedCarInfo(eventInfo);
            eventInfo.setCarInfo(carInfo);

            synchronized (this) {

                // 처음 들어오는 차량은 새로운 그룹을 만들고
                // 이미지에서 차량 번호를 추출 후
                // 해당 차량정보를 GPMS 서버에 전송
                if (isNewCarEnters(currentTime)) {
                    lastRegTime = currentTime;

                    // 새로운 그룹을 생성
                    EventInfoGroup group = addNewGroupToQueue(eventInfo);

                    // 1초 후 비교로직 실행
                    startTimer();

                    // 새로운 스레드를 생성하여 차량 번호를 추출 후
                    // generatedCarInfo(차량 번호를 추출)
                    // 차량정보를 GPMS 서버에 전송.
                    new Thread(() -> {
                        gpmsAPI.requestEntranceCar(group.getKey(), carInfo);
                    }).start();
                } else {
                    addElementToFrontGroup(eventInfo);
                }
            }
        }
        catch (Exception e) {
            log.error("입차 - 전방 에러", e.getMessage());
        }
    }

    // 새로운 입차차량 그룹을 생성 후 큐에 추가.
    public EventInfoGroup addNewGroupToQueue(EventInfo eventInfo) {
        List<EventInfo> eventInfoList = new ArrayList<>();
        eventInfoList.add(eventInfo);
        EventInfoGroup group = new EventInfoGroup(eventInfoList);

        entranceFrontQueue.offer(group);
        return group;
    }



    // 현재 입차중인 차량 정보를 그룹에 추가
    public void addElementToFrontGroup(EventInfo info) {
        EventInfoGroup group = entranceFrontQueue.peek();
        group.getEventList().add(info);
    }


    // 입차 전방 이벤트 그룹을 반환
    public EventInfoGroup pollEntranceFrontQueue() {
        return entranceFrontQueue.poll();
    }

    // 타이머 설정.
    private void startTimer() {
        new Timer().schedule(task(), timerTime);
    }

    protected TimerTask task() {
        return new TimerTask() {
            @SneakyThrows
            @Override
            public void run() {
                EventInfoGroup eventGroup = pollEntranceFrontQueue();
                List<CarInfo> carInfos = getCurrentGroupEventList(eventGroup);

                // 그룹내에 등록된 차량 정보가 두개 이상일 때
                // (보조 LPR 이 달려있을 경우)
                if (carInfos.size() > 1) {
                    // 메인 LPR 에서 검출한 차량번호와
                    // 동일하면 무시하고 아닐경우 차량번호를 전송.
                    if (!isEqualsCarNumber(carInfos)) {
                        gpmsAPI.requestEntranceCar(eventGroup.getKey(), carInfos.get(carInfos.size()-1));
                    } else {
                        CommonUtils.deleteImageFile(carInfos.get(carInfos.size()-1).getFullPath());
                    }
                }

            }
        };
    }
}
