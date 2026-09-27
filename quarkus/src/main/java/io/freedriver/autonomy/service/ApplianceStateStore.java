package io.freedriver.autonomy.service;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.PosixFileAttributes;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.nio.file.attribute.UserPrincipal;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * Saves the last on or off state of every appliance under {@code autonomy.state.dir}.
 *
 * <p>The directory is created mode 0700 and the file mode 0600, owned by the
 * process user. Missing parents keep the default permissions. Each write goes to
 * a temp file in that directory, is forced to disk, and is renamed over the
 * previous file. Load, update, and write of one change run together so two
 * overlapping saves cannot drop each other. A directory that is not a real
 * directory owned by this user, or a state file that is not a regular
 * non-symlink file owned by this user without group or other write, is refused:
 * nothing is restored and nothing is written.
 */
@ApplicationScoped
@Slf4j
public class ApplianceStateStore {

    static final String FILE_NAME = "appliance-state.json";
    static final String CORRUPT_FILE_NAME = "appliance-state.json.corrupt";

    private static final Set<PosixFilePermission> DIRECTORY_PERMISSIONS = Set.of(
            PosixFilePermission.OWNER_READ,
            PosixFilePermission.OWNER_WRITE,
            PosixFilePermission.OWNER_EXECUTE);

    private static final Set<PosixFilePermission> FILE_PERMISSIONS = Set.of(
            PosixFilePermission.OWNER_READ,
            PosixFilePermission.OWNER_WRITE);

