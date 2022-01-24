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
 * 출차 이벤트를 담당하는 클래스.
 * LPR 하나
 */
@Slf4j
public class Exit extends Breaker {
    // 출차 이벤트 큐
    private Queue<EventInfoGroup> exitQueue = new LinkedList<>();

    public Exit(FacilityInfo facilityInfo) {
        super(facilityInfo);
    }

    @Override
    public void startProcessing(EventInfo eventInfo) {
        try {
            long currentTime = eventInfo.getCreatedTime();
            CarInfo carInfo = generatedCarInfo(eventInfo);
            eventInfo.setCarInfo(carInfo);

            synchronized (this) {

                // 입차 프로세스와 동일하게
                // 신규 차량일 경우 새로운 그룹을 생성
                // 차량 정보를 GPMS 서버에 전송
                if (isNewCarEnters(currentTime)) {
                    lastRegTime = currentTime;

                    // 새로운 그룹 생성
                    EventInfoGroup group = addNewGroupToExitQueue(eventInfo);

                    // 1초후 비교로직 실행
                    startTimer();

                /*
                  출차 로직은 다음과 같이 처리
                  1. 1번 사진 정상인식만 경우 출차 이벤트
                  2. 1s 대기
                  3. (보조LPR유)
                      2번 사진 미인식/오인식
                      - 1번 사진 미인식/오인식 인 경우 1번 사진 출차 이벤트
                        1번 사진 정상인식 인 경우 step 1에서 출차 기 처리 되어 skip
                      2번 사진 정상인식
                      - 1번 사진 미인식/오인식 인 경우 skip
                        1번 사진 정상인식 && 2번 사진과 1번 사진 차량번호 상이 한 경우 2번 사진 출차 이벤트
                     (보조LPR무)
                      1번 미인식/오인식 출차 이벤트
                 */

                    // 새로운 스레드를 생성하여
                    // 차량 번호 인식 된 경우에만 GPMS 서버에 차량 정보 생성 후 전송(2021.12.18 by lucy)
                    if (carInfo.ocrValidate()) {
                        new Thread(() -> {
                            gpmsAPI.requestExitCar(group.getKey(), carInfo);
                            carInfo.setRequest(true);
                        }).start();
                    }
                } else {
                    // 신규 입차시 생성된 그룹에 이벤트 정보를 등록.
                    addElementToExitGroup(eventInfo);
                }
            }
        } catch (Exception e) {
            log.error("출차 - 전방 에러 {}", e.getMessage());
        }
    }

    // 새로운 출차차량 그룹을 생성 후 큐에 추가
    public EventInfoGroup addNewGroupToExitQueue(EventInfo eventInfo) {
        List<EventInfo> eventInfoList = new ArrayList<>();
        eventInfoList.add(eventInfo);
        EventInfoGroup group = new EventInfoGroup(eventInfoList);
        exitQueue.offer(group);

        return group;
    }

    // 현재 출차중인 차량정보를 그룹에 추가.
    public void addElementToExitGroup(EventInfo eventInfo) {
        exitQueue.peek()
                .getEventList()
                .add(eventInfo);
    }

    // 출차 큐에 쌓인 그룹을 반환
    public EventInfoGroup pollExitQueue() {
        return exitQueue.poll();
    }

    private void startTimer() {
        new Timer().schedule(task(), timerTime);
    }

    protected TimerTask task() {
        return new TimerTask() {
            @SneakyThrows
            @Override
            public void run() {
                EventInfoGroup eventGroup = pollExitQueue();
                List<CarInfo> carInfos = getCurrentGroupEventList(eventGroup);

                // 마지막에 찍힌 차량 정보
                CarInfo lastCar = carInfos.get(carInfos.size() - 1);

                // 그룹내에 등록된 차량 정보가 두개 이상일 때
                // (보조 LPR 이 달려있을 경우)
                if (carInfos.size() > 1) {
                    // OCR 이 정상 처리된 차량인지 확인
                    if (lastCar.ocrValidate()) {
                        // 정상 처리된 차량일 경우
                        // 차량 리스트를 순회하며
                        // 같은 차량 번호가 아닐 때
                        if (!isEqualsCarNumber(carInfos)) {
                            // 출차 재요청.
                            gpmsAPI.requestExitCar(eventGroup.getKey(), lastCar);
                            lastCar.setRequest(true);
                        }
                        //else {
                        //    CommonUtils.deleteImageFile(carInfo.getFullPath());
                        //}
                    } else {
                        CarInfo firstCar = carInfos.get(0);
                        if (!firstCar.ocrValidate()) {
                            // 1번 미출차 요청.
                            gpmsAPI.requestExitCar(eventGroup.getKey(), firstCar);
                            firstCar.setRequest(true);
                        }
                    }
                } else {
                    // 보조 LPR 이 없을 경우 미인식/오인식 데이터에 대해서는 출차 전방에서 GPMS로 출차 이벤트 발생 안했기 때문에 여기서 처리
                    if (!lastCar.ocrValidate()) {
                        gpmsAPI.requestExitCar(eventGroup.getKey(), lastCar);
                        lastCar.setRequest(true);
                    }
                }

                carInfos.forEach( carInfo ->
                        CommonUtils.deleteImageFile(carInfo.getFullPath())
                );
            }
        };
    }
}
