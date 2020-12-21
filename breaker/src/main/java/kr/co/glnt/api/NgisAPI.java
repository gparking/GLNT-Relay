package kr.co.glnt.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import kr.co.glnt.dto.CarInfo;
import kr.co.glnt.dto.ResponseDTO;
import lombok.SneakyThrows;
import lombok.Synchronized;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Objects;

@Slf4j
@Component("ngisAPI")
public class NgisAPI {
    private final RestTemplate template;
    private final ObjectMapper objectMapper;
    private final ModelMapper modelMapper;
//
    public NgisAPI(@Qualifier(value = "ngisRestTemplate") RestTemplate template,
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
    @Synchronized
    public int requestNgisOpen() {
        ResponseDTO response = template.getForObject("/ngis/open", ResponseDTO.class);
        if(hasError(response)) return -1;
        return response.getCode();
    }

    @SneakyThrows
    @Synchronized
//    public String requestOCR(String imagePath) {
//        ResponseDTO response = template.postForObject("/ngis/ocr", objectMapper.writeValueAsString(imagePath), ResponseDTO.class);
//        if (hasError(response)) {
//            return null;
//        }
//        return Objects.toString(response.getData(), "");
//    }
    public CarInfo requestOCR(String imagePath) {
        ResponseDTO response = template.postForObject("/ngis/ocr", objectMapper.writeValueAsString(imagePath), ResponseDTO.class);
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
