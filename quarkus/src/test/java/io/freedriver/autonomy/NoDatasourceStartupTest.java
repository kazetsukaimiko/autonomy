package io.freedriver.autonomy;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.Locale;

import io.freedriver.autonomy.events.store.EventMetadata;
import io.freedriver.autonomy.events.store.EventStore;
import io.freedriver.autonomy.events.store.EventTypes;
import io.freedriver.autonomy.events.store.MongoEventStore;
import io.freedriver.autonomy.scheduled.ScheduledCommands;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.junit.jupiter.api.Test;

@QuarkusTest
@QuarkusTestResource(MappingsTestResource.class)
class NoDatasourceStartupTest {

    @Inject
    EventStore eventStore;

    @Inject
    ScheduledCommands scheduled;

    @Test
    void startsWithNoDatasourceAndEmptyEventReads() {
        assertInstanceOf(MongoEventStore.class, eventStore);
        assertThrows(ClassNotFoundException.class,
                () -> Class.forName("io.freedriver.autonomy.scheduled.PendingCommandEntity"));
        assertTrue(scheduled.available());
        // Before today, so GET /rest/event stays empty whether or not Mongo kept the write.
        EventMetadata metadata = new EventMetadata(Instant.parse("2026-09-27T00:00:00Z"), "test", "src", "evt");
        eventStore.append(EventTypes.JOYSTICK_EVENT, "payload", metadata);

        Config config = ConfigProvider.getConfig();
        assertFalse(config.getOptionalValue("quarkus.datasource.jdbc.url", String.class).isPresent());
        assertFalse(config.getOptionalValue("quarkus.datasource.db-kind", String.class).isPresent());
        assertFalse(config.getOptionalValue("quarkus.hibernate-orm.database.generation", String.class).isPresent());
        String storeDir = config.getValue("autonomy.scheduled.store-dir", String.class);
        assertTrue(storeDir.startsWith(System.getProperty("java.io.tmpdir")));
        assertFalse(storeDir.contains(".local/share"));
        for (String name : config.getPropertyNames()) {
            String value = config.getOptionalValue(name, String.class).orElse("");
            assertFalse(value.toLowerCase(Locale.ROOT).contains("hsqldb"), name);
            assertFalse(value.contains("9001/autonomy.db"), name);
        }

        given()
                .when().get("/rest/simple")
                .then()
                .statusCode(200);

        given()
                .when().get("/rest/event")
                .then()
                .statusCode(200)
                .body(equalTo("[]"));

        given()
                .when().get("/rest/stream-serialization")
                .then()
                .statusCode(200)
                .body(equalTo("[\"alpha\",\"beta\",\"gamma\"]"));

        given()
                .when().get("/rest/vedirect/device")
                .then()
                .statusCode(200)
                .body("$", hasSize(0));

        given()
                .when().get("/rest/vedirect/device/unknown-serial")
                .then()
                .statusCode(500)
                .body(containsString("Unknown Device Serial"));
    }
}
