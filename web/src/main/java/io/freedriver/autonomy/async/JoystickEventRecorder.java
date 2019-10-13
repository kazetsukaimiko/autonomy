package io.freedriver.autonomy.async;

import io.freedriver.autonomy.hrorm.JoystickTypeConverter;
import io.freedriver.autonomy.hrorm.PathConverter;
import io.freedriver.controller.JoystickEvent;
import io.freedriver.jsonlink.Connector;
import org.hrorm.IndirectKeylessDaoBuilder;
import org.hrorm.KeylessDao;
import org.hrorm.Schema;
import org.hrorm.Where;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.Default;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;

@ApplicationScoped
public class JoystickEventRecorder {

    private static final Logger LOGGER = Logger.getLogger(JoystickEventRecorder.class.getName());
    private static final Path configPath = Paths.get(System.getProperty("user.home"), ".config/autonomy");

    KeylessDao<JoystickEvent> joystickEventDao;

    public KeylessDao<JoystickEvent> getJoystickEventDao() throws IOException, SQLException {
        if (joystickEventDao == null) {
            IndirectKeylessDaoBuilder<JoystickEvent, JoystickEvent> builder = new IndirectKeylessDaoBuilder<JoystickEvent, JoystickEvent>("event", JoystickEvent::new, Function.identity())
                    .withConvertingStringColumn("path", JoystickEvent::getPath, JoystickEvent::setPath, new PathConverter())
                    .withInstantColumn("timestamp", JoystickEvent::getNow, JoystickEvent::setNow)
                    .withConvertingStringColumn("type", JoystickEvent::getType, JoystickEvent::setType, new JoystickTypeConverter())
                    .withLongColumn("time", JoystickEvent::getTime, JoystickEvent::setTime)
                    .withLongColumn("number", JoystickEvent::getNumber, JoystickEvent::setNumber)
                    .withLongColumn("value", JoystickEvent::getValue, JoystickEvent::setValue);


            Connection connection = connect("events");
            try {
                Schema schema = new Schema(builder);
                connection.prepareStatement(schema.sql()).execute();
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Couldnt create schema", e);
            }
            joystickEventDao = builder.buildDao(connect("events"));
        }
        return joystickEventDao;
    }


    public static Connection connect(String schemaName) throws IOException {
        Path databasePath = Paths.get(configPath.toAbsolutePath().toString(), schemaName);
        try {
            if (!Files.exists(databasePath)) {
                Files.createDirectories(databasePath);
            }
            Class.forName("org.h2.Driver");

            String url = String.join("", "jdbc:h2:", databasePath.toAbsolutePath().toString(), "/h2.db");
            return DriverManager.getConnection(url);
        } catch (Exception ex){
            LOGGER.log(Level.SEVERE, "Failed to connect", ex);
            throw new RuntimeException(ex);
        }
    }


    public void saveJoystickEvent(@Observes @Default JoystickEvent joystickEvent) throws IOException {
        LOGGER.info("Observed: " + joystickEvent.toString());
        try {
            getJoystickEventDao().insert(joystickEvent);
            long count = getJoystickEventDao().foldingSelect(0L, (e, evt) -> e+1L, Where.where());
            LOGGER.info(count + " Events recorded");
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, "Failed to connect", ex);
        }
    }
}
