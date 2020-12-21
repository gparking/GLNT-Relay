package kr.co.glnt;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.annotation.EnableAsync;

@EnableRetry
@EnableAsync
@SpringBootApplication
public class BreakerApplication {
    public static void main(String[] args) {
        SpringApplication.run(BreakerApplication.class, args);
    }



}
