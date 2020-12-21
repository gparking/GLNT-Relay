package kr.co.glnt.dto;

import lombok.Data;

import java.util.*;

/**
 * 차량 입차시 Event 가 n번 발생하여 해당 Event 들을 묶기 위해 만든 클래스.
 * 설정된 시간 이내에 발생한 이벤트는 동일 차량으로 취급.
 */
@Data
public class CarGroup {
    private String key;                 // 그룹의 고유 키
    private List<CarInfo> eventList;        // 입차 차량 리스트. 묶는 단위는 1초 안에 들어오는 차량 n대
    private boolean state = false;      // ocr 인식 체크
    private boolean apiCall = false;    // API 호출 체크
    private Timer timer;                // 차량 입차 시 시작되는 타이머.
                                        // 전방 카메라가 전부 미인식으로 떨어졌을때
                                        // GPMS 서버에 게이트 오픈 요청을 하기 위해 필요

    public CarGroup() {
    }

    /**
     * 새로운 차량이 입차되었을 경우 새로운 이벤트 리스트를 생성.
     * 해당 차량 OCR 인식 여부 체크.
     * @param carInfo
     */
    public CarGroup(CarInfo carInfo) {
        this.key = generateUUID();
        this.eventList = new ArrayList<>();
        this.eventList.add(carInfo);
        this.state = ocrValidate(carInfo);
    }

    /**
     * 설정된 시간 이내에 발생한 이벤트일 경우 이벤트 리스트에 추가.
     * @param carInfo
     */
    public void addElement(CarInfo carInfo) {
        eventList.add(carInfo);
        this.state = ocrValidate(carInfo);
    }

    /**
     * 이벤트 그룹의 고유 키값 설정.
     */
    private String generateUUID() {
        StringBuilder builder = new StringBuilder();
        return builder.append(UUID.randomUUID())
                .append(System.currentTimeMillis())
                .toString();
    }

    /**
     * 차량 정보 OCR판독 여부 확인.
     *
     * @param carInfo 차량정보
     */
    public boolean ocrValidate(CarInfo carInfo) {
        return carInfo.ocrValidate();
    }


    /**
     * 타이머 시작.
     * @param task
     */
    public void startTimer(TimerTask task) {
        timer = new Timer();
        timer.schedule(task, 1000);
    }

    /**
     * 타이터 종료.
     */
    public void stopTimer() {
        timer.cancel();
    }
}
