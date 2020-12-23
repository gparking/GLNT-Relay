package kr.co.glnt.relay;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

@EnableRetry
@EnableAsync
@EnableWebMvc
@SpringBootApplication
public class GlntRelayApplication {
    public static void main(String[] args) {
        SpringApplication.run(GlntRelayApplication.class, args);
    }
}
