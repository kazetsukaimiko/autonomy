package io.freedriver.jsonlink;

import io.freedriver.jsonlink.jackson.schema.v1.AnalogRead;
import io.freedriver.jsonlink.jackson.schema.v1.DigitalWrite;
import io.freedriver.jsonlink.jackson.schema.v1.Identifier;
import io.freedriver.jsonlink.jackson.schema.v1.Mode;
import io.freedriver.jsonlink.jackson.schema.v1.ModeSet;
import io.freedriver.jsonlink.jackson.schema.v1.Request;
import io.freedriver.jsonlink.jackson.schema.v1.Response;
import io.freedriver.jsonlink.pin.Pin;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static java.time.temporal.ChronoUnit.MILLIS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

public class ConnectorTest {
    private static final Logger LOGGER = Logger.getLogger(ConnectorTest.class.getName());
    static Identifier LED_PIN = Identifier.of(40); // Hallway

    List<UUID> allUUIDs;

    @BeforeEach
    public void init() {
        allUUIDs = Connectors.allConnectors()
                .map(Connector::getUUID)
                .distinct()
                .peek(uuid -> LOGGER.info(String.valueOf(uuid)))
                .collect(Collectors.toList());
    }

    @Test
    public void testReadAnalogs() {
        Request readAllAnalogs = new Request();

        IntStream.range(87, 89)
                .mapToObj(Identifier::new)
                .map(identifier -> new AnalogRead(identifier, 5.0f, 250f))
                .peek(System.out::println)
                .forEach(readAllAnalogs::analogRead);

        /*
        System.out.println(readAllAnalogs);
        if (true) {
            return;
        }*/
        Connectors.allConnectors()
                .findFirst()
                .ifPresent(connector -> {

                    for(int i=0;i<100;i++) {
                        Response r = connector.send(readAllAnalogs);
                        r.getAnalog()
                                .forEach(analogResponse -> {
                                    System.out.println(analogResponse);

                                });
                        try {
                            Thread.sleep(50);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                });
    }

    @Test
    public void allConnectorsHaveUniqueUUID() {
        assertEquals(Connectors.allConnectors().count(), allUUIDs.size());
    }

    @Test
    public void eachConnectorBlinks() {
        allUUIDs.forEach(uuid -> Connectors.getConnector(uuid)
                .ifPresent(connector -> {
                    Response modeResponse = connector.send(new Request().modeSet(new ModeSet(LED_PIN, Mode.OUTPUT)));
                    assertFalse(modeResponse.getError().stream()
                        .anyMatch(errorString -> errorString.toLowerCase()
                                .contains("invalid digital pin")));
                    IntStream.range(0, 10)
                            .forEach(i -> setStatus(connector, i % 2 == 0));
                }));
    }

    public static Response setStatus(Connector connector, boolean pinState) {
        LOGGER.info("Setting status : " + (pinState ? "TRUE":"FALSE"));
        Response r = connector.send(new Request()
                .digitalWrite(new DigitalWrite(LED_PIN, pinState)));
        delay(Duration.of(250, MILLIS));
        return r;
    }

    private static void delay(Duration duration) {
        try {
            Thread.sleep(duration.toMillis());
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public static List<Connector> allConnectors() {
        return Connectors.allConnectors()
                .collect(Collectors.toList());
    }

}
