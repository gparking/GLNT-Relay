package kr.co.glnt.relay.dto;


import lombok.Data;

/**
 * 자동차 정보 클래스
 */
@Data
public class CarInfo {
    private int code;               // OCR 결과 코드
    private String number;          // OCR 로 인식된 번호.
    private String fullPath;        // 사진 전체경로
    private long inDate;            // 입차 시간.
    private String dtFacilitiesId;    // 들어온 게이트 아이디.
    private boolean request;

    /**
     * OCR 결과 코드를 비교해 성공인지 실패인지 확인
     * code == 0: 미인식
     *
     * 성공 코드 : 1, 2, 3, 4, 5
     * code == 1: 서울12가1234
     * code == 2: 서울3가1234
     * code == 3: 12가1234
     * code == 4: 가로 1줄짜리 번호판.
     * code == 5: 주황색 특정차 번호판.
     * code >= 100: 부분인식
     */
    public boolean ocrValidate() {
        return (code > 0 && code < 10);
    }


}
