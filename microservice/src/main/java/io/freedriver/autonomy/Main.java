package io.freedriver.autonomy;

import io.freedriver.microservice.embed.EmbeddedDatabase;
import io.freedriver.microservice.embed.EmbeddedDatabaseConfig;
import io.freedriver.microservice.embed.EmbeddedWebServer;
import io.freedriver.microservice.embed.EmbeddedWebServerConfig;
import io.freedriver.microservice.embed.impl.EmbeddedUndertowWebServer;
import io.freedriver.microservice.embed.impl.HSQLDBEmbeddedDatabase;
import io.undertow.Undertow;
import io.undertow.servlet.api.DeploymentManager;

import javax.servlet.ServletException;
import java.io.IOException;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Main {
    private static final Logger LOGGER = Logger.getLogger(Main.class.getName());
    private static final int DATABASE_PORT = 9001;

    private static Undertow server;
    private static DeploymentManager manager;

    public static void main(String[] args) throws Exception {
        try (EmbeddedDatabase database = startEmbeddedDatabase()) {
            // TODO: Liquibase Update before deployment
            try (EmbeddedWebServer server = startEmbeddedWebServer()) {
                server.waitUntilDone();
                database.waitUntilDone();
            } catch (Exception e) {
                e.printStackTrace();
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        System.out.println("Escaped.");
    }

    public static void setLevel(Level targetLevel) {
        Logger root = Logger.getLogger("");
        root.setLevel(targetLevel);
        for (Handler handler : root.getHandlers()) {
            handler.setLevel(targetLevel);
        }
        System.out.println("level set: " + targetLevel.getName());
    }

    private static EmbeddedDatabase startEmbeddedDatabase() throws IOException {
        EmbeddedDatabase edb = new HSQLDBEmbeddedDatabase(embeddedDatabaseConfig());
        return edb;
    }

    private static EmbeddedDatabaseConfig embeddedDatabaseConfig() {
        return EmbeddedDatabaseConfig
                .defaultConfig()
                .addDatabase("autonomy.db");
    }

    private static EmbeddedWebServer startEmbeddedWebServer() throws ServletException {
        return new EmbeddedUndertowWebServer(Main.class, EmbeddedWebServerConfig.defaultServer());
    }
}
