package io.freedriver.autonomy.cdi;

import org.infinispan.Cache;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.persistence.metamodel.SingularAttribute;
import java.util.function.Function;

@ApplicationScoped
public class AttributeCache {

    @Inject
    @io.freedriver.autonomy.cdi.qualifier.AttributeCache
    private Cache<SingularAttribute<?, ?>, Object> backingCache;

    @SuppressWarnings("unchecked")
    public <E, T, A extends SingularAttribute<E, T>> T computeIfAbsent(A attribute, Function<A, Object> mappingFunction) {
        if (!backingCache.containsKey(attribute)) {
            backingCache.put(attribute, mappingFunction.apply(attribute));
        }
        return (T) backingCache.get(attribute);
    }
}
