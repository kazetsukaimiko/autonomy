package io.freedriver.autonomy.service.crud;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.stream.Stream;

import io.freedriver.autonomy.jpa.entity.event.Event;
import io.freedriver.autonomy.jpa.entity.event.Event_;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaDelete;
import jakarta.persistence.criteria.Root;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class EventCrudService<E extends Event> extends JPACrudService<E> {

    public static Instant getStartOfDay() {
        LocalDateTime localDateTime = LocalDateTime.now().toLocalDate().atStartOfDay();
        return localDateTime.toInstant(ZoneOffset.UTC);
    }

    public static Instant ago(Duration duration) {
        return LocalDateTime
                .now()
                .minus(duration)
                .toInstant(ZoneOffset.UTC);
    }

    public Stream<E> fromStartOfDay() {
        return since(LocalDateTime.now().toLocalDate().atStartOfDay().toInstant(ZoneOffset.UTC), getEntityClass().getSimpleName() + " for today");
    }

    public Stream<E> last(Duration duration) {
        return since( Instant.now().minus(duration), getEntityClass().getSimpleName() + " last " + duration.toMillis() + "ms");
    }

    public Stream<E> since(Instant instant, String message) {
        return select((root, cb) -> Stream.of(
                cb.ge(root.get(Event_.timestamp), instant.toEpochMilli())
        ), message);
    }

    @Transactional
    public int applyTTL(Duration duration) {
        log.trace("Applying TTL");
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaDelete<E> delete = cb.createCriteriaDelete(getEntityClass());
        Root<E> root = delete.from(getEntityClass());
        long inThePast = Instant.now().minus(duration).toEpochMilli();
        return entityManager.createQuery(
                delete.where(cb.le(root.get(Event_.timestamp), inThePast)))
                .executeUpdate();
    }


}
