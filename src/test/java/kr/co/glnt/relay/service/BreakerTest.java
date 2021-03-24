package kr.co.glnt.relay.service;

import kr.co.glnt.relay.dto.CarInfo;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;


public class BreakerTest {

    @Test
    public void isEqualsTest() {
        CarInfo info1 = new CarInfo();
        info1.setCode(0);
        info1.setNumber("");

        CarInfo info2 = new CarInfo();
        info2.setCode(4);
        info2.setNumber("12가1234");

        boolean isEquals =false;

        List<CarInfo> carInfos = Arrays.asList(info1, info2);
        CarInfo firstCar = carInfos.get(0);

        for (int i = 1; i < carInfos.size(); i++) {
            // 정상차량이 아니면 같은 차량으로 처리한다.
            if (!carInfos.get(i).ocrValidate()) {
                isEquals =  true;
            }

            if (firstCar.getNumber().equals(carInfos.get(i).getNumber())) {
                isEquals = true;
            }
        }

        if (isEquals) {
            System.out.println("파일 삭제");
        } else {
            System.out.println("api 호출");
        }
    }
}