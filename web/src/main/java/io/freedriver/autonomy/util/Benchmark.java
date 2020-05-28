package io.freedriver.autonomy.util;

import java.util.concurrent.Callable;
import java.util.function.BiConsumer;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Benchmark {
    private static final Logger LOGGER = Logger.getLogger(Benchmark.class.getName());

    public static final String TIME_PLACEHOLDER = "{execution_time}";
    public static final Benchmark INFO = new Benchmark(Level.INFO);
    public static final Benchmark DEBUG = new Benchmark(Level.INFO);

    private final BiConsumer<String, Object[]> logConsumer;

    public Benchmark(BiConsumer<String, Object[]> logConsumer) {
        this.logConsumer = logConsumer;
    }

    public Benchmark(Level level) {
        this((m, o) -> LOGGER.log(level, m, o));
    }

    public void log(String message, long time, Object... args) {
        if (message == null) {
            message = "Took " + TIME_PLACEHOLDER;
        }
        if (!message.contains(TIME_PLACEHOLDER)) {
            message = message + " took "+ TIME_PLACEHOLDER;
        }
        logConsumer.accept(message.replaceAll(TIME_PLACEHOLDER, time+"ms"), args);
    }

    private <T> T benchmark(Callable<T> callable, String message, Object... args) {
        long start = System.currentTimeMillis();
        T t;
        try {
            t = callable.call();
        } catch (Exception e) {
            throw new BenchmarkException("Exception Benchmarking", e);
        }
        log(message, System.currentTimeMillis() - start, args);
        return t;
    }

    public static Benchmark getDefault() {
        return INFO;
    }

    public static <T> T bench(Callable<T> callable, String message, Object... args) {
        return getDefault()
                .benchmark(callable, message, args);
    }
}
