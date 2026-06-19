package Database;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DatabaseTest {

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
        }
    }
}
