package io.freedriver.autonomy.service.crud;

import io.freedriver.autonomy.jpa.entity.event.Event;
import io.freedriver.autonomy.jpa.entity.event.Event_;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaDelete;
import javax.persistence.criteria.Root;
import javax.transaction.Transactional;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.logging.Level;
import java.util.logging.Logger;


public abstract class EventCrudService<E extends Event> extends JPACrudService<E> {
    private final Logger LOGGER = Logger.getLogger(getClass().getName());

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


    @Transactional
    public int applyTTL(Duration duration) {
        LOGGER.log(Level.FINEST, "Applying TTL");
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaDelete<E> delete = cb.createCriteriaDelete(getEntityClass());
        Root<E> root = delete.from(getEntityClass());
        long inThePast = Instant.now().minus(duration).toEpochMilli();
        return entityManager.createQuery(
                delete.where(cb.le(root.get(Event_.timestamp), inThePast)))
                .executeUpdate();
    }


}
