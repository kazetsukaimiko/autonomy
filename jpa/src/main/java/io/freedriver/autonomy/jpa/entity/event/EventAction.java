package io.freedriver.autonomy.jpa.entity.event;

import java.io.Serializable;
import java.util.Optional;

/**
 * Serializable lambdas.
 * For Jackson, use Java Serialization combined with Base64 encoding to convert the lambda into a string.
 */

@FunctionalInterface
public interface EventAction extends Serializable {
    long serialVersionUID = -1L;
    Optional<Event> fire(Event sourceEvent);
}
