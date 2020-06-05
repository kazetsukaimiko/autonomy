package io.freedriver.autonomy.service;

import io.freedriver.autonomy.jpa.entity.event.Event;

import javax.transaction.Transactional;

public abstract class EventService<E extends Event> extends JPACrudService<E> {
    @Override
    @Transactional
    public E persist(E event) {
        return super.persist(event);
    }
}
