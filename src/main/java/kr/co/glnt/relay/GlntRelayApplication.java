package kr.co.glnt.relay;

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

@EnableRetry
@EnableAsync
@EnableWebMvc
@EnableScheduling
@SpringBootApplication
public class GlntRelayApplication {

    private static ConfigurableApplicationContext context;

    public static void main(String[] args) {
        SpringApplication application = new SpringApplicationBuilder()
                .sources(GlntRelayApplication.class)
                .listeners(new ApplicationPidFileWriter("/glnt-app.pid"))
                .build();
        context = application.run(args);

        System.out.println("context : " + context);
    }

    public static void restart() {
        GlntNettyClient.getChannelMap().forEach((key, value) -> value.disconnect());
        ApplicationArguments args = context.getBean(ApplicationArguments.class);
        Thread thread = new Thread(() -> {
            context.close();
            SpringApplication application = new SpringApplicationBuilder()
                                                .sources(GlntRelayApplication.class)
                                                .listeners(new ApplicationPidFileWriter("/glnt-app.pid"))
                                                .build();
            context = application.run(args.getSourceArgs());
        });
        thread.setDaemon(false);
        thread.start();
    }
}
