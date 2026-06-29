package io.freedriver.autonomy;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

import org.junit.jupiter.api.Test;

public class UUIDTest {
    @Test
    public void testUUIDLength() {
        assertEquals(36, UUID.randomUUID().toString().length());
    }

    @Test
    public void randomUUID() {
        System.out.println(UUID.randomUUID().toString());
    }

    @Test
    public void testStartOfDay() {
        LocalDateTime localDateTime = LocalDateTime.now().toLocalDate().atStartOfDay();
        System.out.println(localDateTime.toInstant(ZoneOffset.UTC).toEpochMilli());
    }
}
