package io.freedriver.autonomy.service.crud;

import io.freedriver.autonomy.iface.Positional;
import org.dizitart.no2.NitriteId;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

public interface CRUDInterface<I, T extends Positional> {
    // CREATE
    I insert(T entity);
    T save(T entity);

    // READ
    Optional<T> findOne(I id);
    Stream<T> findAllIds(Stream<I> ids);
    Stream<T> findAll();

    // UPDATE
    T update(T entity);
    List<T> saveOrder(List<T> entities);

    // DELETE
    Optional<NitriteId> delete(T entity);
    Stream<I> deleteAll(Stream<T> entity);
    Optional<I> deleteById(I id);
    Stream<I> deleteAllById(Stream<I> ids);

    long count();
}
