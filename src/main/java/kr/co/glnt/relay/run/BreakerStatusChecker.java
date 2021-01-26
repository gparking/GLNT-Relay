package kr.co.glnt.relay.run;

import kr.co.glnt.relay.config.ServerConfig;
import kr.co.glnt.relay.dto.FacilityAlarm;
import kr.co.glnt.relay.dto.FacilityPayloadWrapper;
import kr.co.glnt.relay.web.GpmsAPI;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.DependsOn;
import org.springframework.core.annotation.Order;
import org.springframework.scheduling.Trigger;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.support.PeriodicTrigger;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Order(3)
@Component
public class BreakerStatusChecker implements ApplicationRunner {
    private ThreadPoolTaskScheduler scheduler;
    private final GpmsAPI gpmsAPI;
    private final ServerConfig config;

    public BreakerStatusChecker(GpmsAPI gpmsAPI, ServerConfig config) {
        this.gpmsAPI = gpmsAPI;
        this.config = config;
    }


    @Override
    public void run(ApplicationArguments args) throws Exception {
        startScheduler();
    }

    public void stopScheduler() {
        scheduler.shutdown();
    }

    public void startScheduler() {
        scheduler = new ThreadPoolTaskScheduler();
        scheduler.initialize();
        scheduler.schedule(getRunnable(), getTrigger());
    }

    private Runnable getRunnable() {
        return () -> {
            List<FacilityAlarm> alarmList = config.findBreakerList().stream()
                    .filter(info -> {
                        // 현재 시간과 마지막 액션 시간 차이 구하고
                        long minute = ChronoUnit.MINUTES.between(info.getLastActionTime(), LocalDateTime.now());
                        // 상태가 30분 이상 지속되었을 때
                        return minute >= 30 && info.getBarStatus().equals("GATE UP OK");
                    })
                    .map(info -> FacilityAlarm.gateLongTimeOpen(info.getFacilitiesId()))
                    .collect(Collectors.toList());

            if (alarmList.size() > 0) {
                gpmsAPI.sendFacilityAlarm(FacilityPayloadWrapper.facilityAlarmPayload(alarmList));
            }
        };
    }


    public Trigger getTrigger() {
        // todo: 시간 설정. config에서 가져와야함.
        System.out.println("Breaker status checker set time : " + config.getCheckTime());
        return new PeriodicTrigger(config.getCheckTime(), TimeUnit.MINUTES);
    }

}
