package kr.co.glnt.relay.common.dto;

import lombok.*;
import org.springframework.http.HttpStatus;

/**
 * GPMS 서버와의 통신에서 사용하는 기본 응답 클래스
 */
@Setter @Getter
@NoArgsConstructor
@ToString
public class ResponseDTO {
    private int code;       // 응답 코드
    private String msg;     // 응답 메세지
    private Object data;    // 응답 데이터

    public ResponseDTO(HttpStatus status, String msg) {
        this.code = status.value();
        this.msg = msg;
    }
}
