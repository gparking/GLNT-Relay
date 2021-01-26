package kr.co.glnt.relay.dto;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;

@Getter @RequiredArgsConstructor
public class FacilityStatusWrapper {
    private final List<Object> facilitiesList;
}
