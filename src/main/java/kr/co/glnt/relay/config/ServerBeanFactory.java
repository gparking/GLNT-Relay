package kr.co.glnt.relay.config;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import kr.co.glnt.relay.dto.CarInfo;
import kr.co.glnt.relay.dto.ResponseDTO;
import org.modelmapper.ModelMapper;
import org.springframework.boot.actuate.autoconfigure.metrics.MetricsProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.http.*;
import org.springframework.http.client.BufferingClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.DefaultUriBuilderFactory;

import javax.xml.ws.Response;
import java.net.URI;
import java.nio.charset.Charset;
import java.util.Objects;

@DependsOn("serverConfig")
@Configuration
public class ServerBeanFactory {

    private final ServerConfig serverConfig;
    private final ModelMapper modelMapper = new ModelMapper();

    public ServerBeanFactory(ServerConfig serverConfig) {
        this.serverConfig = serverConfig;
    }

    @Bean("gpmsRestTemplate")
    public RestTemplate gpmsTemplate() {
        return generateRestTemplate(serverConfig.getGpmsUrl());
    }

    @Bean("ngisRestTemplate")
    public RestTemplate ngisRestTemplate() {
        return generateNgisRestTemplate(serverConfig.getNgisUrl());
    }

    @Bean("webClient")
    public WebClient webClient() {
        return generateWebClient();
    }

    @Bean
    public ObjectMapper objectMapper() {
        return Jackson2ObjectMapperBuilder.json()
                .featuresToDisable(SerializationFeature.FAIL_ON_EMPTY_BEANS)
                .featuresToDisable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .featuresToDisable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .modules(new JavaTimeModule())
                .build();
    }

    @Bean
    public ModelMapper modelMapper() {
        modelMapper.createTypeMap(ResponseDTO.class, CarInfo.class)
                .addMapping(ResponseDTO::getCode, CarInfo::setCode)
                .addMapping(ResponseDTO::getData, (destination, value) -> destination.setNumber(Objects.toString(value, "")));
        return modelMapper;
    }


    public RestTemplate generateRestTemplate(String url) {
        SimpleClientHttpRequestFactory clientHttpRequestFactory = new SimpleClientHttpRequestFactory();
        clientHttpRequestFactory.setReadTimeout(5000);
        clientHttpRequestFactory.setConnectTimeout(5000);

        RestTemplate restTemplate = new RestTemplate();
        restTemplate.setUriTemplateHandler(new DefaultUriBuilderFactory(url));
        restTemplate.getInterceptors().add(new RestTemplateLoggingInterceptor());
        restTemplate.getMessageConverters().add(0, new StringHttpMessageConverter(Charset.forName("UTF-8")));
        return restTemplate;
    }

    public RestTemplate generateNgisRestTemplate(String url) {
        SimpleClientHttpRequestFactory clientHttpRequestFactory = new SimpleClientHttpRequestFactory();
        clientHttpRequestFactory.setReadTimeout(5000);
        clientHttpRequestFactory.setConnectTimeout(5000);

        RestTemplate restTemplate = new RestTemplate();
        restTemplate.setUriTemplateHandler(new DefaultUriBuilderFactory(url));
        restTemplate.getInterceptors().add(new NgisLoggingInterceptor());
        restTemplate.getMessageConverters().add(0, new StringHttpMessageConverter(Charset.forName("UTF-8")));
        return restTemplate;
    }

    public WebClient generateWebClient() {
        return WebClient.builder()
                .baseUrl(serverConfig.getGpmsUrl())
                .defaultHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader("Accept", MediaType.APPLICATION_JSON_VALUE)
                .build();
    }
}
