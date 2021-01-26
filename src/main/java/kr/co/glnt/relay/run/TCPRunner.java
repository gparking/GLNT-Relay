package kr.co.glnt.relay.run;

import kr.co.glnt.relay.config.ServerConfig;
import kr.co.glnt.relay.dto.FacilityInfo;
import kr.co.glnt.relay.dto.FacilityPayloadWrapper;
import kr.co.glnt.relay.tcp.GlntNettyClient;
import kr.co.glnt.relay.web.GpmsAPI;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.DependsOn;
import org.springframework.core.annotation.Order;
import org.springframework.scheduling.annotation.Scheduled;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Order(2)
@DependsOn("FacilityInitialize")
public class TCPRunner implements ApplicationRunner {
    private final ServerConfig serverConfig;
    private final GlntNettyClient client;
    private final GpmsAPI gpmsAPI;

    public TCPRunner(ServerConfig serverConfig, GlntNettyClient client, GpmsAPI gpmsAPI) {
        this.serverConfig = serverConfig;
        this.client = client;
        this.gpmsAPI = gpmsAPI;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        log.info(">>> BreakerRunner run");
        List<FacilityInfo> facilityInfos = serverConfig.getFacilityList().stream()
                .filter(info -> info.getPort() > 0)
                .collect(Collectors.toList());

        client.setFeatureCount(facilityInfos.size());

        facilityInfos.forEach(info -> {
//            client.connect(info.getIp(), info.getPort());
            client.connect("192.168.20.121", 4001);
        });
    }

    // 10분에 한번씩 연결된 디바이스 상태체크.
    @Scheduled(fixedRate = (1000 * 60) * 10, initialDelay = 1000 * 10)
    public void devicesHealthCheck() {
        gpmsAPI.sendFacilityHealth(FacilityPayloadWrapper.healthCheckPayload(client.getChannelsStatus()));
    }

    // 10분에 한번씩 LPR ping check
    @Scheduled(fixedRate = (1000 * 60) * 10, initialDelay = 1000 * 10)
    public void lprPingCheck() {
        gpmsAPI.sendFacilityHealth(FacilityPayloadWrapper.healthCheckPayload(client.getFullLprStatus()));
    }

}
