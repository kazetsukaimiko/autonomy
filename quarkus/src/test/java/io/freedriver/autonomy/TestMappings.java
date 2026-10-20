package io.freedriver.autonomy;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * Writes the empty mappings fixture under the temporary directory.
 * Never creates or modifies {@code ~/.config/autonomy/mappings_v2.json}.
 */
public final class TestMappings {

    static final String CONFIG_KEY = "autonomy.mappings.file";
    static final String EMPTY_MAPPINGS = """
            {"eventTTL":7,"eventTTLUnit":"DAYS","mappings":[]}
            """;
    private static final String SNAPSHOT_KEY = "autonomy.test.defaultMappingsSnapshot";

    private TestMappings() {
    }

    public static Path defaultMappingsFile() {
        return Path.of(System.getProperty("user.home"), ".config", "autonomy", "mappings_v2.json");
    }

    /**
     * Records the default mappings file once, before the first fixture write.
     */
    public static void rememberDefaultFingerprint() {
        if (System.getProperty(SNAPSHOT_KEY) == null) {
            System.setProperty(SNAPSHOT_KEY, fingerprint(defaultMappingsFile()));
        }
    }

    public static void assertDefaultUntouched() {
        String expected = System.getProperty(SNAPSHOT_KEY);
        if (expected == null) {
            throw new IllegalStateException("Default mappings fingerprint was not recorded");
        }
        String actual = fingerprint(defaultMappingsFile());
        if (!expected.equals(actual)) {
            throw new AssertionError("Default mappings file changed: " + defaultMappingsFile());
        }
    }

    public static Path writeTemporary() {
        rememberDefaultFingerprint();
        try {
            Path dir = Files.createTempDirectory("autonomy-mappings");
            dir.toFile().deleteOnExit();
            Path mappings = dir.resolve("mappings_v2.json");
            Files.writeString(mappings, EMPTY_MAPPINGS);
            mappings.toFile().deleteOnExit();
            return mappings;
        } catch (IOException e) {
            throw new IllegalStateException("Could not prepare a temporary mappings file", e);
        }
    }

    static String fingerprint(Path path) {
        try {
            if (!Files.exists(path, LinkOption.NOFOLLOW_LINKS)) {
                return "absent";
            }
            BasicFileAttributes attributes = Files.readAttributes(
                    path, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
            String content = "unread";
            if (attributes.isRegularFile() || (attributes.isSymbolicLink() && Files.isRegularFile(path))) {
                content = HexFormat.of().formatHex(
                        MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(path)));
            }
            return attributes.fileKey()
                    + "|" + attributes.lastModifiedTime().toMillis()
                    + "|" + attributes.size()
                    + "|" + attributes.isSymbolicLink()
                    + "|" + content;
        } catch (IOException | NoSuchAlgorithmException e) {
            throw new IllegalStateException("Could not fingerprint " + path, e);
        }
    }
}
