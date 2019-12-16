package io.freedriver.jsonlink;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.naming.MalformedLinkException;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ConnectorTest {
    private static final Logger LOGGER = Logger.getLogger(ConnectorTest.class.getName());
    List<Connector> CONNECTORS;

    @BeforeEach
    public void init() {
        LOGGER.info("Getting connectors");
        CONNECTORS = allConnectors();
    }

    @Test
    public void allConnectorsHaveUniqueUUID() {
        List<UUID> allUUIDs = CONNECTORS.stream()
                .map(Connector::getUUID)
                .distinct()
                .peek(uuid -> LOGGER.info(String.valueOf(uuid)))
                .collect(Collectors.toList());
        assertEquals(CONNECTORS.size(), allUUIDs.size());
    }


    public static List<Connector> allConnectors() {
        return Connector.getDefault().stream()
                .collect(Collectors.toList());
    }

}
