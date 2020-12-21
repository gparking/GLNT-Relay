package kr.co.glnt.dto;

import lombok.Data;

/**
 * GPMS 서버와의 통신에서 사용하는 기본 응답 클래스
 */
@Data
public class ResponseDTO {
    private int code;       // 응답 코드
    private String msg;     // 응답 메세지
    private Object data;    // 응답 데이터

}
