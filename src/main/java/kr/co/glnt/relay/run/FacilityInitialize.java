package kr.co.glnt.relay.run;

import kr.co.glnt.relay.config.ServerConfig;
import kr.co.glnt.relay.dto.FacilityInfo;
import kr.co.glnt.relay.dto.FacilityInfoPayload;
import kr.co.glnt.relay.web.GpmsAPI;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Order(0)
@Component
public class FacilityInitialize implements ApplicationRunner {

    private final ServerConfig config;
    private final GpmsAPI gpmsAPI;

    public FacilityInitialize(ServerConfig config, GpmsAPI gpmsAPI) {
        this.config = config;
        this.gpmsAPI = gpmsAPI;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        log.info(">>> FacilityInitialize run");
        List<FacilityInfo> facilityList = gpmsAPI.getParkinglotData(new FacilityInfoPayload(config.getServerKey()));
//        List<FacilityInfo> facilityList = new ObjectMapper().readValue(new ClassPathResource("facilities.json").getFile(), new TypeReference<List<FacilityInfo>>(){});
        config.setFacilityList(facilityList);

        facilityList = facilityList.stream()
                .filter(facilityInfo -> facilityInfo.getCheckTime() > 0)
                .collect(Collectors.toList());
        if (facilityList.size() > 0) {
            config.setCheckTime(facilityList.get(0).getCheckTime());
        }
    }
}
