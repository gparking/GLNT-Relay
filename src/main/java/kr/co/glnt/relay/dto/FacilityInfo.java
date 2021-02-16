package kr.co.glnt.relay.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * 시설물 정보 클래스.
 */
@Data
public class FacilityInfo {
    private int sn;
    private String category;
    private String modelid;
    private String dtFacilitiesId;
    private String facilitiesId;
    private int flagUse;
    private String gateId;
    private String udpGateId;
    private String ip;
    private int port;
    private int sortCount;
    private int resetPort;
    private int flagConnect;
    private String gateType;
    private String lprType;
    private String imagePath;
    private String gateSvrKey;
    private String createdBy;
    private String updateBy;
    private String fname;
    private String barStatus = "init";     // 차단기 Open/Close/UpLock
    private int passCount;
    private boolean state;      // 차단기 전원 상태
    private LocalDateTime lastActionTime = LocalDateTime.now();
    private int checkTime;

    public void setBarStatus(String barStatus) {
        this.barStatus = barStatus;
        this.lastActionTime = LocalDateTime.now();
    }

    public String generateGateLprType() {
        return Objects.nonNull(this.lprType)
                ? this.gateType.concat(this.lprType)
                : this.gateType;
    }

    public String generateHost() {
        return String.format("%s:%d", ip, port);
    }
}
