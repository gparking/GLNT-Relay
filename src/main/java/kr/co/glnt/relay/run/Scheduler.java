package kr.co.glnt.relay.run;

import kr.co.glnt.relay.config.ServerConfig;
import kr.co.glnt.relay.dto.FacilityInfo;
import kr.co.glnt.relay.dto.FacilityPayloadWrapper;
import kr.co.glnt.relay.dto.FacilityStatus;
import kr.co.glnt.relay.web.GpmsAPI;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Slf4j
@Order(2)
@Component
public class Scheduler {
    private final ServerConfig config;
    private final GpmsAPI api;

    public Scheduler(ServerConfig config, GpmsAPI api) {
        this.config = config;
        this.api = api;
    }

    // LPR 연결 상태 전송하기
    @Scheduled(fixedDelay = 1000)
    public void lprPingTask() {
        try {
            List<FacilityStatus> statusList = new ArrayList<>();
            for (FacilityInfo info : config.findLprList()) {
                if (Objects.nonNull(info.getDtFacilitiesId()) && !info.getDtFacilitiesId().isEmpty()) {
                    String healthStatus = sendPing(info.getIp());
                    statusList.add(FacilityStatus.deviceHealth(info.getDtFacilitiesId(), healthStatus));
                }
            }

            api.sendFacilityHealth(FacilityPayloadWrapper.healthCheckPayload(statusList));
        } catch (Exception e) {
            log.error("<!> LPR STATUS CHECK ERROR: {}", e.getMessage());
        }
    }


    public String sendPing(String ip) throws Exception {
        boolean isActive = InetAddress.getByName(ip).isReachable(1500);
        return isActive
                ? "NORMAL"
                : "ERROR";
    }
}
