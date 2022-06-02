package io.freedriver.autonomy.deployments.common.event;

import java.time.Duration;
import java.time.Instant;

public class ReceivedItem<T> {
    private final T item;
    private final Instant expiry;

    public ReceivedItem(T item, Instant expiry) {
        this.item = item;
        this.expiry = expiry;
    }

    public ReceivedItem(T item) {
        this(item, Instant.now().plus(Duration.ofMinutes(2)));
    }

    public T getItem() {
        return item;
    }

    public Instant getExpiry() {
        return expiry;
    }

    public boolean hasExactItem(Object o) {
        return item == o;
    }

    public boolean isExpired() {
        return Instant.now().isAfter(expiry);
    }

    public boolean match(ReceivedItem<?> receivedItem) {
        return hasExactItem(receivedItem.getItem());
    }

    public String getItemClassAsString() {
        return item != null
                ? item.getClass().getName()
                : "null";
    }
}
