package io.freedriver.autonomy.vedirect;

import io.freedriver.autonomy.cache.CacheKey;
import io.freedriver.autonomy.cdi.AttributeCache;
import io.freedriver.autonomy.cdi.qualifier.AutonomyCache;
import io.freedriver.autonomy.cdi.qualifier.OneSecondCache;
import io.freedriver.autonomy.entity.view.ControllerHistoryView;
import io.freedriver.autonomy.entity.view.ControllerStateView;
import io.freedriver.autonomy.entity.view.ControllerTimeView;
import io.freedriver.autonomy.entity.view.ControllerView;
import io.freedriver.autonomy.jpa.entity.VEDirectMessage;
import io.freedriver.autonomy.jpa.entity.VEDirectMessage_;
import io.freedriver.autonomy.service.crud.EventCrudService;
import io.freedriver.autonomy.util.Benchmark;
import io.freedriver.math.measurement.types.electrical.Energy;
import io.freedriver.math.measurement.types.electrical.Potential;
import io.freedriver.math.measurement.types.electrical.Power;
import io.freedriver.victron.VictronDevice;
import org.infinispan.Cache;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.Initialized;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import javax.persistence.Tuple;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import javax.persistence.criteria.Subquery;
import javax.transaction.Transactional;
import java.time.*;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@ApplicationScoped // TODO EventService
public class VEDirectMessageService extends EventCrudService<VEDirectMessage> {
    private static final Logger LOGGER = Logger.getLogger(VEDirectMessageService.class.getSimpleName());

    @Inject
    private AttributeCache maxCache;

    @Inject
    @AutonomyCache
    private Cache<CacheKey<VictronDevice, ControllerTimeView>, ControllerTimeView> timeViewCache;

    @Inject
    @AutonomyCache
    private Cache<CacheKey<VictronDevice, ControllerHistoryView>, ControllerHistoryView> historyViewCache;

    @Inject
    @AutonomyCache
    private Cache<LocalDate, Set<VictronDevice>> victronDeviceCache;

    @Inject
    @OneSecondCache
    private Cache<CacheKey<VictronDevice, VEDirectMessage>, VEDirectMessage> lastMessageCache;


    public void init(@Observes @Initialized(ApplicationScoped.class) Object init) {
        devices();
    }


    /**
     * Saves a VEDirectMessage.
     *
     * @param veDirectMessage
     * @return
     */
    @Transactional
    public VEDirectMessage save(VEDirectMessage veDirectMessage) {
        return persist(veDirectMessage);
    }


    /**
     * Get last Duration of messages for the given device.
     *
     * @param device
     * @param duration
     * @return
     */
    public Stream<VEDirectMessage> last(VictronDevice device, Duration duration) {
        return select((root, cb) -> Stream.of(
                cb.equal(root.get(VEDirectMessage_.serialNumber), device.getSerialNumber()),
                cb.ge(root.get(VEDirectMessage_.timestamp), Instant.now().minus(duration).toEpochMilli())
                ), "for device " + device + " last " + duration.toMillis() + "ms");
    }


