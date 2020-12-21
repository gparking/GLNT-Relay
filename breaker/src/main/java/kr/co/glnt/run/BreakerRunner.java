package kr.co.glnt.run;

import kr.co.glnt.api.GpmsAPI;
import kr.co.glnt.api.NgisAPI;
import kr.co.glnt.config.ServerConfig;
import kr.co.glnt.dto.ParkinglotInfo;
import kr.co.glnt.dto.payload.ParkingInfoPayload;
import kr.co.glnt.watcher.GlntFolderWatcher;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 애플리케션 실행시 동작하는 Runner 클래스
 */
@Slf4j
@Component
public class BreakerRunner implements ApplicationRunner {
    private final ServerConfig config;
    private final GpmsAPI gpmsAPI;
    private final NgisAPI ngisAPI;
    public static Queue<String> entranceQueue = new LinkedList<>();

    public BreakerRunner(ServerConfig config, GpmsAPI gpmsAPI, NgisAPI ngisAPI) {
        this.config = config;
        this.gpmsAPI = gpmsAPI;
        this.ngisAPI = ngisAPI;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        // 1. get parking lot info data
        List<ParkinglotInfo> featureList = gpmsAPI.getParkinglotData(new ParkingInfoPayload(config.getServerName()));
        if (Objects.isNull(featureList) || featureList.size() == 0) {
            log.error("주차장 게이트정보 조회를 실패하여 프로그램을 종료합니다.");
            System.exit(0);
        }

        int isOpen = ngisAPI.requestNgisOpen();
        if (isOpen < 0) {
            log.error("인식 모듈 연결이 실패했습니다.");
            System.exit(0);
        }

        // 2. data grouping (in gate / out gate)
        Map<String, List<ParkinglotInfo>> parkingGroup = featureList.stream()
                .filter(info -> Objects.nonNull(info.getImagePath()))
                .collect(Collectors.groupingBy(ParkinglotInfo::getImagePath));


        // 2. watcher thread 실행
        parkingGroup.forEach((key, value) -> {
            GlntFolderWatcher watcher = new GlntFolderWatcher(value.get(0));
            Thread watcherThread = new Thread(watcher);
            watcherThread.setName(value.get(0).getGateLprType());
            watcherThread.start();
        });

//        new Thread(new EntranceQueue(ngisAPI)).start();

    }

}
