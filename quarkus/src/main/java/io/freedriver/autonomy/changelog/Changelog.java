package io.freedriver.autonomy.changelog;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;

/**
 * The changelog packaged in the app jar at {@value #RESOURCE}.
 * The Maven build writes it with git-cliff. It sits outside {@code META-INF/resources},
 * so Quarkus never serves it as a static file; {@code GET /api/changelog} is the one way to read it.
 */
@ApplicationScoped
public class Changelog {

    public static final String RESOURCE = "changelog/CHANGELOG.md";

    /** The changelog text, or empty when the jar has no changelog file. */
    public Optional<String> text() {
        return read(RESOURCE);
    }

    static Optional<String> read(String resource) {
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        if (loader == null) {
            return Optional.empty();
        }
        URL url = loader.getResource(resource);
        if (url == null) {
            return Optional.empty();
        }
        return Optional.of(readString(url));
    }

    private static String readString(URL url) {
        try (InputStream in = url.openStream()) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
