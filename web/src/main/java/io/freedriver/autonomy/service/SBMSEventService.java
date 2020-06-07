package io.freedriver.autonomy.service;

import io.freedriver.autonomy.jpa.entity.event.sbms.SBMSMessage;
import io.freedriver.autonomy.service.crud.EventCrudService;

import javax.enterprise.event.Observes;
import javax.enterprise.inject.Default;

public class SBMSEventService extends EventCrudService<SBMSMessage> {
    @Override
    public Class<SBMSMessage> getEntityClass() {
        return SBMSMessage.class;
    }


    /*
     * EVENT HANDLERS
     */
    public synchronized void actOnVEDirectMessage(@Observes @Default kaze.serial.SBMSMessage sbmsMessage) {
        persist(new SBMSMessage(sbmsMessage));
    }

}
