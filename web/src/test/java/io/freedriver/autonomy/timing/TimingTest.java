package io.freedriver.autonomy.timing;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.temporal.ChronoUnit;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class TimingTest {

    @Test
    public void testInvalidTimings() {
        long start = System.currentTimeMillis();
        Timing.wait(Duration.of(1, ChronoUnit.SECONDS)).exec();
        long finish = System.currentTimeMillis();

        assertTrue(finish-start >= 1000);
        assertTrue(finish-start <= 1050);
    }

}
