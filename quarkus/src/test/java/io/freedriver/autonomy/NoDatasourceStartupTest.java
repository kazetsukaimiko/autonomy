package io.freedriver.autonomy;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import java.time.Instant;

import io.freedriver.autonomy.events.store.EventMetadata;
import io.freedriver.autonomy.events.store.EventQuery;
import io.freedriver.autonomy.events.store.EventStore;
import io.freedriver.autonomy.events.store.EventTypes;
import io.freedriver.autonomy.events.store.NoOpEventStore;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.ConfigProvider;
import org.junit.jupiter.api.Test;

@QuarkusTest
@QuarkusTestResource(MappingsTestResource.class)
class NoDatasourceStartupTest {

    @Inject
    EventStore eventStore;

    @Test
    void startsWithNoDatasourceAndEmptyEventReads() {
        assertInstanceOf(NoOpEventStore.class, eventStore);
        EventMetadata metadata = new EventMetadata(Instant.parse("2026-09-27T00:00:00Z"), "test", "src", "evt");
        eventStore.append(EventTypes.JOYSTICK_EVENT, "payload", metadata);
        assertEquals(0, eventStore.query(EventQuery.all(EventTypes.JOYSTICK_EVENT)).count());

        var config = ConfigProvider.getConfig();
        assertFalse(config.getOptionalValue("quarkus.datasource.jdbc.url", String.class).isPresent());
        assertFalse(config.getOptionalValue("quarkus.datasource.db-kind", String.class).isPresent());

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
