package kr.co.glnt.relay.common.config;

import kr.co.glnt.relay.breaker.dto.FacilityInfo;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Setter @Getter
@Configuration
@ConfigurationProperties("spring.server-config")
public class ServerConfig {
    private String gpmsUrl;
    private String ngisUrl;
    private String serverName;
    private String fileErrPath;
    private List<FacilityInfo> facilityList;
}
