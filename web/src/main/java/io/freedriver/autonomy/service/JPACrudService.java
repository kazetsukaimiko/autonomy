package io.freedriver.autonomy.service;

import io.freedriver.autonomy.Autonomy;
import io.freedriver.autonomy.jpa.entity.EntityBase;
import io.freedriver.autonomy.jpa.entity.event.Event_;
import io.freedriver.autonomy.util.Benchmark;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.criteria.*;
import javax.transaction.Transactional;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public abstract class JPACrudService<E extends EntityBase> {
    @PersistenceContext(name = Autonomy.DEPLOYMENT, unitName = Autonomy.DEPLOYMENT)
    protected EntityManager entityManager;

    public abstract Class<E> getEntityClass();


    public Stream<E> select(BiFunction<Root<E>, CriteriaBuilder, Stream<Predicate>> selectionXFunction, String description) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<E> cq = cb.createQuery(getEntityClass());
        Root<E> root = cq.from(getEntityClass());
        Predicate[] predicates = selectionXFunction.apply(root, cb)
                .collect(Collectors.toList())
                .toArray(new Predicate[] {});
        return queryStream(cq.select(root)
                .where(cb.and(predicates)), description);
    }

    @Transactional
    public E persist(E event) {
        entityManager.persist(event);
        return event;
    }

    public Optional<E> get(long id) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<E> cq = cb.createQuery(getEntityClass());
        Root<E> root = cq.from(getEntityClass());
        cq.select(root)
                .where(cb.equal(root.get(Event_.id), id));
        return Optional.ofNullable(entityManager.createQuery(cq).getSingleResult());
    }

    public int delete(long id) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaDelete<E> cq = cb.createCriteriaDelete(getEntityClass());
        Root<E> root = cq.from(getEntityClass());
        return entityManager.createQuery(cq.where(cb.equal(root.get(Event_.id), id)))
                .executeUpdate();
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
}
