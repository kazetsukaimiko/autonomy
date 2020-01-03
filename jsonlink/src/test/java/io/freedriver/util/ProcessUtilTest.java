package io.freedriver.util;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ProcessUtilTest {
    private static final Random RANDOM = new Random(System.currentTimeMillis());

    @Test
    public void testStreamLineReader() {



        List<String> parts = IntStream.range(0, RANDOM.nextInt(1000))
                .mapToObj(i -> UUID.randomUUID())
                .map(UUID::toString)
                .collect(Collectors.toList());
        ByteArrayInputStream bais = new ByteArrayInputStream(String.join("\n", parts).getBytes());

        List<String> received = ProcessUtil.linesInputStream(bais)
                .collect(Collectors.toList());

        assertEquals(parts.size(), received.size(), "Should produce the same number of lines");

        IntStream.range(0, parts.size()-1)
                .forEach(idx -> assertEquals(parts.get(idx), received.get(idx), "Each line should match"));
    }
}
