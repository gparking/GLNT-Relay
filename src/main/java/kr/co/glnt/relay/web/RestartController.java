package kr.co.glnt.relay.web;

import kr.co.glnt.relay.GlntRelayApplication;
import kr.co.glnt.relay.tcp.GlntNettyClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class RestartController {

    @PostMapping("/restart")
    public void restart() {
        GlntNettyClient.setRESTART(true);
        GlntRelayApplication.restart();
    }
}
