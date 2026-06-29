package io.freedriver.autonomy.cdi;

import io.freedriver.serial.api.connection.SerialConnectionManager;
import io.freedriver.serial.connection.DefaultSerialConnectionManager;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Disposes;
import jakarta.enterprise.inject.Produces;

@ApplicationScoped
public class SerialConnectionManagerProducer {

    @Produces
    @ApplicationScoped
    public SerialConnectionManager serialConnectionManager() {
        return DefaultSerialConnectionManager.create();
    }

    void dispose(@Disposes SerialConnectionManager manager) {
        manager.close();
    }
}