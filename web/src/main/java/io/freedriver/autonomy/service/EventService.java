package io.freedriver.autonomy.service;

import io.freedriver.autonomy.jpa.entity.event.Event;


public abstract class EventService<E extends Event> extends JPACrudService<E> {

}
