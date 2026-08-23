package io.freedriver.autonomy.vedirect;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.freedriver.autonomy.cache.CacheKey;
import io.freedriver.autonomy.cdi.qualifier.AutonomyCache;
import io.freedriver.autonomy.cdi.qualifier.OneSecondCache;
import io.freedriver.autonomy.entity.view.ControllerHistoryView;
import io.freedriver.autonomy.entity.view.ControllerStateView;
import io.freedriver.autonomy.entity.view.ControllerTimeView;
import io.freedriver.autonomy.entity.view.ControllerView;
import io.freedriver.autonomy.events.store.EventQuery;
import io.freedriver.autonomy.events.store.EventTypes;
import io.freedriver.autonomy.jpa.entity.VEDirectMessage;
import io.freedriver.autonomy.service.crud.EventCrudService;
import io.freedriver.autonomy.util.Benchmark;
import io.freedriver.victron.VictronDevice;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class VEDirectMessageService extends EventCrudService<VEDirectMessage> {

    @Inject
    @AutonomyCache
    Map<CacheKey<VictronDevice, ControllerTimeView>, ControllerTimeView> timeViewCache;

    @Inject
    @AutonomyCache
    Map<CacheKey<VictronDevice, ControllerHistoryView>, ControllerHistoryView> historyViewCache;

    @Inject
    @AutonomyCache
    Map<LocalDate, Set<VictronDevice>> victronDeviceCache;

    @Inject
    @OneSecondCache
    Map<CacheKey<VictronDevice, VEDirectMessage>, VEDirectMessage> lastMessageCache;

    public VEDirectMessage save(VEDirectMessage veDirectMessage) {
        return persist(veDirectMessage);
    }

    public Stream<VEDirectMessage> last(VictronDevice device, Duration duration) {
        return query(EventQuery.bySource(
                eventType(),
                device.serialNumber(),
                Instant.now().minus(duration),
                Integer.MAX_VALUE));
    }

    public Stream<VEDirectMessage> byDevice(VictronDevice device) {
        return query(EventQuery.bySource(eventType(), device.serialNumber(), Instant.EPOCH, Integer.MAX_VALUE));
    }

    public long countByDevice(VictronDevice device) {
        return Benchmark.bench(() -> byDevice(device).count(), "countByDevice {}", device);
    }

    public ControllerHistoryView getControllerHistoryForToday(VictronDevice device) {
        return historyViewCache.computeIfAbsent(new CacheKey<>(device, ControllerHistoryView.class), k -> {
            List<VEDirectMessage> today = todayFor(k.base());
            return new ControllerHistoryView(
                    today.stream().map(VEDirectMessage::panelPower).filter(Objects::nonNull).mapToDouble(Number::doubleValue).max().orElse(0d),
                    today.stream().map(VEDirectMessage::panelVoltage).filter(Objects::nonNull).mapToDouble(Number::doubleValue).max().orElse(0d),
                    today.stream().map(VEDirectMessage::mainVoltage).filter(Objects::nonNull).mapToDouble(Number::doubleValue).max().orElse(0d),
                    today.stream().map(VEDirectMessage::yieldToday).filter(Objects::nonNull).mapToDouble(Number::doubleValue).max().orElse(0d));
        });
    }

    public ControllerTimeView getControllerTimeViewForToday(VictronDevice device) {
        return timeViewCache.computeIfAbsent(new CacheKey<>(device, ControllerTimeView.class), k -> {
            Instant startOfDay = getStartOfDay();
            ControllerTimeView view = new ControllerTimeView(Duration.between(startOfDay, Instant.now()));
            todayFor(k.base()).stream()
                    .collect(Collectors.groupingBy(
                            message -> Map.entry(
                                    Optional.ofNullable(message.stateOfOperation()),
                                    Optional.ofNullable(message.offReason())),
                            Collectors.counting()))
                    .forEach((key, count) -> view.apply(
                            key.getKey().orElse(null),
                            key.getValue().orElse(null),
                            count));
            return view;
        });
    }

    public Set<VictronDevice> devices() {
        return victronDeviceCache.computeIfAbsent(LocalDate.now(), ld -> queryAll()
                .filter(message -> message.productType() != null && message.serialNumber() != null)
                .map(message -> new VictronDevice(message.productType(), message.serialNumber()))
                .collect(Collectors.toSet()));
    }

    public Stream<VEDirectMessage> queryAll() {
        return query(EventQuery.all(eventType()));
    }

    public Stream<VictronDevice> distinctDevices() {
        return queryAll()
                .filter(message -> message.productType() != null && message.serialNumber() != null)
                .map(message -> new VictronDevice(message.productType(), message.serialNumber()))
                .distinct();
    }

    public Optional<VEDirectMessage> max(VictronDevice device) {
        return Benchmark.bench(
                () -> {
                    CacheKey<VictronDevice, VEDirectMessage> key = new CacheKey<>(device, VEDirectMessage.class);
                    VEDirectMessage cached = lastMessageCache.get(key);
                    if (cached != null) {
                        return Optional.of(cached);
                    }
                    Optional<VEDirectMessage> latest = byDevice(device)
                            .max(Comparator.nullsFirst(Comparator.comparing(VEDirectMessage::timestamp)));
                    latest.ifPresent(message -> lastMessageCache.put(key, message));
                    return latest;
                },
                "Last VEDirectMessage for " + device);
    }

    @Override
    public String eventType() {
        return EventTypes.VEDIRECT_MESSAGE;
    }

    @Override
    public Class<VEDirectMessage> payloadType() {
        return VEDirectMessage.class;
    }

    public ControllerView getControllerView(VictronDevice device) {
        return Benchmark.bench(
                () -> new ControllerView(
                        device,
                        getControllerTimeViewForToday(device),
                        max(device).map(ControllerStateView::new).orElse(null),
                        getControllerHistoryForToday(device)),
                "ControllerView for " + device);
    }

    private List<VEDirectMessage> todayFor(VictronDevice device) {
        return query(EventQuery.bySource(eventType(), device.serialNumber(), getStartOfDay(), Integer.MAX_VALUE))
                .toList();
    }
}
