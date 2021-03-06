package io.freedriver.autonomy.jpa.iface;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.LongStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class PositionalTest {
    // Log
    static final Consumer<Object> CONSUMER = (x) -> {};

    Random random = new Random(System.currentTimeMillis());
    int max = 5;

    @Test
    public void testNext() {
        List<PatronsInLine> a = LongStream.range(0, max)
                .mapToObj(PatronsInLine::new)
                .collect(Collectors.toList());
        a.forEach(p -> System.out.println(p.getUuid().toString() + " ("+p.getPosition()+")"));

        assertTrue(Positional.correctOrder(a));
        IntStream.range(0, max-1)
                .forEach(idx -> {
                    assertEquals(idx < max ? a.get(idx+1) : a.get(0), Positional.next(a, a.get(idx)));
                });
    }

    @Test
    public void testMove() {
        List<PatronsInLine> a = LongStream.range(0, max)
                .mapToObj(PatronsInLine::new)
                .collect(Collectors.toList());

        IntStream.range(0, 10000)
                .forEach(i -> testMoveSpecific(a, random.nextInt(max), random.nextInt(max)));

        assertNull(Positional.move(null, random.nextInt(max), random.nextInt(max)));
    }

    @Test
    public void testReorder() {
        assertNull(Positional.reorder(null));
    }

    private static <T extends Positional> void testPosition(List<T> a) {
        if (a != null && !a.isEmpty()) {
            LongStream.range(0, a.size()-1)
                    .forEach(idx -> assertEquals(idx, a.get((int)idx).getPosition()));
        }
    }

    private static void testMoveSpecific(List<PatronsInLine> a, int fromIndex, int toIndex) {
        CONSUMER.accept("Index "+Math.min(fromIndex, a.size()-1)+" -> " + toIndex);
        List<PatronsInLine> b = Positional.move(a, fromIndex, toIndex);
        compareLists(a, b);
        testPosition(b);
        assertEquals(a.get(Math.min(fromIndex, a.size()-1)), b.get(Math.min(a.size()-1, toIndex)));
    }

    private static void compareLists(List<PatronsInLine> a, List<PatronsInLine> b) {
        assertEquals(a.size(), b.size());

        IntStream.range(0, a.size())
                .mapToObj(idx -> a.get(idx).getUuid().toString()
                        + " (" + a.get(idx).getPosition() + ")"
                        + " : " + b.get(idx).getUuid().toString()
                        + " (" + b.get(idx).getPosition() + ")")
                .forEach(CONSUMER);
    }

    private final class PatronsInLine implements Positional {
        private final UUID uuid = UUID.randomUUID();
        private long position;

        public PatronsInLine(long position) {
            this.position = position;
        }

        public UUID getUuid() {
            return uuid;
        }

        @Override
        public long getPosition() {
            return position;
        }

        @Override
        public void setPosition(long newPosition) {
            this.position = newPosition;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            PatronsInLine that = (PatronsInLine) o;
            return Objects.equals(uuid, that.uuid);
        }

        @Override
        public int hashCode() {
            return Objects.hash(uuid);
        }
    }
}
