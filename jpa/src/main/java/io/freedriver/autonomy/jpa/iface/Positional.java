package io.freedriver.autonomy.jpa.iface;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.LongStream;

public interface Positional {
    Comparator<Positional> EXPLICIT_ORDER = Comparator.comparingLong(Positional::getPosition);
    long getPosition();
    void setPosition(long newPosition);

    static <T extends Positional> List<T> move(List<T> toOrder, int fromIndex, int toIndex) {
        if (toOrder == null || toOrder.isEmpty()) {
            return toOrder;
        }
        ArrayList<T> inOrder = new ArrayList<>(toOrder);
        int from = Math.min(toOrder.size()-1, Math.max(0, fromIndex));
        int to = Math.min(inOrder.size()-1, Math.max(0, toIndex));
        if (from == to) {
            return reorder(inOrder);
        }
        inOrder.add(to, inOrder.remove(from));

        return reorder(inOrder);
    }

    static <T extends Positional> List<T> reorder(List<T> inOrder) {
        if (inOrder == null || inOrder.isEmpty()) {
            return inOrder;
        }
        LongStream.range(0, inOrder.size())
                .forEach(idx -> inOrder.get((int) idx).setPosition(idx));
        return inOrder;
    }

    static <T extends Positional> boolean correctOrder(List<T> inOrder) {
        return LongStream.range(0, inOrder.size())
                .allMatch(idx -> Objects.equals(idx, inOrder.get((int) idx).getPosition()));
    }

    static <T extends Positional> T next(List<T> inOrder, T current) {
        return inOrder.stream()
                .filter(item -> Objects.equals(current.getPosition()+1, item.getPosition()))
                .findFirst()
                .orElse(inOrder.isEmpty() ? null : inOrder.get(0));
    }

}
