package kr.co.glnt.relay.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import kr.co.glnt.relay.dto.CarInfo;
import kr.co.glnt.relay.dto.ResponseDTO;
import lombok.SneakyThrows;
import lombok.Synchronized;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.File;
import java.util.Objects;

@Slf4j
@Component("ngisAPI")
public class NgisAPI {
    private final WebClient template;
    private final ObjectMapper objectMapper;
    private final ModelMapper modelMapper;
//
    public NgisAPI(@Qualifier(value = "ngisClient") WebClient template,
                   ObjectMapper objectMapper, ModelMapper modelMapper) {
        this.template = template;
        this.objectMapper = objectMapper;
        this.modelMapper = modelMapper;
    }

    /**
     * NgisCar.dll 호출
     * @return 0: 성공
     *        -1: 실패
     */
//    @Synchronized
//    public int requestNgisOpen() {
//        ResponseDTO response = template.getForObject("/open", ResponseDTO.class);
//        log.info(">>>> ngis connect response: {}", response);
//        if(hasError(response)) return -1;
//        return response.getCode();
//    }

    @Synchronized
    public int requestNgisOpen() {
        ResponseDTO response = template.get()
                .uri("/open")
                .retrieve()
                .bodyToMono(ResponseDTO.class)
                .block();
        if (hasError(response)) return -1;
        return response.getCode();
    }

    @SneakyThrows
    @Synchronized
    public CarInfo requestOCR(String imagePath) {
        File file = new File(imagePath);
        long currentSize = file.length();
        log.info(">>>> event {} fileSize: {} bytes", imagePath, currentSize);

        long startTime = System.currentTimeMillis();
        long timeoutNanos = 500;
        long waitTime = timeoutNanos;

        for (;;) {
            if (new File(imagePath).length() == currentSize) {
                break;
            }
            waitTime = timeoutNanos - (System.currentTimeMillis() - startTime);
            if (waitTime <= 0) {
                log.info(">>>> file wait timeout");
                break;
            }
        }

        log.info(">>>> ocr {} fileSize: {} bytes", imagePath, file.length());

        ResponseDTO response = template.post()
                .uri("/ocr")
                .bodyValue(objectMapper.writeValueAsString(imagePath))
                .retrieve()
                .bodyToMono(ResponseDTO.class)
                .block();

        log.info(">>>> {} ngis ocr response: {}", imagePath, response);
        if (hasError(response)) {
            return null;
        }
        return modelMapper.map(response, CarInfo.class);
    }

    private boolean hasError(ResponseDTO response) {
        if (Objects.isNull(response)) return true;
        String errMsg = response.getMsg();
        if (Objects.nonNull(errMsg)) {
            log.error(errMsg);
            return true;
        }
        return false;
    }
}
