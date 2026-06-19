package Database;

import org.sqlite.mc.SQLiteMCSqlCipherConfig;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Owns the single, long-lived (optionally encrypted) SQLite connection, creates the
 * schema, and exposes small JDBC helpers. SQLCipher derives the key with PBKDF2 on
 * every connection open, so one long-lived connection pays that cost once; it also
 * makes SQLite serialize writes, which removes the old read-modify-write race.
 */
public class Database implements AutoCloseable {

    private final Connection conn;

    /**
     * @param path          filesystem path to the database file
     * @param encryptionKey SQLCipher passphrase; null/blank opens an unencrypted DB
     */
    public Database(String path, String encryptionKey) {
        try {
            Path parent = new File(path).getAbsoluteFile().getParentFile().toPath();
            Files.createDirectories(parent);

            String url = "jdbc:sqlite:" + path;
            if (encryptionKey != null && !encryptionKey.isBlank()) {
                this.conn = DriverManager.getConnection(
                        url,
                        SQLiteMCSqlCipherConfig.getDefault()
                                .withKey(encryptionKey)
                                .build()
                                .toProperties());
            } else {
                this.conn = DriverManager.getConnection(url);
            }

            try (Statement st = conn.createStatement()) {
                st.execute("PRAGMA journal_mode=WAL");
                st.execute("PRAGMA busy_timeout=5000");
            }
            ensureSchema();
        } catch (Exception e) {
            throw new RuntimeException("Failed to open database at " + path, e);
        }
    }

    private void ensureSchema() {
        String[] ddl = {
                "CREATE TABLE IF NOT EXISTS meta (key TEXT PRIMARY KEY, value TEXT NOT NULL)",
                "CREATE TABLE IF NOT EXISTS guild_config (" +
                        "guild_id TEXT NOT NULL, config_key TEXT NOT NULL, config_value TEXT NOT NULL, " +
                        "PRIMARY KEY (guild_id, config_key))",
                "CREATE TABLE IF NOT EXISTS sessions (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT, guild_id TEXT NOT NULL, session_date TEXT NOT NULL)",
                "CREATE TABLE IF NOT EXISTS food (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT, guild_id TEXT NOT NULL, name TEXT NOT NULL, emoji TEXT NOT NULL)",
                "CREATE TABLE IF NOT EXISTS characters (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT, guild_id TEXT NOT NULL, user_id TEXT, name TEXT NOT NULL, picture TEXT)",
                "CREATE TABLE IF NOT EXISTS npc_messages (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT, guild_id TEXT NOT NULL, type TEXT NOT NULL, npc TEXT, message TEXT NOT NULL)",
                "CREATE INDEX IF NOT EXISTS idx_sessions_guild ON sessions(guild_id)",
                "CREATE INDEX IF NOT EXISTS idx_food_guild ON food(guild_id)",
                "CREATE INDEX IF NOT EXISTS idx_characters_guild ON characters(guild_id)",
                "CREATE INDEX IF NOT EXISTS idx_npc_messages_guild ON npc_messages(guild_id)"
        };
        try (Statement st = conn.createStatement()) {
            for (String sql : ddl) {
                st.execute(sql);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to create schema", e);
        }
    }

    public <T> List<T> query(String sql, RowMapper<T> mapper, Object... params) {
        synchronized (conn) {
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                bind(ps, params);
                try (ResultSet rs = ps.executeQuery()) {
                    List<T> out = new ArrayList<>();
                    while (rs.next()) {
                        out.add(mapper.map(rs));
                    }
                    return out;
                }
            } catch (SQLException e) {
                throw new RuntimeException("Query failed: " + sql, e);
            }
        }
    }

    public int update(String sql, Object... params) {
        synchronized (conn) {
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                bind(ps, params);
                return ps.executeUpdate();
            } catch (SQLException e) {
                throw new RuntimeException("Update failed: " + sql, e);
            }
        }
    }

    public long insert(String sql, Object... params) {
        synchronized (conn) {
            try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                bind(ps, params);
                ps.executeUpdate();
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    return keys.next() ? keys.getLong(1) : -1;
                }
            } catch (SQLException e) {
                throw new RuntimeException("Insert failed: " + sql, e);
            }
        }
    }

    /** Runs {@code work} inside a transaction; any RuntimeException rolls it back. */
    public void runInTransaction(Runnable work) {
        synchronized (conn) {
            try {
                conn.setAutoCommit(false);
                work.run();
                conn.commit();
            } catch (RuntimeException e) {
                try {
                    conn.rollback();
                } catch (SQLException ex) {
                    e.addSuppressed(ex);
                }
                throw e;
            } catch (SQLException e) {
                throw new RuntimeException("Transaction failed", e);
            } finally {
                try {
                    conn.setAutoCommit(true);
                } catch (SQLException ignored) {
                }
            }
        }
    }

    private static void bind(PreparedStatement ps, Object... params) throws SQLException {
        for (int i = 0; i < params.length; i++) {
            ps.setObject(i + 1, params[i]);
        }
    }

    @Override
    public void close() {
        try {
            conn.close();
        } catch (SQLException ignored) {
        }
    }
}
