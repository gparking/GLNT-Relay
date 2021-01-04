package kr.co.glnt.relay.dto;

import lombok.Data;

/**
 * 파일 생성 이벤트 정보
 */
@Data
public class EventInfo {
    private long createdTime = System.currentTimeMillis(); // 이벤트 발생 시간.
    private final String fullPath;                         // 파일 전체경로.
}
