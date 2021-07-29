package kr.co.glnt.relay.run;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import kr.co.glnt.relay.config.ServerConfig;
import kr.co.glnt.relay.dto.*;
import kr.co.glnt.relay.tcp.GlntNettyClient;
import kr.co.glnt.relay.watcher.GlntFolderWatcher;
import kr.co.glnt.relay.web.GpmsAPI;
import kr.co.glnt.relay.web.NgisAPI;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.Trigger;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.support.PeriodicTrigger;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.nio.charset.Charset;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Order(1)
@Component
public class AppRunner {
    private ThreadPoolTaskScheduler scheduler;
    private GlntNettyClient client;
    private ServerConfig config;
    private GpmsAPI gpmsAPI;
    private NgisAPI ngisAPI;
    private ObjectMapper mapper;


    private ConfigurableEnvironment env;


    public AppRunner(GlntNettyClient client, ServerConfig config, GpmsAPI gpmsAPI, NgisAPI ngisAPI, ObjectMapper mapper, ConfigurableEnvironment env) {
        this.client = client;
        this.config = config;
        this.gpmsAPI = gpmsAPI;
        this.ngisAPI = ngisAPI;
        this.mapper = mapper;
        this.env = env;
    }

    @PostConstruct
    public void init() {


        String profiles = env.getProperty("server-config.lpr-on");
        initFacilityInfos();
        initDisplayResetMessage();
        initDisplayFormat();
        deviceConnect();
        if (profiles.equals("ON")) lprRunner();
        startScheduler();
    }

    // 시설물 정보 가져오기
    private void initFacilityInfos() {
        List<FacilityInfo> facilityList = gpmsAPI.getParkinglotData(new FacilityInfoPayload(config.getServerKey()));
        if (facilityList.size() > 0) {
            config.setFacilityList(facilityList);
        }
    }

    // 전광판 리셋 메세지 가져오기
    private void initDisplayResetMessage() {
        ResponseDTO response = gpmsAPI.requestDisplayInitMessage();

        if (Objects.isNull(response)) {
            log.error("<!> 전광판 초기 메세지 가져오기 실패: response is null");
            return;
        }

        if (response.getCode() != HttpStatus.OK.value()) {
            log.error("<!> 전광판 초기 메세지 가져오기 실패: {}", response.getMsg());
            return;
        }

        // data 를 DisplayResetMessage 로 변환.
        List<DisplayResetMessage> displayResetMessages = mapper.convertValue(response.getData(), new TypeReference<List<DisplayResetMessage>>() {});

        if (displayResetMessages.size() == 0) {
            log.error("<!> 전광판 초기 메세지가 없습니다.");
            return;
        }

        config.setResetMessage(displayResetMessages.get(0));

    }

    private void initDisplayFormat() {
        ResponseDTO responseDTO = gpmsAPI.requestDisplayFormat();
        HttpStatus status = HttpStatus.valueOf(responseDTO.getCode());
        if (status != HttpStatus.OK) {
            log.error("<!> 전광판 초기 포맷설정을 실패했습니다 / {}", responseDTO.getMsg());
            return;
        }

        DisplayFormat displayFormat = mapper.convertValue(responseDTO.getData(), DisplayFormat.class);
        config.changeMessageFormat(displayFormat);
        log.info(">>>> display format: {}", displayFormat);
    }


    // 디바이스 TCP 연결
    private void deviceConnect() {
        List<FacilityInfo> facilityInfos = config.getFacilityList().stream()
                .filter(info -> info.getPort() > 0)
                .collect(Collectors.toList());

        // new
        client.setConnectionList(facilityInfos);
        client.connect();

        // old
//        client.setFeatureCount(facilityInfos.size());
//        facilityInfos.forEach(info -> {
//            client.connect(info.getIp(), info.getPort());
//        });
    }

    // 폴더 감지 시작.
    private void lprRunner() {
        log.info(">>>> LPR 연결 시작");
        int isOpen = ngisAPI.requestNgisOpen();
        if (isOpen < 0) {
            log.error("<!> 인식 모듈 연결이 실패했습니다.");
        }

        List<FacilityInfo> facilityList = config.getFacilityList();

        // 1. data grouping (in gate / out gate)
        // image path 가 존재하는 디렉터리들만 따로 추출.
        Map<String, FacilityInfo> lprGroup = facilityList.stream()
                .filter(info -> Objects.nonNull(info.getImagePath()) && !info.getImagePath().isEmpty())
                .collect(Collectors.toMap(i -> i.getImagePath(), j -> j));


        // 2. gateId 로 게이트별 lpr 을 묶고
        Map<String, List<FacilityInfo>> gateIdGroup = lprGroup.values().stream()
                .collect(Collectors.groupingBy(FacilityInfo::getGateId));


        // 3. watcher thread 실행
        lprGroup.forEach((key, value) -> {
            // gateType + lprType 을 키로 시설정보를 가지고있는 Map 을 생성
            Map<String, FacilityInfo> gateLprMap = gateIdGroup.get(value.getGateId()).stream()
                    .collect(Collectors.toMap(i -> i.generateGateLprType(), j -> j));

            // 각 watcher 에 group 을 저장
            GlntFolderWatcher watcher = new GlntFolderWatcher(value, gateLprMap);
            new Thread(watcher).start();
        });
    }

    // 차단기 상태 체크 스케쥴러
    public void startScheduler() {
        scheduler = new ThreadPoolTaskScheduler();
        scheduler.initialize();
        scheduler.schedule(getRunnable(), getTrigger());
    }

    private Runnable getRunnable() {
        return () -> {
            List<FacilityStatus> alarmList = config.findBreakerList().stream()
                    .filter(info -> {
                        // 현재 시간과 마지막 액션 시간 차이 구하고
                        long minute = ChronoUnit.MINUTES.between(info.getLastActionTime(), LocalDateTime.now());
                        // 상태가 30분 이상 지속되었을 때
                        return minute >= config.getCheckTime() && info.getBarStatus().equals("GATE UP OK");
                    })
                    .map(info -> FacilityStatus.gateLongTimeOpen(info.getFacilitiesId()))
                    .collect(Collectors.toList());

            if (alarmList.size() > 0) {
                gpmsAPI.sendFacilityAlarm(FacilityPayloadWrapper.facilityAlarmPayload(alarmList));
            }
        };
    }


    public Trigger getTrigger() {
        log.info(">>>> 차단기 상태 확인 설정된 시간: {}", config.getCheckTime());
        return new PeriodicTrigger(config.getCheckTime(), TimeUnit.MINUTES);
    }

}
