package kr.co.glnt.watcher;


import kr.co.glnt.api.GpmsAPI;
import kr.co.glnt.api.NgisAPI;
import kr.co.glnt.config.ApplicationContextProvider;
import kr.co.glnt.dto.ParkinglotInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;

@Slf4j
public abstract class Breaker {
    // 주차장 정보. (차단기, 정산기 등..)
    protected ParkinglotInfo parkinglotInfo;
    protected NgisAPI ngisAPI;
    protected GpmsAPI gpmsAPI;

    public Breaker(ParkinglotInfo parkinglotInfo) {
        this.parkinglotInfo = parkinglotInfo;

        ApplicationContext ctx = ApplicationContextProvider.getApplicationContext();
        ngisAPI = ctx.getBean("ngisAPI", NgisAPI.class);
        gpmsAPI = ctx.getBean("gpmsAPI", GpmsAPI.class);
    }

    // gate flow start
    public abstract void startProcessing(String fullPath);



}
