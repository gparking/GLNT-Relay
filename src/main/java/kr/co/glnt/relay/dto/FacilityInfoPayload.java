package kr.co.glnt.relay.dto;

import lombok.Data;
import lombok.RequiredArgsConstructor;

/**
 * 게이트 정보 리스트 조회에 쓰이는 클래스
 */
@Data
@RequiredArgsConstructor
public class FacilityInfoPayload {
    private final String gateSvrKey;
    private String featureId;
}
