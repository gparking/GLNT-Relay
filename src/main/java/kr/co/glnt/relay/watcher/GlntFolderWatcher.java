package kr.co.glnt.relay.watcher;

import kr.co.glnt.relay.common.CommonUtils;
import kr.co.glnt.relay.dto.EventInfo;
import kr.co.glnt.relay.dto.FacilityInfo;
import kr.co.glnt.relay.service.Breaker;
import kr.co.glnt.relay.service.BreakerFactory;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 디렉토리 모니터링 클래스
 * in_front, in_back, out_front 당 하나씩.
 */
@Slf4j
public class GlntFolderWatcher implements Runnable {
    private Path watchFolder;
    private WatchService service;
    private FacilityInfo facilityInfo;
    private Map<String, FacilityInfo> gateLprGroup;

    private static final Pattern JPEG_FILE_EXTENSIONS = Pattern.compile("^.+\\.(?i)(?:jpe?g)$");
    /** Empty file initial retry, in ms. */
    private static final long INITIAL_RETRY = 50L;
    /** Empty file maximum retry, in ms. */
    private static final long MAXIMUM_RETRY = 2000L;

    @SneakyThrows
    public GlntFolderWatcher(FacilityInfo facilityInfo, Map<String, FacilityInfo> gateLprGroup) {
        this.facilityInfo = facilityInfo;
        this.gateLprGroup = gateLprGroup;
        service = FileSystems.getDefault().newWatchService();
        watchFolder = Paths.get(facilityInfo.getImagePath());

        if (!watchFolder.toFile().exists()) {
            Files.createDirectories(watchFolder);
        }
        watchFolder.register(service, StandardWatchEventKinds.ENTRY_CREATE);
    }

    @SneakyThrows
    @Override
    public void run() {
        Breaker breaker = BreakerFactory.getInstance(facilityInfo);
        for (;;) {
            WatchKey key = null;
            key = service.take();
            List<WatchEvent<?>> events = key.pollEvents();

            for (WatchEvent<?> event : events) {
                WatchEvent.Kind<?> kind = event.kind();
                if (!kind.equals(StandardWatchEventKinds.ENTRY_CREATE)) {
                    continue;
                }

                //파일 정합성 check
                String fullPath = getFullPath(event);
                if (!isValidationImageFile(Paths.get(fullPath))) continue;

                log.info(">>>> {}({}) 파일 생성: {}, size: {} bytes", facilityInfo.getFname(), facilityInfo.getDtFacilitiesId(), fullPath, fullPath.length());
                switch (BreakerFactory.valueOf(facilityInfo.generateGateLprType())) {
                    // 보조 LPR 이 달려있을 경우 동시에 사진이 들어와
                    // 이벤트 발생 시간을 정확하게 체크하기 위해 신규 스레드 생성.
                    case IN_OUTINFRONT:
                        if (gateLprGroup.containsKey("IN_OUTINBACK")) {
                            gateLprGroup.get("IN_OUTINBACK").setActive(true);
                        }
                    case INFRONT:
                    case OUTFRONT:
                        new Thread(() ->
                                breaker.startProcessing(new EventInfo(fullPath))
                        ).start();
                        break;


                    case IN_OUTINBACK:
                        // 양방향 게이트일 경우 시설물 액티브 상태를 확인
                        // 활성: 차량 입차
                        // 비활성: 차량 출차
                        // 출차 중일때 해당 이벤트 무시.
                        if (!gateLprGroup.get("IN_OUTINBACK").isActive()) {
                            CommonUtils.deleteImageFile(fullPath);
                            break;
                        }
                        breaker.startProcessing(new EventInfo(fullPath));
                        break;


                    case IN_OUTOUTFRONT:
                        if (gateLprGroup.containsKey("IN_OUTINBACK")) {
                            gateLprGroup.get("IN_OUTINBACK").setActive(false);
                        }
                    default:
                        breaker.startProcessing(new EventInfo(fullPath));
                        break;
                }
            }
            key.reset();
        }
    }

    private String getFullPath(WatchEvent<?> event) {
        StringBuilder builder = new StringBuilder();
        String fileName = event.context().toString();
        return builder.append(facilityInfo.getImagePath())
                .append("\\")
                .append(fileName)
                .toString();
    }

    private boolean isValidationImageFile(final Path path) {
        Matcher matcher = JPEG_FILE_EXTENSIONS.matcher(path.toString());
        if (matcher.matches()) {
            try
            {
                // block until file is non-empty
                //waitForNonEmptyFile(path);

                for (long retry = INITIAL_RETRY; retry < MAXIMUM_RETRY; retry *= 2)
                {
                    if (isNonEmpty(path))
                    {
                        return true;
                    }
                    try
                    {
                        log.trace("  waiting " + retry + "ms for empty file...");
                        Thread.currentThread().sleep(retry);
                    }
                    catch (InterruptedException e)
                    {
                        // ok
                    }
                }
                throw new IOException("path " + path + " is empty");
            }
            catch (IOException e)
            {
                log.warn("unable to create file, " + e.getMessage());

                try
                {
                    // delete problematic file from watch directory
                    log.info("deleting " + path);
                    Files.delete(path);
                }
                catch (IOException ioe)
                {
                    // ignore
                }
                return false;
            }
        } else
            return false;
    }

    void waitForNonEmptyFile(final Path path) throws IOException
    {
        for (long retry = INITIAL_RETRY; retry < MAXIMUM_RETRY; retry *= 2)
        {
            if (isNonEmpty(path))
            {
                return;
            }
            try
            {
                log.trace("  waiting " + retry + "ms for empty file...");
                Thread.currentThread().sleep(retry);
            }
            catch (InterruptedException e)
            {
                // ok
            }
        }
        throw new IOException("path " + path + " is empty");
    }

    static boolean isNonEmpty(final Path path) throws IOException
    {
        BasicFileAttributes attr = Files.readAttributes(path, BasicFileAttributes.class);
        return attr.isRegularFile() && (attr.size() > 0L);
    }
}
