package kr.co.glnt.watcher;

import kr.co.glnt.dto.CarGroup;
import kr.co.glnt.dto.CarInfo;
import kr.co.glnt.dto.ParkinglotInfo;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.util.Objects;
import java.util.TimerTask;

/**
 * 입차 전방 이벤트 처리를 담당하는 클래스.
 */
@Slf4j
public class Entrance extends Breaker {
    private long lastRegTime = System.currentTimeMillis();  // 최종 입차 시간.
    private int MAX_TIME = 1000;    // 발생하는 이벤트들을 하나로 취급할 시간. (현재는 1초)

    public Entrance(ParkinglotInfo parkinglotInfo) {
        super(parkinglotInfo);
    }

    @SneakyThrows
    @Override
    public void startProcessing(String fullPath) {
        // TODO: GPMS SVR 로 파일전송 (base64 || multipart)
        // 1. 이벤트 발생 시간
        long currentTime = System.currentTimeMillis();
        BreakerManager manager = BreakerManager.valueOf(parkinglotInfo.getGateLprType());

        // 2. ocr
        CarInfo carInfo = ngisAPI.requestOCR(fullPath);
        carInfo.setFacilitiesId(parkinglotInfo.getFacilitiesId());
        carInfo.setFullPath(fullPath);
        carInfo.setInDate(currentTime);

        // 3. time check
        if (isNewCarEnters(currentTime)) { // 입차 차량
            lastRegTime = currentTime;
            manager.addNewGroupInFrontQueue(carInfo);
            startTimer(manager);
        }
        else { // 입차중인 차량
            manager.addOcrInfoInGroup(carInfo);
        }

        manager.loggingQueue();

        // 현재 그룹에서 인식된 번호가 있으면 API 호출
        CarGroup carGroup = manager.getCurrentCarGroup();
        if (Objects.isNull(carGroup)) return;

        if (carGroup.isState()) {
            if(!carGroup.isApiCall()) {
                carGroup.stopTimer();
                carGroup.setApiCall(true);
                gpmsAPI.requestEntranceCar(carGroup.getKey(), carInfo);
                manager.addNewGroupInBackQueue();
            }
        }
    }

    boolean isNewCarEnters(long currentTime) {
        return (currentTime - lastRegTime) > MAX_TIME;
    }

    private void startTimer(BreakerManager manager) {
        CarGroup group = manager.getCurrentCarGroup();
        CarInfo info = group.getEventList().get(0);
        TimerTask task = new TimerTask() {
            @Override
            public void run() {
                log.info("타이머 스탑!");
                if (Objects.nonNull(info)) {
                    gpmsAPI.requestEntranceCar(group.getKey(), info);
                }
                manager.addNewGroupInBackQueue();
            }
        };
        group.startTimer(task);
    }
}
