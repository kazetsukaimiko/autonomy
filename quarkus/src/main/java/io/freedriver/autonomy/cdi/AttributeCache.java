package io.freedriver.autonomy.cdi;

import java.util.function.Function;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.metamodel.SingularAttribute;
import org.infinispan.Cache;

@ApplicationScoped
public class AttributeCache {

    @Inject
    @io.freedriver.autonomy.cdi.qualifier.AttributeCache
    Cache<SingularAttribute<?, ?>, Object> backingCache;

    @SuppressWarnings("unchecked")
    public <E, T, A extends SingularAttribute<E, T>> T computeIfAbsent(A attribute, Function<A, Object> mappingFunction) {
        if (!backingCache.containsKey(attribute)) {
            backingCache.put(attribute, mappingFunction.apply(attribute));
        }
        return (T) backingCache.get(attribute);
    }
}
