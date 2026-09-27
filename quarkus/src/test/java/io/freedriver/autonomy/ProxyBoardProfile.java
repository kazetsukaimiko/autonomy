package io.freedriver.autonomy;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;

import io.freedriver.autonomy.service.ProxyFakeConnectorService;
import io.quarkus.test.junit.QuarkusTestProfile;

public class ProxyBoardProfile implements QuarkusTestProfile {

    static final Path STATE_DIR = createStateDir();

    private static Path createStateDir() {
        try {
            return Files.createTempDirectory("autonomy-proxy-state");
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public Map<String, String> getConfigOverrides() {
        return Map.of("autonomy.state.dir", STATE_DIR.toString());
    }

    @Override
    public Set<Class<?>> getEnabledAlternatives() {
        return Set.of(ProxyFakeConnectorService.class);
    }
}
