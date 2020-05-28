package io.freedriver.autonomy.util;

import org.junit.jupiter.api.Test;

public class BenchmarkTest {

    @Test
    public void testBenchmark() {
        boolean result = Benchmark.bench(() -> true, "True");
    }
}
