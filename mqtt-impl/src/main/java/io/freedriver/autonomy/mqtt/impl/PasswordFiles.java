package io.freedriver.autonomy.mqtt.impl;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.util.EnumSet;
import java.util.Set;

/**
 * Reads the MQTT password. Permissions are checked before the bytes are read.
 * Only one trailing newline is removed; every other character is kept.
 */
final class PasswordFiles {

    private static final Set<PosixFilePermission> GROUP_OR_OTHER = EnumSet.of(
            PosixFilePermission.GROUP_READ,
            PosixFilePermission.GROUP_WRITE,
            PosixFilePermission.GROUP_EXECUTE,
            PosixFilePermission.OTHERS_READ,
            PosixFilePermission.OTHERS_WRITE,
            PosixFilePermission.OTHERS_EXECUTE);

    private PasswordFiles() {
    }

    static char[] read(Path file) throws ConfigFileException, PasswordFilePermissions {
        if (!Files.isRegularFile(file)) {
            throw new ConfigFileException("password file unreadable");
        }
        if (!ownerOnly(file)) {
            throw new PasswordFilePermissions(file);
        }
        final String raw;
        try {
            raw = Files.readString(file);
        } catch (IOException e) {
            throw new ConfigFileException("password file unreadable");
        }
        String password = stripSingleTrailingNewline(raw);
        if (password.isEmpty()) {
            throw new ConfigFileException("password file unreadable");
        }
        return password.toCharArray();
    }

    /**
     * Owner-only means no group or other permission bits. 600 and 400 pass.
     * A file whose permissions cannot be read is not treated as owner-only.
     */
    static boolean ownerOnly(Path file) {
        try {
            Set<PosixFilePermission> permissions = Files.getPosixFilePermissions(file);
            return permissions.stream().noneMatch(GROUP_OR_OTHER::contains);
        } catch (IOException | UnsupportedOperationException e) {
            return false;
        }
    }

    static String stripSingleTrailingNewline(String raw) {
        if (raw.endsWith("\r\n")) {
            return raw.substring(0, raw.length() - 2);
        }
        if (raw.endsWith("\n")) {
            return raw.substring(0, raw.length() - 1);
        }
        return raw;
    }
}
