package kr.co.glnt.relay.tcp;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import kr.co.glnt.relay.config.ServerConfig;
import kr.co.glnt.relay.dto.FacilityInfo;
import kr.co.glnt.relay.run.AppRunner;
import kr.co.glnt.relay.service.PaymentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class GlntNettyHandlerTest {

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    AppRunner appRunner;

    @Autowired
    ServerConfig config;

    @Autowired
    PaymentService paymentService;

    @Test
    void nettyTest() throws JsonProcessingException {


        System.out.println("-----------초기화 시작-------------");

        appRunner.init();

        System.out.println("-----------data setting-----------");

        HashMap<String,Object> data = new HashMap<>();
        HashMap<String,String> content = new HashMap<>();

        content.put("parkTicketNumber","92021050417515");
        content.put("paymentMachineType","exit");
        content.put("vehicleNumber","23호4423");





        data.put("contents",content);
        data.put("eventDateTime","20210727090820");
        data.put("parkingSiteId","TPD0000217");
        data.put("requestId","652");
        data.put("type","adjustmentdataRequest");


        String message = objectMapper.writeValueAsString(data);

        FacilityInfo byFacilitiesId = config.findByFacilitiesId("PAY004201", "PAY004201");

        //GlntNettyClient.sendMessage(byFacilitiesId.generateHost(),"GATE UP", Charset.forName("ASCII"));

        System.out.println("---------- 데이터 전송 -------------");
        paymentService.paymentProcessing(byFacilitiesId,message);

        System.out.println("---------- 테스트 종료 --------------");




    }

}

