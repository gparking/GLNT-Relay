package kr.co.glnt.relay.run;

import kr.co.glnt.relay.config.ServerConfig;
import kr.co.glnt.relay.dto.FacilityInfo;
import kr.co.glnt.relay.dto.FacilityInfoPayload;
import kr.co.glnt.relay.web.GpmsAPI;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;

@Order(1)
@Component
public class FacilityInitializer implements Runnable {

    private final ServerConfig config;
    private final GpmsAPI gpmsAPI;

    public FacilityInitializer(ServerConfig config, GpmsAPI gpmsAPI) {
        this.config = config;
        this.gpmsAPI = gpmsAPI;
    }

    @Override
    public void run() {
        List<FacilityInfo> facilityList = gpmsAPI.getParkinglotData(new FacilityInfoPayload(config.getServerKey()));
//        List<FacilityInfo> facilityList = new ObjectMapper().readValue(new ClassPathResource("facilities.json").getFile(), new TypeReference<List<FacilityInfo>>(){});
        config.setFacilityList(facilityList);
        config.setCheckTime(facilityList.get(0).getCheckTime());
    }
}
