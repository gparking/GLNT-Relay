package kr.co.glnt.relay.breaker.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import kr.co.glnt.relay.breaker.dto.EventInfo;
import kr.co.glnt.relay.breaker.dto.EventInfoGroup;
import kr.co.glnt.relay.breaker.dto.CarInfo;
import kr.co.glnt.relay.breaker.dto.FacilityInfo;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.stream.Collectors;

import static java.util.Map.Entry.comparingByValue;

/**
 * 입차 전방 이벤트 처리를 담당하는 클래스.
 */
// TODO: 입차 카운트 관련 확인.
@Slf4j
public class Entrance extends Breaker {
    private static CarInfo previousInfo = new CarInfo();

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

    private void startTimer() {
        Timer timer = new Timer();
        timer.schedule(test(), TIMER_TIME);
    }


    /**
     * 정상 인식된 번호가 있으면 인식된 번호들을 키로 잡아 묶고,
     * 묶인 번호들 중 더 많이 찍힌 정보를 보냄.
     * 정상 인식된 번호가 없으면 부분인식이 있는지 체크 후 있으면 부분인식 정보를 보내고
     * 없으면 carInfos.get(0)의 정보를 보낸다.(미인식)
     */
    private TimerTask test() {
        return new TimerTask() {
            @Override
            public void run() {
                try {
                    long startTime = System.currentTimeMillis();
                    EventInfoGroup eventGroup = EventQueueManager.pollEntranceFrontQueue();
                    List<CarInfo> carInfos = eventGroup.getEventList()
                            .stream()
                            .map(eventInfo -> {
                                CarInfo carInfo = ngisAPI.requestOCR(eventInfo.getFullPath());
                                carInfo.setInDate(eventInfo.getCreatedTime());
                                carInfo.setFullPath(eventInfo.getFullPath());
                                carInfo.setFacilitiesId(facilityInfo.getFacilitiesId());
                                return carInfo;
                            }).collect(Collectors.toList());

                    log.info("carInfos : {}", new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(carInfos));

                    // 정상 인식된 번호가 있을 시.
                    long ocrCount = carInfos.stream().filter(CarInfo::isState).count();
                    CarInfo carInfo = null;
                    if (ocrCount > 0) {
                        String carNumber = null;
                        // OCR 이 인식된(부분인식 포함.) 값들을 차량번호를 키로 하여 map으로 변환.
                        Map<String, List<CarInfo>> carMap = carInfos.stream()
                                .filter(CarInfo::isState)
                                .collect(Collectors.groupingBy(CarInfo::getNumber));

                        // 인식된 번호중 더 많이 찍힌 차량번호를 추출.
                        carNumber = carMap.entrySet().stream()
                                .max((entry1, entry2) -> entry1.getValue().size() > entry2.getValue().size() ? 1 : -1)
                                .get()
                                .getKey();

                        // 차량 정보 가져오기
                        carInfo = carMap.get(carNumber).get(0);
                    }
                    else { // 정상인식된 번호가 없을 때
                        // 부분인식이 있는지 확인.
                        for (CarInfo info : carInfos) {
                            if (info.getCode() > 10) { // 부분인식이 있으면
                                carInfo = info;
                                return;
                            }
                            carInfo = info;
                        }
                    }


                    previousInfo = carInfo;
                    gpmsAPI.requestEntranceCar(eventGroup.getKey(), carInfo);
                    eventGroup.setEntranceFrontCar(carInfo);


                    log.info("메소드 실행 시간 : {}", (System.currentTimeMillis() - startTime));
                }
                catch (Exception e) {
                    log.error("입차 - 전방 타이머 함수 에러", e.getMessage());
                }
            }
        };
    }
}
