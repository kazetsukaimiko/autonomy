package io.freedriver.autonomy.events.store;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Instant;
import java.util.UUID;

import io.freedriver.autonomy.MappingsTestResource;
import io.freedriver.autonomy.event.GenerationOrigin;
import io.freedriver.autonomy.event.input.sensors.DoubleValueSensorEvent;
import io.freedriver.autonomy.event.speech.SpeechEvent;
import io.freedriver.autonomy.event.speech.SpeechEventType;
import io.freedriver.autonomy.jpa.entity.VEDirectMessage;
import io.freedriver.autonomy.service.FloatValueSensorEventService;
import io.freedriver.autonomy.service.SpeechService;
import io.freedriver.autonomy.vedirect.VEDirectMessageService;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

@QuarkusTest
@QuarkusTestResource(MappingsTestResource.class)
@TestProfile(MongoDownProfile.class)
class MongoDownCallersTest {

    @Inject
    SpeechService speechService;

    @Inject
    VEDirectMessageService veDirectMessageService;

    @Inject
    FloatValueSensorEventService sensorEventService;

    @Test
    void speechVeDirectAndSensorWritesDoNotStopTheProcess() {
        SpeechEvent speech = new SpeechEvent(
                UUID.randomUUID().toString(),
                Instant.now(),
                GenerationOrigin.NON_HUMAN,
                "test.Source",
                "speaker",
                "evt",
                SpeechEventType.IMPERATIVE,
                "subject",
                "hello");
        speechService.observeEvent(speech);

        VEDirectMessage saved = veDirectMessageService.save(new VEDirectMessage(
                UUID.randomUUID().toString(),
                Instant.now(),
                GenerationOrigin.NON_HUMAN,
                "test.Source",
                "serial",
                "frame",
                null,
                null,
                null,
                "serial",
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null));
        assertEquals("serial", saved.serialNumber());

        DoubleValueSensorEvent sensor = sensorEventService.save(new DoubleValueSensorEvent(
                UUID.randomUUID().toString(),
                Instant.now(),
                GenerationOrigin.NON_HUMAN,
                "test.Source",
                "board",
                "evt",
                UUID.randomUUID(),
                "pin",
                1.5d));
        assertEquals(1.5d, sensor.eventValue());

        given()
                .when().get("/rest/config")
                .then()
                .statusCode(200);
        given()
                .when().get("/rest/event")
                .then()
                .statusCode(200);
        given()
                .when().get("/rest/vedirect/device")
                .then()
                .statusCode(200)
                .body("$", hasSize(0));
        given()
                .when().get("/rest/stream-serialization")
                .then()
                .statusCode(200)
                .body(equalTo("[\"alpha\",\"beta\",\"gamma\"]"));
    }
}
