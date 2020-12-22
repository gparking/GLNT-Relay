package kr.co.glnt.relay.breaker.dto;

import lombok.Data;

import java.util.Objects;

/**
 * 게이트 정보 클래스.
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

    public String getGateLprType() {
        return Objects.nonNull(this.lprType)
                ? this.gateType.concat(this.lprType)
                : this.gateType;
    }

}
