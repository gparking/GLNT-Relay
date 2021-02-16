package kr.co.glnt.relay.run;

import kr.co.glnt.relay.config.ServerConfig;
import kr.co.glnt.relay.dto.FacilityInfo;
import kr.co.glnt.relay.dto.FacilityInfoPayload;
import kr.co.glnt.relay.tcp.GlntNettyClient;
import kr.co.glnt.relay.watcher.GlntFolderWatcher;
import kr.co.glnt.relay.web.GpmsAPI;
import kr.co.glnt.relay.web.NgisAPI;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.DependsOn;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;


/**
 * 애플리케션 실행시 동작하는 Runner 클래스
 */
@Slf4j
@Order(1)
@Component
public class BreakerRunner implements ApplicationRunner {
    private final NgisAPI ngisAPI;
    private final ServerConfig config;

    public BreakerRunner(ServerConfig config, GpmsAPI gpmsAPI, NgisAPI ngisAPI) {
        this.config = config;
        this.ngisAPI = ngisAPI;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        log.info(">>> BreakerRunner run");
        int isOpen = ngisAPI.requestNgisOpen();
        if (isOpen < 0) {
            log.error("인식 모듈 연결이 실패했습니다.");
        }

        List<FacilityInfo> facilityList = config.getFacilityList();

        // 2. data grouping (in gate / out gate)
        Map<String, List<FacilityInfo>> parkingGroup = facilityList.stream()
                .filter(info -> Objects.nonNull(info.getImagePath()))
                .collect(Collectors.groupingBy(FacilityInfo::getImagePath));

        // 2. watcher thread 실행
        parkingGroup.forEach((key, value) -> {
            GlntFolderWatcher watcher = new GlntFolderWatcher(value.get(0));
            new Thread(watcher).start();
        });
    }
}

