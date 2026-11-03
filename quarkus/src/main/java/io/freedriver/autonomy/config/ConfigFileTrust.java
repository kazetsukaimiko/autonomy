package io.freedriver.autonomy.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFileAttributeView;
import java.nio.file.attribute.PosixFileAttributes;
import java.nio.file.attribute.PosixFilePermission;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Symlink, owner, and group/other-write checks for mappings.d and connections.d.
 */
public final class ConfigFileTrust {
    private ConfigFileTrust() {}

    public static long processUid() throws IOException {
        return uid(Path.of("/proc", Long.toString(ProcessHandle.current().pid())));
    }

    public static long uid(Path path) throws IOException {
        Object value = Files.getAttribute(path, "unix:uid", LinkOption.NOFOLLOW_LINKS);
        if (!(value instanceof Number number)) {
            throw new IOException("no unix:uid for " + path);
        }
        return number.longValue();
    }

    public static List<String> nodeProblems(Path path, String label, long processUid) {
        List<String> problems = new ArrayList<>();
        try {
            if (Files.isSymbolicLink(path)) {
                problems.add(label + " is a symlink: " + path);
                return problems;
            }
            if (!Files.exists(path, LinkOption.NOFOLLOW_LINKS)) {
                problems.add(label + " does not exist: " + path);
                return problems;
            }
            long owner = uid(path);
            if (owner != processUid) {
                problems.add(label + " owner uid " + owner + " is not process uid " + processUid);
            }
            PosixFileAttributes attributes = Files.getFileAttributeView(
                            path, PosixFileAttributeView.class, LinkOption.NOFOLLOW_LINKS)
                    .readAttributes();
            Set<PosixFilePermission> permissions = attributes.permissions();
            if (permissions.contains(PosixFilePermission.GROUP_WRITE)) {
                problems.add(label + " is writable by group: " + path);
            }
            if (permissions.contains(PosixFilePermission.OTHERS_WRITE)) {
                problems.add(label + " is writable by other: " + path);
            }
        } catch (IOException e) {
            problems.add(label + " could not be checked: " + path + " (" + e.getMessage() + ")");
        }
        return problems;
    }
}
