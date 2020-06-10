package io.freedriver.jsonlink;

import io.freedriver.jsonlink.config.ConnectorConfig;
import jssc.SerialPort;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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
    private static final String LINUX_SERIAL_BY_ID_PATH = "/dev/serial/by-id/";
    private static final Set<String> CONNECTOR_MATCHES = Stream.of("Arduino").collect(Collectors.toSet());

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

    public static synchronized CompletableFuture<Optional<Connector>> findOrOpen(
            String device, ExecutorService pool) {
        return CompletableFuture
                .supplyAsync(() -> {
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
                    return Optional.of(createConnector(device).get(2000, TimeUnit.MILLISECONDS));
                } catch (InterruptedException | ExecutionException e) {
                    //throw new ConnectorException("Couldn't create connector " + device, e);
                    LOGGER.log(Level.SEVERE, "Failed building connector " + device, e);
                    getFailedConnectors()
                            .put(device, FailedConnector.failed(device));
                } catch (TimeoutException e) {
                    LOGGER.log(Level.WARNING, "Timed out building connector " + device);
                    getFailedConnectors()
                            .put(device, FailedConnector.timedOut(device));
                }
            }
            return Optional.empty();
        }, pool);
    }

    public static synchronized CompletableFuture<Void> findOrOpenAndConsume(
            String device, ExecutorService pool, Consumer<Connector> onCompletion) {
        return findOrOpen(device, pool)
                .thenAccept(optional -> optional
                        .ifPresent(onCompletion));
    }


    public static Stream<String> allDevices() {
        if (Files.isDirectory(Paths.get(LINUX_SERIAL_BY_ID_PATH))) {
            try (Stream<Path> serialDevices = Files.list(Paths.get(LINUX_SERIAL_BY_ID_PATH))) {
                List<Path> deviceList = serialDevices.collect(Collectors.toList());
                return deviceList
                        .stream()
                        .filter(Connectors::match)
                        .map(Path::toAbsolutePath)
                        .map(Path::toString)
                        .filter(getConfig()::doNotIgnore); // TODO : Filter based on symlinkage too
            } catch (IOException e) {
                LOGGER.log(Level.WARNING, "Couldn't scan " + LINUX_SERIAL_BY_ID_PATH, e);
            }
        }
        return Stream.empty();
    }

    public static boolean match(Path path) {
        return CONNECTOR_MATCHES.stream()
            .anyMatch(path.toString()::contains);
    }

    public static boolean noMatch(Path path) {
        return !match(path);
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
        LOGGER.finest("Ignored devices: " + String.join(",", connectorConfig.getIgnoreDevices()));
        return connectorConfig;
    }
}
