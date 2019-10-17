package io.freedriver.autonomy.cdi.provider;

import io.freedriver.autonomy.cdi.qualifier.H2Database;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.enterprise.inject.spi.InjectionPoint;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.logging.Level;
import java.util.logging.Logger;

@ApplicationScoped
public class H2ConnectionProvider {
    private static final Logger LOGGER = Logger.getLogger(H2ConnectionProvider.class.getName());
    private static final Path configPath = Paths.get(System.getProperty("user.home"), ".config/autonomy");

    /**
     * Produces a javax.sql.Connection Object that is bound to an H2 Datastore
     */
    @Produces @H2Database("")
    public Connection connect(InjectionPoint injectionPoint) throws IOException {

        String schemaName = injectionPoint.getQualifiers().stream()
                .filter(H2Database.class::isInstance)
                .map(H2Database.class::cast)
                .filter(h2DbAnnotation -> !h2DbAnnotation.value().trim().isEmpty())
                .findFirst()
                .map(H2Database::value)
                .orElseThrow(() -> new RuntimeException("You must specify @"+H2Database.class.getSimpleName()+"(\"schemaName\")"));

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
}
