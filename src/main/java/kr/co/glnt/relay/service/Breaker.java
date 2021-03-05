package kr.co.glnt.relay.service;


import kr.co.glnt.relay.config.ApplicationContextProvider;
import kr.co.glnt.relay.dto.CarInfo;
import kr.co.glnt.relay.dto.EventInfo;
import kr.co.glnt.relay.dto.EventInfoGroup;
import kr.co.glnt.relay.dto.FacilityInfo;
import kr.co.glnt.relay.web.GpmsAPI;
import kr.co.glnt.relay.web.NgisAPI;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

// 차단기 부모.
@Slf4j
public abstract class Breaker {
    protected long lastRegTime = System.currentTimeMillis();
    protected int maxTime = 1000;    // 발생하는 이벤트들을 하나로 취급할 시간
    protected int timerTime = 1000;
    // 주차장 정보. (차단기, 정산기 등..)
    protected FacilityInfo facilityInfo;
    protected NgisAPI ngisAPI;
    protected GpmsAPI gpmsAPI;

    public Breaker(FacilityInfo facilityInfo) {
        this.facilityInfo = facilityInfo;

        ApplicationContext ctx = ApplicationContextProvider.getApplicationContext();
        ngisAPI = ctx.getBean("ngisAPI", NgisAPI.class);
        gpmsAPI = ctx.getBean("gpmsAPI", GpmsAPI.class);
    }


    /**
     * 차단기 시작
     */
    public abstract void startProcessing(EventInfo eventInfo);


    /**
     * 지정한 시간내에 들어온 차량인지 확인 (같은 차량)
     */
    protected boolean isNewCarEnters(long currentTime) {
        return (currentTime - lastRegTime) > maxTime;
    }


    /**
     * 이벤트 그룹의 차량 정보들을 OCR 처리하여 차량정보 리스트를 반환
     */
    protected List<CarInfo> getCurrentGroupEventList(EventInfoGroup eventGroup) {
        return eventGroup.getEventList().stream()
                .map(eventInfo -> generatedCarInfo(eventInfo))
                .collect(Collectors.toList());
    }

    /**
     * EventInfo 에 있는 이미지를 사용해
     * 차량 번호를 추출 후 차량 정보를 생성하여 반환
     */
    protected CarInfo generatedCarInfo(EventInfo eventInfo) {
        CarInfo carInfo = ngisAPI.requestOCR(eventInfo.getFullPath());
        carInfo.setInDate(eventInfo.getCreatedTime());
        carInfo.setFullPath(eventInfo.getFullPath());
        carInfo.setDtFacilitiesId(facilityInfo.getDtFacilitiesId());
        return carInfo;
    }

    /**
     * 입차 요청할 차량정보 추출
     */
    protected CarInfo getCarInfoToBeTransmit(List<CarInfo> carInfos) {
        long ocrCount = carInfos.stream().filter(CarInfo::ocrValidate).count();
        if (ocrCount > 0) {
            return recognizedVehicle(carInfos);
        } else {
            return partiallyRecognizedVehicle(carInfos);
        }
    }


    /**
     * 1초 이내에 들어온 입차자량 리스트를 비교해
     * 미인식(오인식 포함) 제외한 차량만 조회하여
     * 차량번호가 같은지 비교한다.
     */
    protected boolean isEqualsCarNumber(List<CarInfo> carInfos) {
        for (int i = 0; i < carInfos.size(); i++) {

            // 정상 처리된 차량 번호일 경우
            if (carInfos.get(i).ocrValidate()) {
                for (int j = i + 1; j < carInfos.size(); j++) {

                    // Main LPR 과 Sub LPR 의 차량 번호를 같은지 비교
                    if (carInfos.get(i).getNumber().equals(carInfos.get(j).getNumber())) {
                        return true;
                    }
                }
            }
        }

        return false;
    }


    /**
     * 정상인식 된 차량들 중에 더 많이 찍힌 차량번호를 리턴.
     */
    private CarInfo recognizedVehicle(List<CarInfo> carInfos) {
        String carNumber = null;
        // OCR 이 인식된 값들을 차량번호를 키로 하여 map으로 변환.
        Map<String, List<CarInfo>> carMap = carInfos.stream()
                .filter(CarInfo::ocrValidate)   // 정상 인식된 차량
                .collect(Collectors.groupingBy(CarInfo::getNumber));

        // 인식된 번호중 더 많이 찍힌 차량번호를 추출.
        Optional<Map.Entry<String, List<CarInfo>>> maxCarInfo = carMap.entrySet().stream()
                .max((entry1, entry2) -> entry1.getValue().size() > entry2.getValue().size() ? 1 : -1);
        if (maxCarInfo.isPresent()) {
            carNumber = maxCarInfo.get().getKey();
        }

        // 차량 정보 가져오기
        return carMap.get(carNumber).get(0);
    }

    /**
     * 미인식된 차량들 중에 부분인식 정보가 있으면 해당 정보를 리턴하고
     * 없을 경우 첫번째 미인식 정보를 리턴한다.
     */
    private CarInfo partiallyRecognizedVehicle(List<CarInfo> carInfos) {
        for (CarInfo info : carInfos) {
            if (info.getCode() > 10) { // 부분인식이 있으면
                return info;
            }
        }
        return carInfos.get(0);
    }

}
