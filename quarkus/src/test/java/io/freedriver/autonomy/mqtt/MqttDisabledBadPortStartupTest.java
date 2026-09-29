package io.freedriver.autonomy.mqtt;

import static io.restassured.RestAssured.given;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import org.junit.jupiter.api.Test;

@QuarkusTest
@TestProfile(MqttDisabledBadPortProfile.class)
class MqttDisabledBadPortStartupTest {

    @Test
    void badPortWithMqttUnsetStillServesRest() {
        given()
                .when().get("/rest/event")
                .then()
                .statusCode(200);
    }
}
