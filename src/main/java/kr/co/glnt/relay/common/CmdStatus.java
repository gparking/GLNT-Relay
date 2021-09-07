package kr.co.glnt.relay.common;

import kr.co.glnt.relay.dto.FacilityInfo;

public enum CmdStatus {
    NORMAL,
    EXIT_STANDBY;

    public static CmdStatus getCmdStatus(FacilityInfo facilityInfo){
        return facilityInfo.getCmdStatus();
    }
}
