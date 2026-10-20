package io.freedriver.autonomy.changelog;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

class ChangelogTest {

    @Test
    void reads_the_packaged_changelog() {
        Optional<String> text = new Changelog().text();
        assertTrue(text.isPresent());
        assertTrue(text.get().startsWith("# Changelog\n"), text.get());
    }

    @Test
    void absent_resource_is_empty() {
        assertEquals(Optional.empty(), Changelog.read("changelog/ABSENT.md"));
    }

    @Test
    void resource_is_outside_the_static_web_root() {
        assertEquals("changelog/CHANGELOG.md", Changelog.RESOURCE);
        assertFalse(Changelog.RESOURCE.startsWith("META-INF/resources"));
        assertFalse(Changelog.RESOURCE.contains(".."));
    }

    @Test
    void generated_changelog_stops_at_the_current_tag() throws IOException {
        Path generated = Path.of("target/generated-changelog/CHANGELOG.md");
        Assumptions.assumeTrue(Files.isRegularFile(generated), "build produced no changelog");
        String text = Files.readString(generated);
        assertTrue(text.startsWith("# Changelog\n"), text);
        assertFalse(text.contains("## [unreleased]"), text);
    }
}
