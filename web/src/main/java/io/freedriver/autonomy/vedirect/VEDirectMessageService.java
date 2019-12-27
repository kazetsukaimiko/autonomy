package io.freedriver.autonomy.vedirect;

import io.freedriver.autonomy.ee.Autonomy;
import io.freedriver.ee.cdi.qualifier.NitriteDatabase;
import io.undertow.servlet.api.Deployment;
import kaze.victron.VEDirectMessage;
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

    private static final Set<VictronProduct> PRODUCT_CACHE = Collections.newSetFromMap(new ConcurrentHashMap<>());

    @Inject @NitriteDatabase(database = NitriteVEDirectMessage.class, deployment = Autonomy.DEPLOYMENT)
    private Nitrite nitrite;

    public void init(@Observes @Initialized(ApplicationScoped.class) Object init) {
        products();
    }


    /**
     * Saves a VEDirectMessage.
     * @param veDirectMessage
     * @return
     */
    public NitriteVEDirectMessage save(VEDirectMessage veDirectMessage) {
        // Add to Cache.
        VictronProduct.of(veDirectMessage)
                .ifPresent(PRODUCT_CACHE::add);
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
     * Get last Duration of messages for the given product.
     * @param product
     * @param duration
     * @return
     */
    public Stream<NitriteVEDirectMessage> last(VictronProduct product, Duration duration) {
        return query(
                ObjectFilters.gte("timestamp", ZonedDateTime.now().minus(duration)),
                ObjectFilters.eq("productType", product.getType()),
                ObjectFilters.eq("serialNumber", product.getSerialNumber()));
    }

    /**
     * Get messages for the given product.
     * @param product
     * @return
     */
    public Stream<NitriteVEDirectMessage> byProduct(VictronProduct product) {
        return query(
                ObjectFilters.eq("productType", product.getType()),
                ObjectFilters.eq("serialNumber", product.getSerialNumber()));
    }

    /**
     * Update the Product Cache.
     * @return
     */
    private Set<VictronProduct> updateProductCache() {
        query()
            .map(VictronProduct::of)
            .flatMap(Optional::stream)
            .distinct()
            .forEach(PRODUCT_CACHE::add);
        return PRODUCT_CACHE;
    }

    /**
     * Get all known VictronProducts.
     * @return
     */
    public Set<VictronProduct> products() {
        if (PRODUCT_CACHE.isEmpty()) {
            return updateProductCache();
        }
        return PRODUCT_CACHE;
    }

    /**
     * Get the ObjectRepository.
     * @return
     */
    private ObjectRepository<NitriteVEDirectMessage> getRepository() {
        return nitrite.getRepository(NitriteVEDirectMessage.class);
    }

}
