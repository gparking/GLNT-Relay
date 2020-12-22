package kr.co.glnt.relay.breaker.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Data
public class DisplayMessage {
    private String facilityId;
    private List<DisplayMessageInfo> messages;

    @Data
    private static class DisplayMessageInfo {
        private String color;
        private String text;
        private int order;
        private int line;

        public DisplayMessageInfo() {}
    }


    private final List<String> messageFormat = Arrays.asList("", "![000/P0000/Y0004/%s%s!]", "![000/P0001/Y0408/%s%s!]");

    public List<String> generateMessageList() {
        Map<Integer, List<DisplayMessageInfo>> map = messages.stream()
                .collect(Collectors.groupingBy(DisplayMessageInfo::getOrder));

        List<String> messageList = new ArrayList<>();
        for (int i = 0; i < map.size(); i++) {
            List<DisplayMessageInfo> infoList = map.get(i);
            for(int j = 0; j < infoList.size(); j++) {
                DisplayMessageInfo info = infoList.get(j);
                String message = String.format(messageFormat.get(info.getLine()), info.getColor(), info.getText());
                messageList.add(message);
            }
        }
        
        return messageList;
    }
}
