package kr.co.glnt.relay.dto;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class EventInfoGroupTest {

    @Test
    public void createUUID() {
        UUID uuid = UUID.randomUUID();
        StringBuilder builder = new StringBuilder(uuid.toString());
        builder.append(LocalDateTime.now());
        System.out.println("uuid: " + builder.toString());
    }
}