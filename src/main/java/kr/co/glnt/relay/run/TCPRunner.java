package kr.co.glnt.relay.run;

import kr.co.glnt.relay.breaker.dto.FacilityInfo;
import kr.co.glnt.relay.common.config.ServerConfig;
import kr.co.glnt.relay.tcp.client.BreakerClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Order(2)
@Slf4j
@Component
public class TCPRunner implements ApplicationRunner {

    private final ServerConfig serverConfig;
    private final BreakerClient client;

    public TCPRunner(ServerConfig serverConfig, BreakerClient client) {
        this.serverConfig = serverConfig;
        this.client = client;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        List<FacilityInfo> facilityInfos = serverConfig.getFacilityList().stream()
                .filter(info -> info.getPort() > 0)
                .collect(Collectors.toList());
        client.setFeatureCount(facilityInfos.size());
        facilityInfos.forEach(info -> {
            client.connect(info.getIp(), info.getPort());
        });




    }
}
