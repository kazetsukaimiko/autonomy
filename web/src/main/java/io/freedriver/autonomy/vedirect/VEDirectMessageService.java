package io.freedriver.autonomy.vedirect;

import io.freedriver.autonomy.Autonomy;
import io.freedriver.autonomy.jpa.entity.VEDirectMessage;
import io.freedriver.autonomy.jpa.entity.VEDirectMessage_;
import io.freedriver.autonomy.util.Benchmark;
import kaze.math.measurement.units.Power;
import kaze.math.number.ScaledNumber;
import kaze.victron.VictronDevice;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.Initialized;
import javax.enterprise.event.Observes;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import javax.persistence.metamodel.SingularAttribute;
import javax.transaction.Transactional;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@ApplicationScoped
public class VEDirectMessageService {
    private static final Logger LOGGER = Logger.getLogger(VEDirectMessageService.class.getSimpleName());
    private static final Set<VictronDevice> DEVICE_CACHE = Collections.newSetFromMap(new ConcurrentHashMap<>());

    @PersistenceContext(name = Autonomy.DEPLOYMENT, unitName = Autonomy.DEPLOYMENT)
    private EntityManager entityManager;


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
    public VEDirectMessage save(kaze.victron.VEDirectMessage veDirectMessage) {
        // Add to Cache.
        VictronDevice.of(veDirectMessage)
                .ifPresent(DEVICE_CACHE::add);

        return saveJPA(new VEDirectMessage(veDirectMessage));
    }

    private VEDirectMessage saveJPA(VEDirectMessage veDirectMessage) {
        entityManager.persist(veDirectMessage);
        return veDirectMessage;
    }

    // TODO: Revert full buffer
    public <T> Stream<T> queryStream(CriteriaQuery<T> cq, String description) {
        return Benchmark.bench(() ->
                        entityManager
                                .createQuery(cq)
                                .getResultStream()
                                .collect(Collectors.toList())
                                .stream(),
                description);
    }

    // TODO: Revert full buffer
    public <T> Stream<T> queryStream(CriteriaQuery<T> cq, int limit, String description) {
        return Benchmark.bench(() ->
                        entityManager
                                .createQuery(cq)
                                .setMaxResults(limit)
                                .getResultStream()
                                .collect(Collectors.toList())
                                .stream(),
                description);
    }


