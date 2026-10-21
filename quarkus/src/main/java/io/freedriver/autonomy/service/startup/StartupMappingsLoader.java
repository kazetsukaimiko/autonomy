package io.freedriver.autonomy.service.startup;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFileAttributeView;
import java.nio.file.attribute.PosixFileAttributes;
import java.nio.file.attribute.PosixFilePermission;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import io.freedriver.autonomy.jaxrs.ObjectMapperContextResolver;
import io.freedriver.jsonlink.config.v2.Mappings;
import io.freedriver.jsonlink.config.v2.MappingsAction;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import lombok.extern.slf4j.Slf4j;

/**
 * Reads {@code mappings_v2.json} and validates it before any startup action runs.
 * A symlink, a uid that is not this process, or a group or other write bit refuses
 * every board. A delay outside its annotations refuses only that board's list.
 */
@Slf4j
public class StartupMappingsLoader {
    private final Validator validator;
    private final Path file;
    private Loaded last = new Loaded(Map.of(), false, List.of());

    public StartupMappingsLoader(Validator validator, Path file) {
        this.validator = validator;
        this.file = file;
    }

    public Loaded last() {
        return last;
    }

    public Loaded load() {
        long uid;
        try {
            uid = expectedUid();
        } catch (IOException e) {
            return refuse("startupActions refused: process uid could not be read: " + e.getMessage());
        }
        return load(file, uid);
    }

    /** Expected owner. Production uses the process uid. */
    protected long expectedUid() throws IOException {
        return uid(Path.of("/proc", Long.toString(ProcessHandle.current().pid())));
    }

    public Loaded load(Path mappingsFile, long processUid) {
        List<String> problems = problems(mappingsFile, processUid);
        if (!problems.isEmpty()) {
            return refuse("startupActions refused: " + String.join("; ", problems));
        }
        Mappings mappings;
        try {
            mappings = ObjectMapperContextResolver.getMapper().readValue(mappingsFile.toFile(), Mappings.class);
        } catch (IOException e) {
            return refuse("startupActions refused: " + e.getMessage());
        }
        Map<String, List<MappingsAction>> actions = new LinkedHashMap<>();
        mappings.getStartupActions().forEach(actions::put);
        Map<String, List<String>> byBoard = new LinkedHashMap<>();
        boolean refuseAll = false;
        for (ConstraintViolation<Mappings> violation : validator.validate(mappings)) {
            String pathText = violation.getPropertyPath().toString();
            if (!pathText.contains("startupActions")) {
                continue;
            }
            String detail = pathText + " " + violation.getMessage();
            String board = boardKey(violation);
            if (board == null) {
                refuseAll = true;
                byBoard.computeIfAbsent("", ignored -> new ArrayList<>()).add(detail);
            } else {
                byBoard.computeIfAbsent(board, ignored -> new ArrayList<>()).add(detail);
            }
        }
        List<String> errors = new ArrayList<>();
        if (refuseAll) {
            String message = "startupActions refused: " + String.join("; ", byBoard.getOrDefault("", List.of()));
            log.error(message);
            errors.add(message);
            actions.clear();
            byBoard.remove("");
        }
        for (Map.Entry<String, List<String>> entry : byBoard.entrySet()) {
            String message = "startupActions for board '" + entry.getKey() + "' refused: "
                    + String.join("; ", entry.getValue());
            log.error(message);
            errors.add(message);
            actions.remove(entry.getKey());
        }
        last = new Loaded(Map.copyOf(actions), false, List.copyOf(errors));
        return last;
    }

    private Loaded refuse(String message) {
        log.error(message);
        last = new Loaded(Map.of(), true, List.of(message));
        return last;
    }

    static List<String> problems(Path mappingsFile, long processUid) {
        List<String> problems = new ArrayList<>();
        if (mappingsFile == null) {
            problems.add("mappings file is missing");
            return problems;
        }
        Path directory = mappingsFile.getParent();
        if (directory == null) {
            problems.add("mappings file has no directory: " + mappingsFile);
            return problems;
        }
        problems.addAll(nodeProblems(directory, "mappings directory", processUid));
        problems.addAll(nodeProblems(mappingsFile, "mappings file", processUid));
        return problems;
    }

    private static List<String> nodeProblems(Path path, String label, long processUid) {
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

    static long uid(Path path) throws IOException {
        Object value = Files.getAttribute(path, "unix:uid", LinkOption.NOFOLLOW_LINKS);
        if (!(value instanceof Number number)) {
            throw new IOException("no unix:uid for " + path);
        }
        return number.longValue();
    }

    private static String boardKey(ConstraintViolation<?> violation) {
        for (jakarta.validation.Path.Node node : violation.getPropertyPath()) {
            if (node.getKey() != null) {
                return String.valueOf(node.getKey());
            }
        }
        return null;
    }

    public record Loaded(Map<String, List<MappingsAction>> actions, boolean fileRefused, List<String> errors) {}
}
