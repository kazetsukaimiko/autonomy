package io.freedriver.autonomy.service.crud;

import io.freedriver.autonomy.Autonomy;
import io.freedriver.autonomy.jaxrs.ObjectMapperContextResolver;
import io.freedriver.autonomy.jpa.entity.event.Event;
import io.freedriver.autonomy.jpa.entity.event.Event_;
import io.freedriver.base.util.file.DirectoryProviders;
import io.freedriver.jsonlink.config.v2.Mappings;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaDelete;
import javax.persistence.criteria.Root;
import java.io.IOException;
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

    public Mappings getMappings() throws IOException {
        return ObjectMapperContextResolver.getMapper().readValue(
                DirectoryProviders.CONFIG
                        .getProvider()
                        .subdir(Autonomy.DEPLOYMENT)
                        .file("mappings_v2.json")
                        .get()
                        .toFile(),
                Mappings.class);
    }

    public Duration getTTL() {
        try {
            Mappings mappings = getMappings();
            return Duration.of(mappings.getEventTTL(), mappings.getEventTTLUnit());
        } catch (IOException ioe) {
            LOGGER.log(Level.WARNING, "Cannot read TTL:", ioe);
            return Duration.ofDays(7);
        }

    }

    @Override
    protected E persist(E event) {
        E evt = super.persist(event);
        int culled = applyTTL(getTTL());
        LOGGER.log(Level.INFO, culled + " removed applying TTL.");
        return evt;
    }

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
