package io.freedriver.autonomy.util;

import kaze.math.power.Power;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Stream;

public class TimeFrame {
    private Instant start;
    private Instant finish;

    public TimeFrame(Instant start, Instant finish) {
        this.start = start;
        this.finish = finish;
    }

    public static <T, F> List<MarkedTimeFrame<F>> blocks(Stream<T> stream, Function<T, Instant> whenFunction, Function<T, F> fieldFunction) {
        List<MarkedTimeFrame<F>> timeFrames = new ArrayList<>();
        timeFrames.add(new MarkedTimeFrame<F>(null, null, null));
        stream
                .sorted(Comparator.comparing(whenFunction))
                .forEach(t -> {
                    Instant when = whenFunction.apply(t);
                    F field = fieldFunction.apply(t);

                    if (timeFrames.size() == 1) {
                        timeFrames.get(0).setStart(when);
                        timeFrames.get(0).setFieldValue(field);
                    } else {
                        MarkedTimeFrame<F> current = timeFrames.get(timeFrames.size()-1);
                        if (!Objects.equals(field, current.getFieldValue())) {
                            current.setFinish(when);
                            timeFrames.add(new MarkedTimeFrame<F>(when, null, field));
                        }
                    }

                });
        MarkedTimeFrame<F> last = timeFrames.get(timeFrames.size()-1);
        if (last.getFinish() == null) {
            last.setFinish(last.getStart());
        }
        return timeFrames;
    }

    public boolean applyFinish(Instant when) {
        if (finish == null) {
            finish = when;
            return true;
        }
        return false;
    }

    public Instant getStart() {
        return start;
    }

    public void setStart(Instant start) {
        this.start = start;
    }

    public Instant getFinish() {
        return finish;
    }

    public void setFinish(Instant finish) {
        this.finish = finish;
    }
    
    public Duration size() {
        return Duration.between(start, finish);
    } 
    
    public boolean greaterThan(Duration compare) {
        return size().compareTo(compare) > 0;
    }

    public boolean lessThan(Duration compare) {
        return size().compareTo(compare) < 0;
    }

    public boolean contains(Instant instant) {
        return (getStart().isAfter(instant) && getFinish().isBefore(instant));
    }
    
}
