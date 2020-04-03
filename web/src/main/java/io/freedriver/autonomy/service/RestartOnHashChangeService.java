package io.freedriver.autonomy.service;

import io.freedriver.util.crypt.CryptUtils;
import io.freedriver.util.crypt.HashAlgorithms;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.Initialized;
import javax.enterprise.event.Observes;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

@ApplicationScoped
public class RestartOnHashChangeService {
    private static final Logger LOGGER = Logger.getLogger(RestartOnHashChangeService.class.getName());

    public static final String HASH_CHANGE_PROPERTY = "restartOn";
    public static final String HASH_CHANGE_COMMAND = "restartCommand";
    public static final Duration INTERVAL = Duration.ofSeconds(1);

    private boolean continueTrying = true;
    private Instant lastModified = null;
    private String lastHash = null;

    public void init(@Observes @Initialized(ApplicationScoped.class) Object init) {
        Optional<Path> comparisonPath = Optional.of(HASH_CHANGE_PROPERTY)
                .map(System::getProperty)
                .map(Paths::get);
        Optional<String> restartCommand = Optional.of(HASH_CHANGE_COMMAND)
                .map(System::getProperty);
        if (comparisonPath.isPresent() && restartCommand.isPresent()) {
            hashChangeLoop(comparisonPath.get(), restartCommand.get());
        } else {
            LOGGER.warning(
                    (comparisonPath.isEmpty() ? "No " + HASH_CHANGE_PROPERTY + " property; " : "") +
                        (restartCommand.isEmpty() ? "No " + HASH_CHANGE_COMMAND + " property; " : "") +
                            " service disabled.");
        }
    }

    private void hashChangeLoop(Path path, String command) {
        while (continueTrying) {
            try {
                waitFor(INTERVAL);
                boolean hashChanged = checkHashChange(path);
                if (hashChanged) {
                    continueTrying = (runRestartCommand(command) == 0);
                }
            } catch (IOException | InterruptedException e) {
                LOGGER.log(Level.WARNING, "Looping error", e);
            }
        }
        System.exit(0);
    }

    private void waitFor(Duration interval) throws InterruptedException {
        Thread.sleep(interval.toMillis());
    }

    private int runRestartCommand(String command) throws IOException, InterruptedException {
        Process p = new ProcessBuilder(command).start();
        return p.waitFor();
    }

    private boolean checkHashChange(Path path) throws IOException {
        FileTime fileTime = Files.getLastModifiedTime(path);
        Instant modified = fileTime.toInstant();
        if (lastModified == null) {
            modified = lastModified;
            lastHash = hash(path);
        }
        if (modified != null && lastModified.isAfter(modified)) {
            String currentHash = hash(path);
            return !Objects.equals(lastHash, currentHash);
        }
        return false;
    }

    private String hash(Path path) {
        return CryptUtils.hashFile(path, HashAlgorithms.SHA_256);
    }

}