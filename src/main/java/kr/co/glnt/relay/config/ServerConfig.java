package kr.co.glnt.relay.config;

import kr.co.glnt.relay.dto.FacilityInfo;
import kr.co.glnt.relay.exception.GlntBadRequestException;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.Map;

@Setter @Getter
@Configuration
@ConfigurationProperties("spring.server-config")
public class ServerConfig {
    private String gpmsUrl;
    private String ngisUrl;
    private String serverName;
    private String fileErrPath;
    private List<FacilityInfo> facilityList;
    private Map<String, String> breakerCommand;


    public FacilityInfo findByFacilitiesId(String facilitiesId) {
        return facilityList
                .stream()
                .filter(info->info.getFacilitiesId().equals(facilitiesId))
                .findFirst()
                .orElseThrow(() -> new GlntBadRequestException("시설아이디를 확인해주세요."));
    }

    public FacilityInfo findFacilityInfoByHost(String host) {
        return facilityList
                .stream()
                .filter(info -> info.generateHost().equals(host))
                .findFirst()
                .orElseThrow(() -> new GlntBadRequestException("일치하는 host 가 없습니다."));
    }

}
