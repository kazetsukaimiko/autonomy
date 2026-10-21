package io.freedriver.autonomy.events.store;

import java.util.Map;

import io.quarkus.test.junit.QuarkusTestProfile;

public class MongoDownProfile implements QuarkusTestProfile {

    @Override
    public Map<String, String> getConfigOverrides() {
        return Map.of(
                "quarkus.mongodb.connection-string", "mongodb://127.0.0.1:1",
                "quarkus.mongodb.server-selection-timeout", "200ms",
                "quarkus.mongodb.connect-timeout", "200ms",
                "quarkus.mongodb.read-timeout", "200ms");
    }
}
