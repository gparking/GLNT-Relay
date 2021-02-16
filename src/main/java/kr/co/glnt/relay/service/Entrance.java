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
 * 기보은 Main LPR 한대만 적용하고
 * 카메라 각도가 잘 안나올 경우
 * Sub LPR 한대를 추가하여
 * 한 게이트당 LPR 은 최대 두대라는 가정하에 진행
 * 여기서 게이트 구분은 상속받은 Breaker 의
 * 필드로 존재하는 FacilityInfo 안의 imagePath 로 한다.
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

                // 처음 들어오는 차량은 새로운 그룹을 만들고
                // 이미지에서 차량 번호를 추출 후
                // 해당 차량정보를 GPMS 서버에 전송
                if (isNewCarEnters(currentTime)) {
                    lastRegTime = currentTime;

                    // 새로운 그룹을 생성
                    EventInfoGroup group = EventQueueManager.addNewGroupToQueue(eventInfo);

                    // 1초 후 비교로직 실행
                    startTimer();

                    // 새로운 스레드를 생성하여 차량 번호를 추출 후
                    // generatedCarInfo(차량 번호를 추출)
                    // 차량정보를 GPMS 서버에 전송.
                    new Thread(() -> {
                        gpmsAPI.requestEntranceCar(group.getKey(), generatedCarInfo(eventInfo));
                    }).start();
                } else {
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

                // 그룹내에 등록된 차량 정보가 두개 이상일 때
                // (보조 LPR 이 달려있을 경우)
                if (carInfos.size() > 1) {

                    // 메인 LPR 에서 검출한 차량번호와
                    // 동일하면 무시하고 아닐경우 차량번호를 전송.
                    if (!isEqualsCarNumber(carInfos)) {
                        gpmsAPI.requestEntranceCar(eventGroup.getKey(), carInfos.get(carInfos.size()-1));
                    }
                }
                // CarInfo carInfo = getCarInfoToBeTransmit(carInfos);



            }
        };
    }
}