    private final Path directory;
    private final ObjectMapper mapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);

    @Inject
    public ApplianceStateStore(@ConfigProperty(name = "autonomy.state.dir") String stateDir) {
        this(Path.of(stateDir));
    }

    ApplianceStateStore(Path directory) {
        this.directory = directory;
    }

    /**
     * Reads the state file. A missing file is a first start. A file that cannot
     * be parsed at all is logged and treated the same way. An entry that is not
     * a boolean is skipped and logged; the other entries are kept.
     */
    public synchronized LoadedApplianceState load() {
        if (!ensureTrusted()) {
            return LoadedApplianceState.untrusted();
        }
        return readState();
    }

    private LoadedApplianceState readState() {
        Path file = stateFile();
        if (!Files.isRegularFile(file, LinkOption.NOFOLLOW_LINKS)) {
            return LoadedApplianceState.missing();
        }
        try {
            JsonNode root = mapper.readTree(file.toFile());
            if (root == null || !root.isObject()) {
                log.warn("Couldn't read appliance state file {}; treating it as no saved state", file);
                return LoadedApplianceState.unreadable();
            }
            Map<String, Boolean> states = new LinkedHashMap<>();
            Iterator<Map.Entry<String, JsonNode>> fields = root.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> field = fields.next();
                JsonNode value = field.getValue();
                if (value != null && value.isBoolean()) {
                    states.put(field.getKey(), value.booleanValue());
                } else {
                    log.warn("Skipping appliance state entry '{}' because it does not parse", field.getKey());
                }
            }
            return LoadedApplianceState.present(Map.copyOf(states));
        } catch (Exception e) {
            log.warn("Couldn't read appliance state file {}; treating it as no saved state", file, e);
            return LoadedApplianceState.unreadable();
        }
    }

    /**
     * Replaces the saved on/off value for each named appliance and leaves the others.
     */
    public synchronized void merge(Map<String, Boolean> updates) throws IOException {
        if (updates.isEmpty() || !ensureTrusted()) {
            return;
        }
        LoadedApplianceState loaded = readState();
        Map<String, Boolean> states = new LinkedHashMap<>(loaded.states());
        states.putAll(updates);
        writeTrusted(states, loaded);
    }

    /**
     * Writes every saved state. The temp file is removed if the rename does not finish.
     * An existing file that cannot be read is moved aside before the new file is written.
     */
    public synchronized void write(Map<String, Boolean> states) throws IOException {
        if (!ensureTrusted()) {
            return;
        }
        writeTrusted(states, readState());
    }

    private void writeTrusted(Map<String, Boolean> states, LoadedApplianceState existing) throws IOException {
        ensureDirectory();
        if (existing.status() == LoadedApplianceState.Status.UNREADABLE) {
            moveUnreadableAside();
        }
        Path target = stateFile();
        Path temp = directory.resolve(".appliance-state-" + UUID.randomUUID() + ".tmp");
        try {
            createPrivateFile(temp);
            mapper.writeValue(temp.toFile(), states);
            forceFile(temp);
            moveIntoPlace(temp, target);
            forceDirectory();
        } finally {
            try {
                Files.deleteIfExists(temp);
            } catch (IOException cleanup) {
                log.warn("Couldn't remove appliance state temp file {}", temp, cleanup);
            }
        }
    }

    Path stateFile() {
        return directory.resolve(FILE_NAME);
    }

    void moveIntoPlace(Path temp, Path target) throws IOException {
        Files.move(temp, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
    }

    private void moveUnreadableAside() throws IOException {
        Path file = stateFile();
        Path corrupt = directory.resolve(CORRUPT_FILE_NAME);
        Files.move(file, corrupt, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        log.warn("Moved unreadable appliance state file {} aside to {}", file, corrupt.getFileName());
    }

    private static void forceFile(Path temp) throws IOException {
        try (FileChannel channel = FileChannel.open(temp, StandardOpenOption.WRITE)) {
            channel.force(true);
        }
    }

    private void forceDirectory() {
        try (FileChannel channel = FileChannel.open(directory, StandardOpenOption.READ)) {
            channel.force(true);
        } catch (IOException | UnsupportedOperationException | IllegalStateException e) {
            log.debug("Couldn't force appliance state directory {} to disk", directory, e);
        }
    }

    private void createPrivateFile(Path temp) throws IOException {
        if (posix()) {
            Files.createFile(temp, PosixFilePermissions.asFileAttribute(FILE_PERMISSIONS));
            return;
        }
        Files.createFile(temp);
    }

    /**
     * The directory, when it already exists, is a real directory owned by this
     * user. The state file, when it already exists, is a regular non-symlink
     * file owned by this user with no group or other write bit.
     */
    private boolean ensureTrusted() {
        if (!posix()) {
            return true;
        }
        try {
            String problem = trustProblem();
            if (problem == null) {
                return true;
            }
            log.error("Refusing appliance state at {}: {}", directory, problem);
            return false;
        } catch (IOException e) {
            log.error("Refusing appliance state at {}", directory, e);
            return false;
        }
    }

    private String trustProblem() throws IOException {
        if (Files.exists(directory, LinkOption.NOFOLLOW_LINKS)) {
            PosixFileAttributes directoryAttributes = Files.readAttributes(
                    directory, PosixFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
            if (directoryAttributes.isSymbolicLink()
                    || !directoryAttributes.isDirectory()
                    || !ownedByProcessUser(directoryAttributes.owner())) {
                return "directory is not a real directory owned by this user";
            }
        }
        Path file = stateFile();
        if (!Files.exists(file, LinkOption.NOFOLLOW_LINKS)) {
            return null;
        }
        PosixFileAttributes fileAttributes = Files.readAttributes(
                file, PosixFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
        if (fileAttributes.isSymbolicLink()
                || !fileAttributes.isRegularFile()
                || !ownedByProcessUser(fileAttributes.owner())) {
            return "file is not a regular file owned by this user";
        }
        Set<PosixFilePermission> permissions = fileAttributes.permissions();
        if (permissions.contains(PosixFilePermission.GROUP_WRITE)
                || permissions.contains(PosixFilePermission.OTHERS_WRITE)) {
            return "file is group or other writable";
        }
        return null;
    }

    private boolean ownedByProcessUser(UserPrincipal owner) throws IOException {
        String userName = System.getProperty("user.name");
        if (userName != null && userName.equals(owner.getName())) {
            return true;
        }
        UserPrincipal current = directory.getFileSystem()
                .getUserPrincipalLookupService()
                .lookupPrincipalByName(userName);
        return owner.equals(current);
    }

    private void ensureDirectory() throws IOException {
        if (!Files.isDirectory(directory)) {
            Path parent = directory.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            try {
                if (posix()) {
                    Files.createDirectory(directory, PosixFilePermissions.asFileAttribute(DIRECTORY_PERMISSIONS));
                } else {
                    Files.createDirectory(directory);
                }
            } catch (FileAlreadyExistsException alreadyExists) {
                if (!Files.isDirectory(directory)) {
                    throw new IOException("Appliance state path is not a directory: " + directory, alreadyExists);
                }
            }
        }
        if (!posix()) {
            return;
        }
        Set<PosixFilePermission> current = Files.getPosixFilePermissions(directory);
        if (current.equals(DIRECTORY_PERMISSIONS)) {
            return;
        }
        if (groupOrOtherCanAccess(current)) {
            log.warn(
                    "Appliance state directory {} is mode {}; tightening it to 0700",
                    directory,
                    PosixFilePermissions.toString(current));
        }
        Files.setPosixFilePermissions(directory, DIRECTORY_PERMISSIONS);
    }

    private boolean posix() {
        return directory.getFileSystem().supportedFileAttributeViews().contains("posix");
    }

    private static boolean groupOrOtherCanAccess(Set<PosixFilePermission> permissions) {
        return permissions.stream().anyMatch(permission ->
                permission.name().startsWith("GROUP_") || permission.name().startsWith("OTHERS_"));
    }
}
