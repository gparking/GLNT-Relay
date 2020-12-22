package kr.co.glnt.relay.run;

import kr.co.glnt.relay.tcp.client.BreakerClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Slf4j
@Component
public class TCPRunner implements ApplicationRunner {

    private final BreakerClient client;

    public TCPRunner(BreakerClient client) {
        this.client = client;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        List<String> list = Arrays.asList("127.0.0.1", "localhost", "192.168.0.13");
        log.info("실행?!?!?!?!!??!?");
        client.setFeatureCount(list.size());
        for (int i = 0; i < 3; i++) {
            client.connect(list.get(i), 8888+i);
            client.sendMessage(list.get(i), "test");
        }

        

    }
}
