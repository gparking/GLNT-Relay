package kr.co.glnt.relay.config;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import kr.co.glnt.relay.dto.CarInfo;
import kr.co.glnt.relay.dto.ResponseDTO;
import org.modelmapper.ModelMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.BufferingClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.DefaultUriBuilderFactory;

import java.net.URI;
import java.util.Objects;

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
        return generateRestTemplate(serverConfig.getNgisUrl());
    }

    public RestTemplate generateRestTemplate(String url) {
        SimpleClientHttpRequestFactory clientHttpRequestFactory = new SimpleClientHttpRequestFactory();
        clientHttpRequestFactory.setReadTimeout(5000);
        clientHttpRequestFactory.setConnectTimeout(5000);

        RestTemplate restTemplate = new RestTemplate(new BufferingClientHttpRequestFactory(clientHttpRequestFactory)) {
            @Override
            @Retryable(value = RestClientException.class, maxAttempts = 3, backoff = @Backoff(delay = 100))
            public <T> T postForObject(URI url, Object request, Class<T> responseType) throws RestClientException {
                return super.postForObject(url, request, responseType);
            }
        };

        restTemplate.setUriTemplateHandler(new DefaultUriBuilderFactory(url));
        restTemplate.getInterceptors().add(new RestTemplateLoggingInterceptor());
        return restTemplate;
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

}
