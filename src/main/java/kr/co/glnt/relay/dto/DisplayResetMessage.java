package kr.co.glnt.relay.dto;

import lombok.Data;

import java.util.List;

@Data
public class DisplayResetMessage {
    private List<DisplayMessage.DisplayMessageInfo> in;
    private List<DisplayMessage.DisplayMessageInfo> out;
}
