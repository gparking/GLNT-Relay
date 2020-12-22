package kr.co.glnt.relay.breaker.service;


import kr.co.glnt.relay.breaker.web.GpmsAPI;
import kr.co.glnt.relay.breaker.web.NgisAPI;
import kr.co.glnt.relay.common.config.ApplicationContextProvider;
import kr.co.glnt.relay.breaker.dto.EventInfo;
import kr.co.glnt.relay.breaker.dto.FacilityInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;

@Slf4j
public abstract class Breaker {
    protected long lastRegTime = System.currentTimeMillis();
    protected int MAX_TIME = 5000;    // 발생하는 이벤트들을 하나로 취급할 시간. (현재는 1초)
    protected int TIMER_TIME = 1000;
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

    public abstract void startProcessing(EventInfo eventInfo);


    protected boolean isNewCarEnters(long currentTime) {
        return (currentTime - lastRegTime) > MAX_TIME;
    }
}
