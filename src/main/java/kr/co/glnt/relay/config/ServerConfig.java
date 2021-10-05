package kr.co.glnt.relay.config;

import kr.co.glnt.relay.dto.DisplayFormat;
import kr.co.glnt.relay.dto.DisplayMessage;
import kr.co.glnt.relay.dto.DisplayResetMessage;
import kr.co.glnt.relay.dto.FacilityInfo;
import kr.co.glnt.relay.exception.GlntBadRequestException;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@ToString
@Setter @Getter
@Configuration("serverConfig")
@ConfigurationProperties("server-config")
public class ServerConfig {
    private String gpmsUrl;
    private String ngisUrl;
    private String serverKey;
    private String fileErrPath;
    private int checkTime;
    private List<FacilityInfo> facilityList;
    private Map<String, String> breakerCommand;
    private DisplayResetMessage resetMessage;
    // 고정 메세지 포맷 (P0000: 위, P0001: 아래) - "![000/P0001/Y0408/%s%s!]"
    // 흐르는 메세지 포맷 (P0000: 위, P0001: 아래) - "![000/P0001/S1000/Y0408/E0606/%s%s!]"
    private List<String> displayFormat; // = Arrays.asList("", "![000/P0000/Y0004/%s%s!]", "![000/P0001/S1000/Y0408/E0606/%s%s!]");



    public FacilityInfo findByFacilitiesId(String type, String dtFacilitiesId) {
        return facilityList
                .stream()
                .filter(info->info.getDtFacilitiesId().equals(dtFacilitiesId))
                .findFirst()
                .orElseThrow(() -> new GlntBadRequestException("<!> " + type + " 시설아이디를 확인해주세요."));
    }

    public FacilityInfo findFacilityInfoByHost(String host) {
        return facilityList
                .stream()
                .filter(info -> info.generateHost().equals(host))
                .findFirst()
                .orElseThrow(() -> new GlntBadRequestException("<!> 일치하는 host 가 없습니다."));
    }

    public List<FacilityInfo> findBreakerList() {
        return this.facilityList.stream()
                .filter(info -> info.getCategory().equals("BREAKER"))
                .collect(Collectors.toList());
    }

    public List<FacilityInfo> findLprList() {
        return this.facilityList.stream()
                .filter(info -> info.getCategory().equals("LPR") && !info.getIp().equals("0.0.0.0"))
                .collect(Collectors.toList());
    }

    public List<FacilityInfo> findDisplayList() {
        return this.facilityList.stream()
                .filter(info -> info.getCategory().equals("DISPLAY"))
                .collect(Collectors.toList());
    }

    public FacilityInfo findFacilityInfo(FacilityInfo facilityInfo, String category) {
        return this.facilityList.stream()
                .filter(info -> info.getGateId().equals(facilityInfo.getGateId()) && info.getCategory().equals(category))
                .findFirst()
                .orElseThrow(() -> new GlntBadRequestException("<!> 일치하는 시설 정보가 없습니다."));
    }

    public void changeMessageFormat(DisplayFormat displayFormat) {
        if (displayFormat.getLine1().equals("FIX")) {
            this.displayFormat.set(1, "![000/P0000/Y0004/%s%s!]");
        } else {
            this.displayFormat.set(1, "![000/P0000/S1000/Y0004/E0606/%s%s!]");
        }

        if (displayFormat.getLine2().equals("FIX")) {
            this.displayFormat.set(2, "![000/P0001/Y0408/%s%s!]");
        } else {
            this.displayFormat.set(2, "![000/P0001/S1000/Y0408/E0606/%s%s!]");
        }
    }

    public List<String> generateMessageList(List<DisplayMessage.DisplayMessageInfo> messages) {
        Collections.sort(messages, (d1, d2) -> d1.getOrder() > d2.getOrder() ? 1 : -1);

        List<String> messageList = new ArrayList<>();
        for (int j = 0; j < messages.size(); j++) {
            DisplayMessage.DisplayMessageInfo info = messages.get(j);

            String format = displayFormat.get(info.getLine());

            String message = String.format(format, info.getColor(), info.getText());

            messageList.add(message);
        }

        return messageList;
    }

    public List<DisplayMessage.DisplayMessageInfo> getDisplayResetMessage(FacilityInfo facilityInfo) {
        // 양방향 전광판
        if (facilityInfo.getGateType().equals("IN_OUT")) {
            if (facilityInfo.getLprType().contains("IN") || facilityInfo.getLprType() == null) {
                return resetMessage.getIn();
            } else {
                return resetMessage.getOut();
            }
        }
        if (facilityInfo.getGateType().contains("IN")) {
            return resetMessage.getIn();
        } else {
            return resetMessage.getOut();
        }
    }

    public FacilityInfo findFacilityInfoByCategory(FacilityInfo facilityInfo, String category) {
        return findFacilityInfo(facilityInfo, category);
    }
}
