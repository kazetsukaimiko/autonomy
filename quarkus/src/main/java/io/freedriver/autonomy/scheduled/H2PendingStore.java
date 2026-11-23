package io.freedriver.autonomy.scheduled;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import io.freedriver.autonomy.config.ConfigFileTrust;
import io.freedriver.autonomy.mqtt.MqttLog;
import org.flywaydb.core.Flyway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * File H2 store opened only after {@link ConfigFileTrust} accepts the directory
 * and any H2 files already in it. Applied list ids are kept for {@link #SEEN_FOR}.
 */
public final class H2PendingStore implements PendingStore, AutoCloseable {

    public static final Duration SEEN_FOR = Duration.ofSeconds(40);

    private static final Logger LOG = LoggerFactory.getLogger(MqttLog.CATEGORY);

    private final Connection connection;

    private H2PendingStore(Connection connection) {
        this.connection = connection;
    }

    public static Optional<H2PendingStore> open(Path directory) {
        try {
            long uid = ConfigFileTrust.processUid();
            if (!Files.exists(directory)) {
                Files.createDirectories(directory);
                Files.setPosixFilePermissions(directory, EnumSet.of(
                        PosixFilePermission.OWNER_READ,
                        PosixFilePermission.OWNER_WRITE,
                        PosixFilePermission.OWNER_EXECUTE));
            }
            List<String> problems = new ArrayList<>(ConfigFileTrust.nodeProblems(directory, "scheduled store", uid));
            if (Files.isDirectory(directory)) {
                try (var files = Files.list(directory)) {
                    files.forEach(file -> problems.addAll(
                            ConfigFileTrust.nodeProblems(file, "scheduled store file", uid)));
                }
            }
            if (!problems.isEmpty()) {
                LOG.info("command-lists not subscribed reason=scheduled store unavailable detail={}",
                        problems.get(0));
                return Optional.empty();
            }
            Path database = directory.resolve("scheduled");
            String url = "jdbc:h2:file:" + database.toAbsolutePath() + ";DB_CLOSE_ON_EXIT=FALSE";
            Flyway.configure()
                    .dataSource(url, "sa", "")
                    .locations("classpath:db/scheduled")
                    .load()
                    .migrate();
            Connection connection = DriverManager.getConnection(url, "sa", "");
            connection.setAutoCommit(true);
            return Optional.of(new H2PendingStore(connection));
        } catch (Exception e) {
            LOG.info("command-lists not subscribed reason=scheduled store unavailable causeClass={}",
                    e.getClass().getName());
            return Optional.empty();
        }
    }

    @Override
    public List<PendingRow> pending() {
        String sql = "select id, target, state, due_at from pending_command order by due_at, id";
        try (PreparedStatement statement = connection.prepareStatement(sql);
                ResultSet rows = statement.executeQuery()) {
            List<PendingRow> pending = new ArrayList<>();
            while (rows.next()) {
                pending.add(read(rows));
            }
            return List.copyOf(pending);
        } catch (SQLException e) {
            throw new IllegalStateException("pending read failed", e);
        }
    }

    @Override
    public Optional<PendingRow> find(UUID id) {
        String sql = "select id, target, state, due_at from pending_command where id = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, id.toString());
            try (ResultSet rows = statement.executeQuery()) {
                if (!rows.next()) {
                    return Optional.empty();
                }
                return Optional.of(read(rows));
            }
        } catch (SQLException e) {
            throw new IllegalStateException("pending read failed", e);
        }
    }

    @Override
    public boolean seen(UUID listId, Instant now) {
        purgeSeenBefore(now.minus(SEEN_FOR));
        String sql = "select 1 from applied_list where list_id = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, listId.toString());
            try (ResultSet rows = statement.executeQuery()) {
                return rows.next();
            }
        } catch (SQLException e) {
            throw new IllegalStateException("applied list read failed", e);
        }
    }

    @Override
    public void admit(UUID listId, Instant seenAt, Set<UUID> cancelIds, Set<String> cancelTargets,
            List<PendingRow> added) {
        try {
            connection.setAutoCommit(false);
            try (PreparedStatement byId = connection.prepareStatement("delete from pending_command where id = ?");
                    PreparedStatement byTarget = connection
                            .prepareStatement("delete from pending_command where target = ?");
                    PreparedStatement insert = connection.prepareStatement(
                            "insert into pending_command (id, target, state, due_at) values (?, ?, ?, ?)");
                    PreparedStatement seen = connection.prepareStatement(
                            "insert into applied_list (list_id, seen_at) values (?, ?)")) {
                for (UUID id : cancelIds) {
                    byId.setString(1, id.toString());
                    byId.addBatch();
                }
                byId.executeBatch();
                for (String target : cancelTargets) {
                    byTarget.setString(1, target);
                    byTarget.addBatch();
                }
                byTarget.executeBatch();
                for (PendingRow row : added) {
                    insert.setString(1, row.id().toString());
                    insert.setString(2, row.target());
                    insert.setBoolean(3, row.on());
                    insert.setLong(4, row.dueAt().toEpochMilli());
                    insert.addBatch();
                }
                insert.executeBatch();
                seen.setString(1, listId.toString());
                seen.setLong(2, seenAt.toEpochMilli());
                seen.executeUpdate();
            }
            connection.commit();
        } catch (SQLException e) {
            rollback();
            throw new IllegalStateException("pending admit failed", e);
        } finally {
            enableAutoCommit();
        }
    }

    @Override
    public void delete(UUID id) {
        try (PreparedStatement statement = connection.prepareStatement("delete from pending_command where id = ?")) {
            statement.setString(1, id.toString());
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("pending delete failed", e);
        }
    }

    @Override
    public void purgeSeenBefore(Instant cutoff) {
        try (PreparedStatement statement = connection
                .prepareStatement("delete from applied_list where seen_at < ?")) {
            statement.setLong(1, cutoff.toEpochMilli());
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("applied list purge failed", e);
        }
    }

    @Override
    public void close() {
        try {
            if (!connection.isClosed()) {
                try (Statement statement = connection.createStatement()) {
                    statement.execute("shutdown");
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException("scheduled store close failed", e);
        }
    }

    private void rollback() {
        try {
            connection.rollback();
        } catch (SQLException e) {
            throw new IllegalStateException("pending rollback failed", e);
        }
    }

    private void enableAutoCommit() {
        try {
            connection.setAutoCommit(true);
        } catch (SQLException e) {
            throw new IllegalStateException("pending autocommit failed", e);
        }
    }

    private static PendingRow read(ResultSet rows) throws SQLException {
        return new PendingRow(
                UUID.fromString(rows.getString("id")),
                rows.getString("target"),
                rows.getBoolean("state"),
                Instant.ofEpochMilli(rows.getLong("due_at")));
    }
}
