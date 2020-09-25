package io.freedriver.autonomy.service;

import io.freedriver.autonomy.cdi.qualifier.OneSecondCache;
import io.freedriver.autonomy.entity.view.LithiumBatteryView;
import io.freedriver.autonomy.jpa.entity.EntityBase_;
import io.freedriver.autonomy.jpa.entity.event.Event;
import io.freedriver.autonomy.jpa.entity.event.Event_;
import io.freedriver.autonomy.jpa.entity.event.sbms.SBMSMessage;
import io.freedriver.autonomy.service.crud.EventCrudService;
import org.infinispan.Cache;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.Default;
import javax.inject.Inject;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import javax.transaction.Transactional;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@ApplicationScoped
public class SBMSEventService extends EventCrudService<SBMSMessage> {
    private static final UUID LATEST_UNIQUES = UUID.randomUUID();

    @Override
    public Class<SBMSMessage> getEntityClass() {
        return SBMSMessage.class;
    }

    @Inject
    @OneSecondCache
    private Cache<UUID, List<SBMSMessage>> oneSecondCache;

    /*
     * EVENT HANDLERS
     */
    @Transactional
    public synchronized void actOnSBMSMessage(@Observes @Default io.freedriver.electrodacus.sbms.SBMSMessage sbmsMessage) {
        persist(new SBMSMessage(sbmsMessage));
    }

    public List<SBMSMessage> getLatestUnique() {
        return oneSecondCache.computeIfAbsent(LATEST_UNIQUES, uuid -> {
            CriteriaBuilder cb = entityManager.getCriteriaBuilder();
            CriteriaQuery<SBMSMessage> cq = cb.createQuery(SBMSMessage.class);
            Root<SBMSMessage> root = cq.from(SBMSMessage.class);
            cq
                    .select(root)
                    .orderBy(cb.desc(root.get(EntityBase_.id)))
                    .where(cb.ge(root.get(Event_.timestamp),
                            ago(Duration.ofMinutes(1)).toEpochMilli()));

            List<SBMSMessage> lastMinute = queryStream(cq, "Last minute of data")
                    .collect(Collectors.toList());

            Set<String> sources = lastMinute.stream()
                    .map(Event::getSourceId)
                    .collect(Collectors.toSet());

            List<SBMSMessage> latestOfEach = new ArrayList<>();
            sources.forEach(source -> lastMinute.stream()
                    .filter(sbmsMessage -> Objects.equals(source, sbmsMessage.getSourceId()))
                    .max(Comparator.comparing(Event::getTimestamp))
                    .ifPresent(latestOfEach::add));

            return latestOfEach;
        });
    }

    public LithiumBatteryView getLithiumBatteryView() {
        List<SBMSMessage> latest = getLatestUnique();
        return new LithiumBatteryView(latest.stream());
    }

    public LithiumBatteryView getLithiumBatteryViewOld() {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<String> cq = cb.createQuery(String.class);
        Root<SBMSMessage> root = cq.from(SBMSMessage.class);
        cq
                .select(root.get(Event_.sourceId))
                .distinct(true)
                .where(cb.ge(root.get(Event_.timestamp), getStartOfDay().toEpochMilli()));

        List<String> allDevices = queryStream(cq, "Distinct SBMS0 sourceIds")
                .collect(Collectors.toList());

        CriteriaQuery<SBMSMessage> lq = cb.createQuery(SBMSMessage.class);
        lq
                .select(root)
                .where(root.get(Event_.sourceId).in(allDevices))
                .orderBy(cb.desc(root.get(Event_.sourceId)), cb.desc(root.get(Event_.timestamp)));


        return new LithiumBatteryView(queryStream(lq, allDevices.size(), "Distinct SBMS0 Rows"));
    }

}
