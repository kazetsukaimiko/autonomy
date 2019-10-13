package io.freedriver.autonomy.timing;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class Timing {
    private final Consumer<Integer> act;
    private final Integer limit;
    private final Duration interval;

    private Timing(Consumer<Integer> act, Integer limit, Duration interval) {
        this.act = act;
        this.limit = limit;
        this.interval = interval;
    }

    static Timing action(Runnable runnable) {
        return action((i) -> runnable.run());
    }

    static Timing action(Consumer consumer) {
        return new Timing(consumer, 1, null);
    }

    static Timing limit(int limit) {
        return new Timing(null, limit, null);
    }

    static Timing interval(Duration interval) {
        return new Timing(null, null, interval);
    }

    static Timing wait(Duration duration) {
        return new Timing((i) -> {
            try {
                Thread.sleep(duration.toMillis());
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }, 1, null);
    }

    public void exec() {
        IntStream.range(0, getLimit())
                .forEach(this::iteration);
    }

    private void iteration(int i) {
        getAct().accept(i);
        if (i < getLimit()) {
            try {
                Thread.sleep(getInterval().toMillis());
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public Chain then(Timing timing) {
        return new Chain(this, timing);
    }

    public Consumer<Integer> getAct() {
        return act != null ? act : (i) -> {};
    }

    public Integer getLimit() {
        return limit != null && limit > 0 ? limit : 0;
    }

    public Duration getInterval() {
        return interval != null ?  interval : Duration.ZERO;
    }

    private final class Chain extends Timing {
        private List<Timing> timings = new ArrayList<>();

        private Chain(Timing... timing) {
            this(Arrays.asList(timing));
        }

        private Chain(List<Timing> timing) {
            super((i) -> timing
                            .stream()
                            .flatMap(tim -> tim instanceof Chain ? ((Chain) tim).timings.stream() : Stream.of(tim))
                            .forEach(Timing::exec),
                    limit,
                    interval);
            this.timings = timing;
        }

        public Chain then(Timing timing) {
            timings.add(timing);
            return this;
        }
    }
}
