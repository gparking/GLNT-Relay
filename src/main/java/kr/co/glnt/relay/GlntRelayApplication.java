package kr.co.glnt.relay;

import io.netty.channel.Channel;
import kr.co.glnt.relay.tcp.GlntNettyClient;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.ApplicationPidFileWriter;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@EnableRetry
@EnableAsync
@EnableWebMvc
@EnableScheduling
@SpringBootApplication
public class GlntRelayApplication {

//    private static ConfigurableApplicationContext context;

    public static void main(String[] args) {
        SpringApplication application = new SpringApplicationBuilder()
                .sources(GlntRelayApplication.class)
                .listeners(new ApplicationPidFileWriter("/tmp/relay.pid"))
                .build();
        application.run(args);
    }
}
