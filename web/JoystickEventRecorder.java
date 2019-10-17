package io.freedriver.autonomy.async;

import io.freedriver.autonomy.entity.JoystickEvent;
import org.hrorm.KeylessDao;
import org.hrorm.Where;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.Default;
import javax.inject.Inject;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

@ApplicationScoped
public class JoystickEventRecorder {

    private static final Logger LOGGER = Logger.getLogger(JoystickEventRecorder.class.getName());

    @Inject
    KeylessDao<JoystickEvent> joystickEventDao;


    public void saveJoystickEvent(@Observes @Default JoystickEvent joystickEvent) throws IOException {
        LOGGER.info("Observed: " + joystickEvent.toString());
        try {
            joystickEventDao.insert(joystickEvent);
            long count = joystickEventDao.foldingSelect(0L, (e, evt) -> e+1L, Where.where());
            LOGGER.info(count + " Events recorded");
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, "Failed to connect", ex);
        }
    }
}
