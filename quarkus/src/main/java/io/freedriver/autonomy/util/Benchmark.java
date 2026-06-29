package io.freedriver.autonomy.util;

import java.util.concurrent.Callable;
import java.util.function.BiConsumer;
import java.util.regex.Pattern;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Benchmark {

    public static final String TIME_PLACEHOLDER = "{execution_time}";
    public static final Benchmark INFO = new Benchmark((m, o) -> log.info(m, o));
    public static final Benchmark DEBUG = new Benchmark((m, o) -> log.trace(m, o));

    private final BiConsumer<String, Object[]> logConsumer;

    public Benchmark(BiConsumer<String, Object[]> logConsumer) {
        this.logConsumer = logConsumer;
    }

    public void log(String message, long time, Object... args) {
        if (message == null) {
            message = "Took " + TIME_PLACEHOLDER;
        }
        if (!message.contains(TIME_PLACEHOLDER)) {
            message = message + " took "+ TIME_PLACEHOLDER;
        }
        logConsumer.accept(message.replaceAll(Pattern.quote(TIME_PLACEHOLDER), time+"ms"), args);
    }

    private <T> T benchmark(Callable<T> callable, String message, Object... args) {
        long start = System.currentTimeMillis();
        T t;
        try {
            t = callable.call();
        } catch (RuntimeException rte) {
            throw rte;
        } catch (Exception e) {
            throw new BenchmarkException("Exception Benchmarking", e);
        }
        log(message, System.currentTimeMillis() - start, args);
        return t;
    }

    public static Benchmark getDefault() {
        return DEBUG;
    }

    public static <T> T bench(Callable<T> callable, String message, Object... args) {
        return getDefault()
                .benchmark(callable, message, args);
    }
}