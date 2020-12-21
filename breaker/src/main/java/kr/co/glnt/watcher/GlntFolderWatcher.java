package kr.co.glnt.watcher;

import kr.co.glnt.dto.ParkinglotInfo;
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
    private ParkinglotInfo parkinglotInfo;


    @SneakyThrows
    public GlntFolderWatcher(ParkinglotInfo parkinglotInfoList) {
        parkinglotInfo = parkinglotInfoList;
        service = FileSystems.getDefault().newWatchService();

        watchFolder = Paths.get(parkinglotInfo.getImagePath());
        if (!Files.exists(watchFolder)) {
            Files.createDirectories(watchFolder);
        }
        watchFolder.register(service, StandardWatchEventKinds.ENTRY_CREATE);

        log.info(">>> {} Monitoring Start", parkinglotInfo.getImagePath());
    }

    @SneakyThrows
    @Override
    public void run() {
        Breaker breaker = BreakerManager.getInstance(parkinglotInfo);
        for (;;) {
            WatchKey key = service.take();
            List<WatchEvent<?>> events = key.pollEvents();
            for (WatchEvent<?> event: events) {
                WatchEvent.Kind<?> kind = event.kind();
                if (!kind.equals(StandardWatchEventKinds.ENTRY_CREATE)) {
                    break;
                }
                // File 생성 후 파일 생성시간을 체크.
                StringBuilder builder = new StringBuilder();
                String fileName = event.context().toString();
                String fullPath = builder.append(parkinglotInfo.getImagePath())
                                        .append("\\")
                                        .append(fileName)
                                        .toString();
                breaker.startProcessing(fullPath);

            }
            key.reset();
        }
    }


}
