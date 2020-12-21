package kr.co.glnt.dto.payload;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;

/**
 * 게이트 정보 리스트 조회에 쓰이는 클래스
 */
@Data
@RequiredArgsConstructor
public class ParkingInfoPayload {
    private final String gateSvrKey;
    private String featureId;
}
