package io.freedriver.autonomy.service;

import io.freedriver.autonomy.Autonomy;
import io.freedriver.autonomy.jpa.entity.EntityBase;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

public class JPACrudService<E extends EntityBase> {
    @PersistenceContext(name = Autonomy.DEPLOYMENT, unitName = Autonomy.DEPLOYMENT)
    protected EntityManager entityManager;

    public E persist(E event) {
        entityManager.persist(event);
        return event;
    }
}
