package io.freedriver.autonomy.vedirect;

import io.freedriver.autonomy.ee.Autonomy;
import io.freedriver.ee.cdi.qualifier.NitriteDatabase;
import io.undertow.servlet.api.Deployment;
import kaze.victron.VEDirectMessage;
import kaze.victron.VictronDevice;
import kaze.victron.VictronProduct;
import org.dizitart.no2.Nitrite;
import org.dizitart.no2.objects.ObjectFilter;
import org.dizitart.no2.objects.ObjectRepository;
import org.dizitart.no2.objects.filters.ObjectFilters;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.Initialized;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

@ApplicationScoped
public class VEDirectMessageService {

    private static final Set<VictronDevice> DEVICE_CACHE = Collections.newSetFromMap(new ConcurrentHashMap<>());

    @Inject @NitriteDatabase(database = NitriteVEDirectMessage.class, deployment = Autonomy.DEPLOYMENT)
    private Nitrite nitrite;

    public void init(@Observes @Initialized(ApplicationScoped.class) Object init) {
        devices();
    }


    /**
     * Saves a VEDirectMessage.
     * @param veDirectMessage
     * @return
     */
    public NitriteVEDirectMessage save(VEDirectMessage veDirectMessage) {
        // Add to Cache.
        VictronDevice.of(veDirectMessage)
                .ifPresent(DEVICE_CACHE::add);
        // Upgrade to a NitriteEntity.
        NitriteVEDirectMessage nitriteVEDirectMessage = new NitriteVEDirectMessage(veDirectMessage);
        // Transfer nitriteId if needed.
        if (veDirectMessage instanceof NitriteVEDirectMessage) {
            nitriteVEDirectMessage.setNitriteId(((NitriteVEDirectMessage) veDirectMessage).getNitriteId());
        }
        // Insert or update- update should never happen.
        if (nitriteVEDirectMessage.getNitriteId() == null) {
            getRepository().insert(nitriteVEDirectMessage);
        } else {
            getRepository().update(nitriteVEDirectMessage);
        }
        return nitriteVEDirectMessage;
    }

    /**
     * Query the NitriteVEDirectMessage database.
     * @param filters
     * @return
     */
    private Stream<NitriteVEDirectMessage> query(ObjectFilter... filters) {
        return StreamSupport.stream(getRepository()
                .find(ObjectFilters.and(filters))
                .spliterator(), false);
    }

    /**
     * Get last Duration of messages for the given device.
     * @param device
     * @param duration
     * @return
     */
    public Stream<NitriteVEDirectMessage> last(VictronDevice device, Duration duration) {
        return query(
                ObjectFilters.gte("timestamp", ZonedDateTime.now().minus(duration)),
                ObjectFilters.eq("deviceType", device.getType()),
                ObjectFilters.eq("serialNumber", device.getSerialNumber()));
    }

    /**
     * Get messages for the given device.
     * @param device
     * @return
     */
    public Stream<NitriteVEDirectMessage> byDevice(VictronDevice device) {
        return query(
                ObjectFilters.eq("deviceType", device.getType()),
                ObjectFilters.eq("serialNumber", device.getSerialNumber()));
    }

    /**
     * Update the Device Cache.
     * @return
     */
    private Set<VictronDevice> updateDeviceCache() {
        query()
            .map(VictronDevice::of)
            .flatMap(Optional::stream)
            .distinct()
            .forEach(DEVICE_CACHE::add);
        return DEVICE_CACHE;
    }

    /**
     * Get all known VictronDevices.
     * @return
     */
    public Set<VictronDevice> devices() {
        if (DEVICE_CACHE.isEmpty()) {
            return updateDeviceCache();
        }
        return DEVICE_CACHE;
    }

    /**
     * Get the ObjectRepository.
     * @return
     */
    private ObjectRepository<NitriteVEDirectMessage> getRepository() {
        return nitrite.getRepository(NitriteVEDirectMessage.class);
    }

}
