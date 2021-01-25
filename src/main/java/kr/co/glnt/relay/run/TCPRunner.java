package kr.co.glnt.relay.run;

import kr.co.glnt.relay.config.ServerConfig;
import kr.co.glnt.relay.dto.FacilityInfo;
import kr.co.glnt.relay.dto.FacilityStatus;
import kr.co.glnt.relay.dto.FacilityStatusWrapper;
import kr.co.glnt.relay.tcp.GlntNettyClient;
import kr.co.glnt.relay.web.GpmsAPI;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Order(2)
@Slf4j
@Component
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
        List<FacilityInfo> facilityInfos = serverConfig.getFacilityList().stream()
                .filter(info -> info.getPort() > 0)
                .collect(Collectors.toList());

        client.setFeatureCount(facilityInfos.size());

        facilityInfos.forEach(info ->
                client.connect(info.getIp(), info.getPort())
        );
    }

    // 10분에 한번씩 연결된 디바이스 상태체크.
    @Scheduled(fixedRate = (1000 * 60) * 10, initialDelay = 1000 * 10)
    public void devicesHealthCheck() {
        gpmsAPI.sendFacilityList(new FacilityStatusWrapper(client.getChannelsStatus()));

    }

    // 10분에 한번씩 LPR ping check
    @Scheduled(fixedRate = (1000 * 60) * 10, initialDelay = 1000 * 10)
    public void lprPingCheck() {
        List<FacilityInfo> lprList = serverConfig.getFacilityList().stream()
                .filter(info -> info.getCategory().equals("LPR"))
                .collect(Collectors.toList());

        List<FacilityStatus> statusList = new ArrayList<>();
        lprList.stream().forEach(info -> {
            try {
                boolean isActive = InetAddress.getByName(info.getIp()).isReachable(1000);
                String healthStatus = isActive
                        ? "normal"
                        : "noResponse";
                statusList.add(new FacilityStatus(info.getFacilitiesId(), healthStatus));
            } catch (IOException e) {
                log.error("LPR 상태 체크 에러: ", e.getMessage());
            }
        });
        gpmsAPI.sendFacilityList(new FacilityStatusWrapper(statusList));

    }
}
