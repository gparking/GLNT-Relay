package kr.co.glnt.relay.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import kr.co.glnt.relay.common.BreakerActionTarget;
import kr.co.glnt.relay.dto.FacilityInfo;
import kr.co.glnt.relay.dto.FacilityPayloadWrapper;
import kr.co.glnt.relay.dto.FacilityStatus;
import kr.co.glnt.relay.web.GpmsAPI;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

@Slf4j
@Service
public class BreakerService {

    private static final Pattern MESSAGE = Pattern.compile("[^a-zA-Z\\s]");
    private BreakerActionTarget breakerActionTarget = BreakerActionTarget.NORMAL;
    private final GpmsAPI gpmsAPI;
    private final DisplayService displayService;
    private final ObjectMapper objectMapper;

    public BreakerService(GpmsAPI gpmsAPI,DisplayService displayService, ObjectMapper objectMapper) {
        this.gpmsAPI = gpmsAPI;
        this.displayService = displayService;
        this.objectMapper = objectMapper;
    }

    public void breakerProcessing(Channel channel, FacilityInfo facilityInfo, String message) {
        String id = facilityInfo.getDtFacilitiesId();
        String msg = MESSAGE.matcher(message).replaceAll("");
        log.info(">>>> {}({}) 메세지 수신: {}", facilityInfo.getFname(), id, msg);

        if (facilityInfo.getGateType().contains("OUT")) {
            exitBreakerTask(facilityInfo, msg);
        } else {
            // DetectOut 을 밟았을 경우
            if (message.contains("DET OUT")) {
                // 입차 대기 큐에서 하나를 빼고
                String rebound = facilityInfo.getOpenMessageQueue().poll();
                if (rebound != null) {
                    if (facilityInfo.getOpenMessageQueue().size() > 0) {
                        ByteBuf byteBuf = Unpooled.copiedBuffer(rebound, StandardCharsets.US_ASCII);
                        channel.writeAndFlush(byteBuf);
                        log.info(">>>> {}({}) {}대 대기중", facilityInfo.getFname(), id, facilityInfo.getOpenMessageQueue().size());
                    }
                }
            }
        }

        // 액션이 완료 되었을때 gpms 로 상태정보 전송.
        if (msg.contains("OK")) {
            statusBreaker(msg,facilityInfo);
        }

        facilityInfo.setBarStatus(msg);
    }

    private void statusBreaker(String msg, FacilityInfo facilityInfo) {

        // 프로그램에서 접근하는것
        if(breakerActionTarget == BreakerActionTarget.NORMAL) {
            sendBreakerStatus(facilityInfo, msg);
        }

        // 수동 스위치에서 UPLOCK
        if(msg.contains("EXUPLOCK")) {
            breakerActionTarget = BreakerActionTarget.EX;
            sendBreakerStatus(facilityInfo, msg);
        }

        // 스위치 UPLOCK
        if(msg.contains("SWUPLOCKOKGATE")) {
            breakerActionTarget = BreakerActionTarget.SW;
            sendBreakerStatus(facilityInfo, msg);
        }

        // UNLOCK 요청일때
        if (msg.contains("UNLOCK")) {
            breakerActionTarget = BreakerActionTarget.NORMAL;
            sendBreakerStatus(facilityInfo, msg);
        }


    }

    // 출차 차단기 작업.
    private void exitBreakerTask(FacilityInfo facilityInfo, String msg) {
        new Thread(() -> {
            // 정상적으로 게이트가 올라갔을 경우 시설물 고장이 아님
            if (msg.contains("GATE UP OK") || !(facilityInfo.getBarStatus().contains("GATE DOWN"))) {
                displayService.exitResetMessage(facilityInfo);
                return;
            }

            //  출차 디텍트를 밟았을때 (DET OUT GATE DOWN ACTION)
            if (msg.contains("DET OUT")) {
                displayService.exitResetMessage(facilityInfo);
                String barStatus = facilityInfo.getBarStatus();
                // 게이트 게이트 상태가 내려가져있는 상황일때.
                // 4대 이상 차량이 출차될 경우 알람 발생
                if (barStatus.equals("GATE DOWN OK")) {
                    int passCount = facilityInfo.getPassCount() + 1;
                    if (passCount > 3) {
                        List<FacilityStatus> alarmList = Arrays.asList(
                                FacilityStatus.gateBarDamageDoubt(facilityInfo.getDtFacilitiesId())
                        );
                        gpmsAPI.sendFacilityAlarm(FacilityPayloadWrapper.facilityAlarmPayload(alarmList));
                        passCount = 0;
                    }

                    facilityInfo.setPassCount(passCount);
                }
            }

        }).start();
    }

    public void sendBreakerStatus(FacilityInfo info, String msg) {
        String sendMsg = "";
        if (msg.contains("GATE UPLOCK OK")) {
            sendMsg = "UPLOCK";
        } else if (msg.contains("GATE UNLOCK OK")) {
            sendMsg = "UNLOCK";
        } else if (msg.contains("GATE UP OK")) {
            sendMsg = "UP";
        } else if (msg.contains("GATE DOWN OK")) {
            sendMsg = "DOWN";
        } else if (msg.contains("SCAN OK")) {
            sendMsg = "SCAN";
        } else if (msg.contains("UPLOCKOK")){
            sendMsg = "XUPLOCK";
        } else if (msg.contains("UNLOCKOK")){
            sendMsg = "UNLOCK";
        }

        if (!sendMsg.isEmpty()) {
            List<FacilityStatus> status = Arrays.asList(
                    FacilityStatus.breakerAction(info.getDtFacilitiesId(), sendMsg)
            );
            gpmsAPI.sendStatusNoti(FacilityPayloadWrapper.healthCheckPayload(status));
        }
    }

    // 차단기 상태정보
    public void breakerActive(ChannelHandlerContext ctx){
        char stx = 0x02, etx = 0x03;
        String msg = String.format("%s%s%s", stx, "STATUS", etx);
        ByteBuf byteBuf = Unpooled.copiedBuffer(msg, Charset.forName("ASCII"));
        ctx.writeAndFlush(byteBuf);
    }



}
