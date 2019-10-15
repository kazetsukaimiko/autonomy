package io.freedriver.autonomy.cdi.provider;

import io.freedriver.autonomy.cdi.qualifier.H2Database;
import io.freedriver.autonomy.entity.JoystickEvent;
import io.freedriver.autonomy.hrorm.JoystickTypeConverter;
import io.freedriver.autonomy.hrorm.PathConverter;
import org.hrorm.DaoBuilder;
import org.hrorm.KeylessDao;
import org.hrorm.Schema;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Default;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

@ApplicationScoped
public class DaoProvider {

    private static final Logger LOGGER = Logger.getLogger(DaoProvider.class.getName());

    @Inject
    @H2Database("events")
    private Connection connection;

    @Produces @Default
    public KeylessDao<JoystickEvent> getJoystickEventDao() throws IOException, SQLException {
        DaoBuilder<JoystickEvent> builder = new DaoBuilder<JoystickEvent>("event", JoystickEvent::new)
                .withPrimaryKey("id", "id_sequence", JoystickEvent::getNumber, JoystickEvent::setNumber)
                .withConvertingStringColumn("path", JoystickEvent::getPath, JoystickEvent::setPath, new PathConverter())
                .withInstantColumn("timestamp", JoystickEvent::getTimestamp, JoystickEvent::setTimestamp)
                .withConvertingStringColumn("type", JoystickEvent::getType, JoystickEvent::setType, new JoystickTypeConverter())
                .withLongColumn("number", JoystickEvent::getNumber, JoystickEvent::setNumber)
                .withLongColumn("value", JoystickEvent::getValue, JoystickEvent::setValue);

        try {
            Schema schema = new Schema(builder);
            connection.prepareStatement(schema.sql()).execute();
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Couldnt create schema", e);
        }
        return builder.buildDao(connection);
    }

}
