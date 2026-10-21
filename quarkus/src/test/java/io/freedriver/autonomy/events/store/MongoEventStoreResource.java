package io.freedriver.autonomy.events.store;

import java.util.Map;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.mongodb.MongoDBContainer;

/**
 * Starts a MongoDB test container for {@link MongoEventStoreTest}. Other tests do not use it.
 */
public class MongoEventStoreResource implements QuarkusTestResourceLifecycleManager {

    static volatile boolean started;

    private MongoDBContainer container;

    @Override
    public Map<String, String> start() {
        started = false;
        if (!dockerAvailable()) {
            return Map.of();
        }
        container = new MongoDBContainer("mongo:7.0");
        container.start();
        started = true;
        return Map.of(
                "quarkus.mongodb.connection-string", container.getReplicaSetUrl("autonomy"),
                "quarkus.mongodb.database", "autonomy");
    }

    @Override
    public void stop() {
        if (container != null) {
            container.stop();
        }
    }

    private static boolean dockerAvailable() {
        try {
            return DockerClientFactory.instance().isDockerAvailable();
        } catch (RuntimeException | Error e) {
            return false;
        }
    }
}
