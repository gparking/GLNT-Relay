package kr.co.glnt.relay.dto;

import kr.co.glnt.relay.common.CmdStatus;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * 시설물 정보 클래스.
 */
@Data
@Slf4j
public class FacilityInfo {
    private CmdStatus cmdStatus = CmdStatus.NORMAL;
    private List<DisplayMessage.DisplayMessageInfo> facilityMessage;
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
    private int checkTime;      // 차단기 상태 확인 반복시간
    private boolean active;     // 양방향 통신일때 차단기 활성화 여부. (입차시에는 true, 출차시에는 false)
    private LocalDateTime lastActionTime = LocalDateTime.now();
    private Queue<String> openMessageQueue = new LinkedList<>();    // 차량이 갇히는거를 방지하기
                                                                    // 위해 open 메세지는 모아놨다가 닫힐때 연다.
    private Timer timer = new Timer();  // openMessageQueue 리셋용 타이머
                                        // 전광판 리셋으로도 사용.

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


    public void addOpenMessage(String msg) {
        this.openMessageQueue.offer(msg);
        messageResetTimerStart();
    }

    private void messageResetTimerStart() {
        timer.cancel();
        timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                openMessageQueue.clear();
                log.info(">>>> {} 오픈 메세지 큐 초기화", fname);
            }
        }, (60 * 1000) * 5);
    }
}
