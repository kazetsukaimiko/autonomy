package io.freedriver.autonomy.mqtt.impl;

import java.nio.file.Path;

/**
 * The password file is not owner-only. The message is a fixed cause name;
 * the path is logged separately and the contents are never included.
 */
final class PasswordFilePermissions extends Exception {

    private final Path path;

    PasswordFilePermissions(Path path) {
        super("password file is readable or writable by group or others");
        this.path = path;
    }

    Path path() {
        return path;
    }
}
