package io.freedriver.autonomy.mqtt.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.List;

import org.junit.jupiter.api.Test;

class PasswordFilesTest {

    private static final String SECRET = "xK9mQ2vL7p";

    @Test
    void stripsOnlyASingleTrailingNewline() throws Exception {
        assertEquals("secret", read("secret\n", "rw-------"));
        assertEquals("secret", read("secret\r\n", "r--------"));
        assertEquals("secret\n", read("secret\n\n", "rw-------"));
        assertEquals("secret\r\n", read("secret\r\n\n", "rw-------"));
        assertEquals(" secret", read(" secret\n", "rw-------"));
        assertEquals("secret ", read("secret \n", "rw-------"));
        assertEquals("secret ", read("secret \r\n", "rw-------"));
        assertEquals("secret\r", read("secret\r", "rw-------"));
        assertEquals("secret", read("secret", "rwx------"));
    }

    @Test
    void refusesGroupOrOtherAccessWithoutExposingTheContents() throws Exception {
        for (String mode : List.of("rw-r--r--", "rw-r-----", "rw----r--", "rw--w----", "rw-rw----", "rwx--x---")) {
            Path file = file(SECRET + "\n", mode);
            PasswordFilePermissions rejected = assertThrows(PasswordFilePermissions.class, () -> PasswordFiles.read(file));
            assertEquals(file, rejected.path());
            assertFalse(rejected.getMessage().contains(SECRET));
            assertFalse(String.valueOf(rejected).contains(SECRET));
        }
    }

    private static String read(String contents, String mode) throws Exception {
        return new String(PasswordFiles.read(file(contents, mode)));
    }

    private static Path file(String contents, String mode) throws Exception {
        Path file = Files.createTempFile("mqtt-password-", ".pass");
        file.toFile().deleteOnExit();
        Files.writeString(file, contents);
        Files.setPosixFilePermissions(file, PosixFilePermissions.fromString(mode));
        return file;
    }
}
