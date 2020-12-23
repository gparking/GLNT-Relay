package kr.co.glnt.relay.breaker.watcher;

import kr.co.glnt.relay.breaker.dto.EventInfo;
import kr.co.glnt.relay.breaker.dto.FacilityInfo;
import kr.co.glnt.relay.breaker.service.Breaker;
import kr.co.glnt.relay.breaker.service.BreakerManager;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.nio.file.*;
import java.util.List;

/**
 * 디렉토리 모니터링 클래스
 * in_front, in_back, out_front 당 하나씩.
 */
@Slf4j
public class GlntFolderWatcher implements Runnable {
    private Path watchFolder;
    private WatchService service;
    private FacilityInfo facilityInfo;


    @SneakyThrows
    public GlntFolderWatcher(FacilityInfo facilityInfoList) {
        facilityInfo = facilityInfoList;
        service = FileSystems.getDefault().newWatchService();
        watchFolder = Paths.get(facilityInfo.getImagePath());
        if (!Files.exists(watchFolder)) {
            Files.createDirectories(watchFolder);
        }
        watchFolder.register(service, StandardWatchEventKinds.ENTRY_CREATE);

        log.info(">>> {} Monitoring Start", facilityInfo.getImagePath());
    }

    @SneakyThrows
    @Override
    public void run() {

        Breaker breaker = BreakerManager.getInstance(facilityInfo);
        for (;;) {
            WatchKey key = null;
            key = service.take();
            List<WatchEvent<?>> events = key.pollEvents();
            for (WatchEvent<?> event: events) {
                WatchEvent.Kind<?> kind = event.kind();
                if (!kind.equals(StandardWatchEventKinds.ENTRY_CREATE)) continue;
                switch (BreakerManager.valueOf(facilityInfo.getGateLprType())) {
                    case INFRONT:
                        new Thread(() -> {
                            // File 생성 후 파일 생성시간을 체크.
                            StringBuilder builder = new StringBuilder();
                            String fileName = event.context().toString();
                            String fullPath = builder.append(facilityInfo.getImagePath())
                                    .append("\\")
                                    .append(fileName)
                                    .toString();
                            breaker.startProcessing(new EventInfo(fullPath));
                        }).start();
                        break;


                    default:
                        // File 생성 후 파일 생성시간을 체크.
                        StringBuilder builder = new StringBuilder();
                        String fileName = event.context().toString();
                        String fullPath = builder.append(facilityInfo.getImagePath())
                                .append("\\")
                                .append(fileName)
                                .toString();
//                    breaker.startProcessing(fullPath);
                        breaker.startProcessing(new EventInfo(fullPath));
                        break;
                }
            }
            key.reset();
        }
    }


}
