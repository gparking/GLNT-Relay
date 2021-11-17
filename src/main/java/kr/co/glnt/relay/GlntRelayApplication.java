package kr.co.glnt.relay;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.ApplicationPidFileWriter;
import org.springframework.core.env.Environment;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import javax.annotation.PostConstruct;
import java.net.InetAddress;
import java.util.Arrays;
import java.util.Set;

@Slf4j
@EnableRetry
@EnableAsync
@EnableWebMvc
@EnableScheduling
@SpringBootApplication
public class GlntRelayApplication {
    private final Environment env;

    public GlntRelayApplication(Environment env) {
        this.env = env;
    }

    @PostConstruct
    public void init() {
        Set<String> profiles = Arrays.stream(env.getActiveProfiles()).collect(java.util.stream.Collectors.toSet());
        try {
            log.info("\n----------------------------------------------------------\n\t" +
                            "SpringbootApplication '{}' {} {} is running! Access URLs:\n\t" +
                            "URL: \thttp://{}:{}\n\t" +
                            "----------------------------------------------------------",
                    env.getProperty("spring.application.name"),
                    profiles,
                    env.getProperty("spring.application.version"),
                    InetAddress.getLocalHost().getHostAddress(),
                    env.getProperty("server.port"));
        } catch (java.net.UnknownHostException e) {
            e.printStackTrace();
        }
    }

//    private static ConfigurableApplicationContext context;

    public static void main(String[] args) {
        SpringApplication application = new SpringApplicationBuilder()
                .sources(GlntRelayApplication.class)
                .listeners(new ApplicationPidFileWriter("/tmp/relay.pid"))
                .build();
        application.run(args);
    }
}
