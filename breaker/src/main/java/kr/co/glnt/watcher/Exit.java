package kr.co.glnt.watcher;

import kr.co.glnt.dto.ParkinglotInfo;
import lombok.extern.slf4j.Slf4j;

// 출차
@Slf4j
public class Exit extends Breaker {

    public Exit(ParkinglotInfo parkinglotInfo) {
        super(parkinglotInfo);
    }

    @Override
    public void startProcessing(String fileName) {
        log.info("parkinglotInfo : {}", parkinglotInfo);
    }

}
