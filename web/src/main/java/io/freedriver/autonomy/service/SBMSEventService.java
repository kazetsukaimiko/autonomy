package io.freedriver.autonomy.service;

import io.freedriver.autonomy.jpa.entity.event.sbms.SBMSMessage;
import io.freedriver.autonomy.service.crud.EventCrudService;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.Default;
import javax.transaction.Transactional;

@ApplicationScoped
public class SBMSEventService extends EventCrudService<SBMSMessage> {
    @Override
    public Class<SBMSMessage> getEntityClass() {
        return SBMSMessage.class;
    }


    /*
     * EVENT HANDLERS
     */
    @Transactional
    public synchronized void actOnSBMSMessage(@Observes @Default kaze.serial.SBMSMessage sbmsMessage) {
        persist(new SBMSMessage(sbmsMessage));
    }

}