    /**
     * Get last Duration of messages for the given device.
     *
     * @param device
     * @param duration
     * @return
     */
    public Stream<VEDirectMessage> last(VictronDevice device, Duration duration) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<VEDirectMessage> cq = cb.createQuery(VEDirectMessage.class);
        Root<VEDirectMessage> root = cq.from(VEDirectMessage.class);
        cq.select(root);
        cq.where(cb.and(
                cb.equal(root.get(VEDirectMessage_.serialNumber), device.getSerialNumber()),
                cb.ge(root.get(VEDirectMessage_.timestamp), Instant.now().minus(duration).toEpochMilli())));
        cq.orderBy(cb.desc(root.get(VEDirectMessage_.id)));
        return queryStream(cq, "for device " + device + " last " + duration.toMillis() + "ms");
    }


    public Stream<VEDirectMessage> fromSunUp(VictronDevice device) {
        return Benchmark.bench(() -> {
            List<VEDirectMessage> last = last(device, Duration.between(getStartOfDay(), Instant.now()))
                    .collect(Collectors.toList());
            long sunUpId = last.stream()
                    .filter(m -> m.getPanelPower().greaterThanOrEqualTo(new Power(ScaledNumber.of(20))))
                    .mapToLong(VEDirectMessage::getId)
                    .min()
                    .getAsLong();
            return last.stream()
                    .filter(m -> m.getId() >= sunUpId)
                    .sorted(Comparator.comparingLong(VEDirectMessage::getId));
        }, "fromSunUp");


        /* TODO Figure out query

        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<VEDirectMessage> cq = cb.createQuery(VEDirectMessage.class);
        Root<VEDirectMessage> root = cq.from(VEDirectMessage.class);
        Subquery<Long> startIdQuery = cq.subquery(Long.class);
        Root<VEDirectMessage> subQueryRoot = startIdQuery.from(VEDirectMessage.class);
        startIdQuery.select(cb.min(subQueryRoot.get(VEDirectMessage_.id)))
                .where(cb.and(
                        cb.ge(root.get(VEDirectMessage_.timestamp), getStartOfDay().toEpochMilli()),
                        cb.ge(root.get(VEDirectMessage_.panelPower).as(BigDecimal.class), 20.0)
        ));
        cq
                .select(root)
                .where(cb.greaterThanOrEqualTo(root.get(VEDirectMessage_.id), startIdQuery))
                .orderBy(cb.asc(root.get(VEDirectMessage_.id)));
        return queryStream(cq, "from Sun Up of device " + device);

         */
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


    /**
     * Update the Device Cache.
     *
     * @return
     */
    private Set<VictronDevice> updateDeviceCache() {
        queryAll()
                .map(veDirectMessage -> VictronDevice.of(veDirectMessage.getProductType(), veDirectMessage.getSerialNumber()))
                .flatMap(Optional::stream)
                .distinct()
                .forEach(DEVICE_CACHE::add);
        return DEVICE_CACHE;
    }

    /**
     * Get all known VictronDevices.
     *
     * @return
     */
    public Set<VictronDevice> devices() {
        if (DEVICE_CACHE.isEmpty()) {
            return updateDeviceCache();
        }
        return DEVICE_CACHE;
    }

    public Stream<VEDirectMessage> queryAll() {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<VEDirectMessage> cq = cb.createQuery(VEDirectMessage.class);
        return entityManager.createQuery(cq.select(cq.from(VEDirectMessage.class)))
                .getResultStream();
    }

    public <T> Stream<T> distinct(VictronDevice device, SingularAttribute<VEDirectMessage, T> attribute) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<T> cq = cb.createQuery(attribute.getBindableJavaType());
        Root<VEDirectMessage> veDirectMessageRoot = cq.from(VEDirectMessage.class);
        cq.select(veDirectMessageRoot.get(attribute)).distinct(true);
        cq.where(cb.equal(veDirectMessageRoot.get(VEDirectMessage_.serialNumber), device.getSerialNumber()));
        return entityManager.createQuery(cq).getResultStream()
                .filter(Objects::nonNull);
    }


    public <T extends Number> T max(VictronDevice device, SingularAttribute<VEDirectMessage, T> attribute) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<T> cq = cb.createQuery(attribute.getBindableJavaType());
        Root<VEDirectMessage> veDirectMessageRoot = cq.from(VEDirectMessage.class);
        cq.select(cb.max(veDirectMessageRoot.get(attribute)));
        cq.where(cb.equal(veDirectMessageRoot.get(VEDirectMessage_.serialNumber), device.getSerialNumber()));
        return entityManager.createQuery(cq).getSingleResult();
    }

    public Optional<VEDirectMessage> max(VictronDevice device) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<VEDirectMessage> cq = cb.createQuery(VEDirectMessage.class);
        Root<VEDirectMessage> veDirectMessageRoot = cq.from(VEDirectMessage.class);
        cq.select(veDirectMessageRoot);
        cq.where(cb.equal(veDirectMessageRoot.get(VEDirectMessage_.serialNumber), device.getSerialNumber()))
                .orderBy(cb.desc(veDirectMessageRoot.get(VEDirectMessage_.timestamp)));
        return queryStream(cq, 1, "Max")
                .findFirst();
    }

    public static Instant getStartOfDay() {
        LocalDateTime localDateTime = LocalDateTime.now().toLocalDate().atStartOfDay();
        return localDateTime.toInstant(ZoneOffset.UTC);
    }

}
