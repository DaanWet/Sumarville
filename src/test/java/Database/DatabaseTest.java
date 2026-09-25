package Database;

import ch.qos.logback.classic.Level;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DatabaseTest {

    /** Every unencrypted SQLite file starts with these 16 bytes; a SQLCipher file starts with a random salt. */
    private static final String PLAIN_SQLITE_HEADER = "SQLite format 3\0";

    @Test
    void openWithoutKeyRefusesAndCreatesNoFile(@TempDir Path dir) {
        Path file = dir.resolve("data").resolve("prod.db");

        IllegalStateException e = assertThrows(IllegalStateException.class,
                () -> Database.open(file.toString(), null, false));

        assertTrue(e.getMessage().contains("DB_ENCRYPTION_KEY"));
        assertThrows(IllegalStateException.class, () -> Database.open(file.toString(), "", false));
        assertFalse(Files.exists(file));
    }

    @Test
    void openWithUnencryptedAllowedCreatesPlainDb(@TempDir Path dir) throws IOException {
        Path file = dir.resolve("dev.db");

        Database.open(file.toString(), null, true).close();

        assertEquals(PLAIN_SQLITE_HEADER, header(file));
    }

    @Test
    void openWithUnencryptedAllowedLogsAWarning(@TempDir Path dir) {
        try (LogCapture logs = new LogCapture(Database.class)) {
            Database.open(dir.resolve("dev.db").toString(), null, true).close();

            assertEquals(1, logs.messagesAt(Level.WARN).size());
        }
    }

    @Test
    void openWithKeyEncryptsTheFileOnDisk(@TempDir Path dir) throws IOException {
        Path file = dir.resolve("prod.db");

        Database.open(file.toString(), "test-passphrase", false).close();

        assertNotEquals(PLAIN_SQLITE_HEADER, header(file));
    }

    @Test
    void schemaIsCreatedAndHelpersRoundTrip(@TempDir Path dir) {
        try (Database db = new Database(dir.resolve("test.db").toString(), null)) {
            long id = db.insert("INSERT INTO food(guild_id, name, emoji) VALUES(?,?,?)", "g1", "Pizza", "🍕");
            assertTrue(id > 0);

            List<String> names = db.query(
                    "SELECT name FROM food WHERE guild_id = ?",
                    rs -> rs.getString(1), "g1");
            assertEquals(List.of("Pizza"), names);

            int changed = db.update("DELETE FROM food WHERE id = ?", id);
            assertEquals(1, changed);
        }
    }

    @Test
    void transactionRollsBackOnFailure(@TempDir Path dir) {
        try (Database db = new Database(dir.resolve("tx.db").toString(), null)) {
            assertThrows(RuntimeException.class, () -> db.runInTransaction(() -> {
                db.insert("INSERT INTO food(guild_id, name, emoji) VALUES(?,?,?)", "g1", "A", "a");
                throw new RuntimeException("boom");
            }));
            List<String> names = db.query("SELECT name FROM food WHERE guild_id = ?", rs -> rs.getString(1), "g1");
            assertTrue(names.isEmpty());
            // autoCommit must be restored after rollback, else later writes silently never persist
            long id2 = db.insert("INSERT INTO food(guild_id, name, emoji) VALUES(?,?,?)", "g1", "B", "b");
            assertTrue(id2 > 0);
            List<String> after = db.query("SELECT name FROM food WHERE guild_id = ?", rs -> rs.getString(1), "g1");
            assertEquals(List.of("B"), after);
        }
    }

    private static String header(Path file) throws IOException {
        try (InputStream in = Files.newInputStream(file)) {
            byte[] bytes = in.readNBytes(16);
            assertEquals(16, bytes.length, "database file is shorter than a SQLite header");
            return new String(bytes, StandardCharsets.US_ASCII);
        }
    }
}
