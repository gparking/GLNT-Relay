package kr.co.glnt.relay.dto;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

public class EventInfoGroupTest {

    @Test
    public void createUUID() {
        UUID uuid = UUID.randomUUID();
        StringBuilder builder = new StringBuilder(uuid.toString());
        builder.append(LocalDateTime.now());
        System.out.println("uuid: " + builder.toString());

        char stx = 0x02;
        char etx = 0x03;
        System.out.println(String.format("%stest%s", stx, etx));
    }


    @Test
    public void timePeriod() {
        LocalDateTime beforeDate = LocalDateTime.of(2021, 1, 26, 9, 30, 0);
        System.out.println(ChronoUnit.MINUTES.between(beforeDate, LocalDateTime.now()));
    }
}