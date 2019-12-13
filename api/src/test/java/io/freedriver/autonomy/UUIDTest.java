package io.freedriver.autonomy;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class UUIDTest {
    @Test
    public void testUUIDLength() {
        assertEquals(36, UUID.randomUUID().toString().length());
    }
}
