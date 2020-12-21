package kr.co.glnt;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import kr.co.glnt.dto.ParkinglotInfo;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.TimeZone;

public class CommonTest {

    @Test
    public void defaultStringMapping() throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        ParkinglotInfo feature = mapper.convertValue(null, ParkinglotInfo.class);
        System.out.println(feature);
    }


    @Test
    public void convertTest() throws Exception {
        long time = System.currentTimeMillis();
        LocalDateTime currentTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(time), TimeZone.getDefault().toZoneId());
        System.out.println(currentTime);
    }
}
