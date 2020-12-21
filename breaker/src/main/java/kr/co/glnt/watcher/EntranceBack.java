package kr.co.glnt.watcher;

import kr.co.glnt.dto.CarGroup;
import kr.co.glnt.dto.ParkinglotInfo;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

/**
 * 입차 후방이벤트를 담당하는 클래스.
 * 입차 전방 이벤트 그룹의 상태값을 체크 후 ocr 인식을 못했으면 후방에서도 api호출.
 */
@Slf4j
public class EntranceBack extends Breaker {

    public EntranceBack(ParkinglotInfo parkinglotInfo) {
        super(parkinglotInfo);
    }

    @SneakyThrows
    @Override
    public void startProcessing(String fullPath) {
        BreakerManager manager = BreakerManager.valueOf(parkinglotInfo.getGateLprType());
        CarGroup carGroup = manager.getCurrentCarGroup();

    }
}
