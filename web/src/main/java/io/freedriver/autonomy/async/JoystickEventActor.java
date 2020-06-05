package io.freedriver.autonomy.async;

import io.freedriver.autonomy.jpa.entity.event.input.joystick.JoystickEvent;
import io.freedriver.autonomy.service.ConnectorService;
import io.freedriver.jsonlink.Connector;
import io.freedriver.jsonlink.config.ConnectorConfig;

import javax.annotation.Resource;
import javax.enterprise.concurrent.ManagedExecutorService;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Default;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import java.io.IOException;
import java.util.logging.Logger;


// TODO : Remove
@ApplicationScoped
public class JoystickEventActor {

    private static final Logger LOGGER = Logger.getLogger(JoystickEventActor.class.getName());

    @Inject
    private ConnectorConfig configuration;

    @Inject @Any
    private Instance<Connector> connectors;

    @Inject
    private ConnectorService connectorService;

    @Resource
    private ManagedExecutorService pool;

    public void actOnEvent(@Observes @Default JoystickEvent joystickEvent) throws IOException {
        LOGGER.info("Observed: " + joystickEvent.toString());
    }

}