    /**
     * Get messages for the given device.
     *
     * @param device
     * @return
     */
    public Stream<VEDirectMessage> byDevice(VictronDevice device) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<VEDirectMessage> cq = cb.createQuery(VEDirectMessage.class);
        Root<VEDirectMessage> root = cq.from(VEDirectMessage.class);
        cq.select(root);
        cq.where(cb.equal(root.get(VEDirectMessage_.serialNumber), device.getSerialNumber()));
        return queryStream(cq, "byDevice " + device);
    }

    /**
     * Get messages for the given device.
     *
     * @param device
     * @return
     */
    public long countByDevice(VictronDevice device) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Long> cq = cb.createQuery(Long.class);
        Root<VEDirectMessage> root = cq.from(VEDirectMessage.class);
        cq.select(cb.count(root));
        cq.where(cb.equal(root.get(VEDirectMessage_.serialNumber), device.getSerialNumber()));
        return Benchmark.bench(() -> entityManager.createQuery(cq).getSingleResult(),
                "countByDevice {}", device);
    }

    public ControllerHistoryView getControllerHistoryForToday(VictronDevice device) {
        return historyViewCache.computeIfAbsent(new CacheKey<>(device, ControllerHistoryView.class), k -> {
            CriteriaBuilder cb = entityManager.getCriteriaBuilder();
            CriteriaQuery<Tuple> cq = cb.createTupleQuery();
            Root<VEDirectMessage> root = cq.from(VEDirectMessage.class);
            cq.multiselect(
                    cb.max(root.get(VEDirectMessage_.panelPower)),
                    cb.max(root.get(VEDirectMessage_.panelVoltage)),
                    cb.max(root.get(VEDirectMessage_.mainVoltage)),
                    cb.max(root.get(VEDirectMessage_.yieldToday)))
                .where(cb.and(cb.ge(root.get(VEDirectMessage_.timestamp), getStartOfDay().toEpochMilli()),
                        cb.equal(root.get(VEDirectMessage_.serialNumber), k.getBase().getSerialNumber())));
            Tuple t = entityManager.createQuery(cq)
                    .getSingleResult();
            return new ControllerHistoryView(
                    t.get(0, Power.class).doubleValue(),
                    t.get(1, Potential.class).doubleValue(),
                    t.get(2, Potential.class).doubleValue(),
                    t.get(3, Energy.class).doubleValue());
        });
    }

    public ControllerTimeView getControllerTimeViewForToday(VictronDevice device) {
        return timeViewCache.computeIfAbsent(new CacheKey<>(device, ControllerTimeView.class), k -> {
            CriteriaBuilder cb = entityManager.getCriteriaBuilder();
            CriteriaQuery<Tuple> cq = cb.createTupleQuery();
            Root<VEDirectMessage> root = cq.from(VEDirectMessage.class);
            Instant startOfDay = getStartOfDay();
            cq.multiselect(
                    root.get(VEDirectMessage_.stateOfOperation),
                    root.get(VEDirectMessage_.offReason),
                    cb.count(root))
                    .where(cb.and(cb.ge(root.get(VEDirectMessage_.timestamp), startOfDay.toEpochMilli()),
                            cb.equal(root.get(VEDirectMessage_.serialNumber), k.getBase().getSerialNumber())))
                    .groupBy(root.get(VEDirectMessage_.stateOfOperation),
                            root.get(VEDirectMessage_.offReason));
            return entityManager.createQuery(cq)
                    .getResultStream()
                    .reduce(new ControllerTimeView(Duration.between(startOfDay, Instant.now())), (v, t) -> v.apply(
                            t.get(root.get(VEDirectMessage_.stateOfOperation)),
                            t.get(root.get(VEDirectMessage_.offReason)),
                            Optional.ofNullable(t.get(2))
                                .filter(Long.class::isInstance)
                                .map(Long.class::cast)
                                .orElse(0L)
                    ), (a, b) -> b);
        });
    }

    /**
     * Get all known VictronDevices.
     *
     * @return
     */
    public Set<VictronDevice> devices() {
        return victronDeviceCache.computeIfAbsent(LocalDate.now(), ld -> {
            CriteriaBuilder cb = entityManager.getCriteriaBuilder();
            CriteriaQuery<Tuple> cq = cb.createTupleQuery();
            Root<VEDirectMessage> root = cq.from(VEDirectMessage.class);
            cq.multiselect(
                    root.get(VEDirectMessage_.productType),
                    root.get(VEDirectMessage_.serialNumber))
                    .distinct(true);

            return entityManager
                    .createQuery(cq)
                    .getResultStream()
                    .map(t -> new VictronDevice(
                            t.get(root.get(VEDirectMessage_.productType)),
                            t.get(root.get(VEDirectMessage_.serialNumber))))
                    .collect(Collectors.toSet());
        });
    }

    public Stream<VEDirectMessage> queryAll() {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<VEDirectMessage> cq = cb.createQuery(VEDirectMessage.class);
        return entityManager.createQuery(cq.select(cq.from(VEDirectMessage.class)))
                .getResultStream();
    }

    public <T> Stream<VictronDevice> distinctDevices() {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Tuple> cq = cb.createTupleQuery();
        Root<VEDirectMessage> root = cq.from(VEDirectMessage.class);
        cq.multiselect(root.get(VEDirectMessage_.serialNumber), root.get(VEDirectMessage_.productType)).distinct(true);
        return queryStream(cq, "Distinct Devices")
                .map(tuple -> new VictronDevice(
                        tuple.get(root.get(VEDirectMessage_.productType)),
                        tuple.get(root.get(VEDirectMessage_.serialNumber))));
    }

    public Optional<VEDirectMessage> max(VictronDevice device) {
        return Benchmark.bench(() -> Optional.ofNullable(lastMessageCache.computeIfAbsent(new CacheKey<>(device, VEDirectMessage.class), k -> {
            CriteriaBuilder cb = entityManager.getCriteriaBuilder();
            CriteriaQuery<VEDirectMessage> cq = cb.createQuery(VEDirectMessage.class);
            Root<VEDirectMessage> veDirectMessageRoot = cq.from(VEDirectMessage.class);

            Subquery<Long> lastTimestampQuery = cq.subquery(Long.class);
            Root<VEDirectMessage> subQueryRoot = lastTimestampQuery.from(VEDirectMessage.class);
            lastTimestampQuery.select(cb.max(subQueryRoot.get(VEDirectMessage_.timestamp)));

            cq.select(veDirectMessageRoot);
            cq.where(cb.and(
                    cb.equal(veDirectMessageRoot.get(VEDirectMessage_.timestamp), lastTimestampQuery),
                    cb.equal(veDirectMessageRoot.get(VEDirectMessage_.serialNumber), k.getBase().getSerialNumber())));
                return entityManager.createQuery(cq)
                        .setFirstResult(0)
                        .setMaxResults(1)
                        .getSingleResult();
        })), "Last VEDirectMessage for " + device);
    }

    public static Instant getStartOfDay() {
        LocalDateTime localDateTime = LocalDateTime.now().toLocalDate().atStartOfDay();
        return localDateTime.toInstant(ZoneOffset.UTC);
    }

    @Override
    public Class<VEDirectMessage> getEntityClass() {
        return VEDirectMessage.class;
    }



    public ControllerView getControllerView(VictronDevice device) {
        return Benchmark.bench(() -> new ControllerView(
                device,
                getControllerTimeViewForToday(device),
                max(device).map(ControllerStateView::new).orElse(null),
                getControllerHistoryForToday(device)),
                "ControllerView for " + device);
    }
}
