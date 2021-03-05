package kr.co.glnt.relay.config;

import kr.co.glnt.relay.dto.DisplayResetMessage;
import kr.co.glnt.relay.dto.FacilityInfo;
import kr.co.glnt.relay.exception.GlntBadRequestException;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@ToString
@Setter @Getter
@Configuration
@ConfigurationProperties("spring.server-config")
public class ServerConfig {
    private String gpmsUrl;
    private String ngisUrl;
    private String serverKey;
    private String fileErrPath;
    private int checkTime;
    private List<FacilityInfo> facilityList;
    private Map<String, String> breakerCommand;
    private DisplayResetMessage resetMessage;


    public FacilityInfo findByFacilitiesId(String dtFacilitiesId) {
        return facilityList
                .stream()
                .filter(info->info.getDtFacilitiesId().equals(dtFacilitiesId))
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

    public List<FacilityInfo> findBreakerList() {
        return this.facilityList.stream()
                .filter(info -> info.getCategory().equals("BREAKER"))
                .collect(Collectors.toList());
    }

    public List<FacilityInfo> findLprList() {
        return this.facilityList.stream()
                .filter(info -> info.getCategory().equals("LPR"))
                .collect(Collectors.toList());
    }

    public List<FacilityInfo> findDisplayList() {
        return this.facilityList.stream()
                .filter(info -> info.getCategory().equals("DISPLAY"))
                .collect(Collectors.toList());
    }

}
