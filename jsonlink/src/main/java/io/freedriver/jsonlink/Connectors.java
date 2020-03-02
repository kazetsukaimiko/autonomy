package io.freedriver.jsonlink;

import io.freedriver.jsonlink.config.ConnectorConfig;
import jssc.SerialPort;
import jssc.SerialPortList;

import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class Connectors {
    private static final ExecutorService THREADPOOL = Executors.newSingleThreadExecutor();
    private static final Set<Connector> ALL_CONNECTORS = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private static final Map<String, FailedConnector> FAILED_CONNECTORS = new ConcurrentHashMap<>();
    private static final Logger LOGGER = Logger.getLogger(Connectors.class.getName());

    private static Consumer<String> callback;

    private Connectors() {
        // Prevent Construction
    }

    private static synchronized <T> T connectors(Function<Stream<Connector>, T> setFunction) {
        return setFunction.apply(new HashSet<>(ALL_CONNECTORS).stream());
    }

    private static synchronized Optional<Connector> findByDeviceId(String device) {
        return connectors(connectors -> connectors
                .filter(connector -> Objects.equals(device, connector.device())))
                .findFirst()
                .map(ConcurrentConnector::new);
    }

    private static synchronized Future<Connector> createConnector(String device) {
        LOGGER.info("Creating connector: " + device);
        return THREADPOOL.submit(() -> {
            SerialConnector serialConnector = new SerialConnector(new SerialPort(device));
            LOGGER.info("Getting UUID:");
            serialConnector.getUUID();
            ALL_CONNECTORS.add(serialConnector);
            return new ConcurrentConnector(serialConnector);
        });
    }

    public static synchronized Map<String, FailedConnector> getFailedConnectors() {
        Set<String> toRemove = FAILED_CONNECTORS.keySet()
                .stream()
                .filter(device -> FAILED_CONNECTORS.get(device).failureExpired())
                .collect(Collectors.toSet());
        toRemove.forEach(FAILED_CONNECTORS::remove);
        return FAILED_CONNECTORS;
    }

    private static synchronized Optional<Connector> findOrOpen(String device) {
        Optional<Connector> found = findByDeviceId(device);
        if (found.isPresent()) {
            Connector inQuestion = found.get();
            if (inQuestion.isClosed()) {
                ALL_CONNECTORS.remove(inQuestion);
            } else {
                return found;
            }
        }
        if (!getFailedConnectors().containsKey(device)) {
            try {
                return Optional.of(createConnector(device).get(5000, TimeUnit.MILLISECONDS));
            } catch (InterruptedException | ExecutionException e) {
                //throw new ConnectorException("Couldn't create connector " + device, e);
                LOGGER.log(Level.SEVERE, "Failed building connector " + device, e);
                getFailedConnectors()
                        .put(device, FailedConnector.failed(device));
            } catch (TimeoutException e) {
                LOGGER.log(Level.WARNING, "Timed out building connector " + device, e);
                getFailedConnectors()
                        .put(device, FailedConnector.timedOut(device));
            }
        }
        return Optional.empty();
    }

    public static Stream<Connector> allConnectors() {
        return Stream.of(SerialPortList.getPortNames())
                .filter(getConfig()::doNotIgnore)
                .map(Connectors::findOrOpen)
                .flatMap(Optional::stream);
    }

    public static Optional<Connector> getConnector(UUID deviceId) {
        return connectors(cs -> cs.filter(connector -> Objects.equals(connector.getUUID(), deviceId)))
                .findFirst();
    }

    public static Consumer<String> getCallback() {
        return callback != null ?
            callback
                :
                i -> {};
    }

    public static void setCallback(Consumer<String> callback) {
        Connectors.callback = callback;
    }

    private static ConnectorConfig getConfig() {
        ConnectorConfig connectorConfig = ConnectorConfig.load();
        LOGGER.info("Ignored devices: " + String.join(",", connectorConfig.getIgnoreDevices()));
        return connectorConfig;
    }
}
