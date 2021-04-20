package kr.co.glnt.relay.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Data
@Slf4j
public class DisplayMessage {
    private String dtFacilityId;
    private List<DisplayMessageInfo> messages;

    @Data @NoArgsConstructor
    public static class DisplayMessageInfo {
        private String color;
        private String text;
        private int order;
        private int line;
    }
}
