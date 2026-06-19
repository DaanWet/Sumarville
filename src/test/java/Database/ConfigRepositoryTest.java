package Database;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class ConfigRepositoryTest {

    @Test
    void setThenGetReturnsValueAndUpserts(@TempDir Path dir) {
        try (Database db = new Database(dir.resolve("c.db").toString(), null)) {
            ConfigRepository repo = new ConfigRepository(db);
            assertEquals(Optional.empty(), repo.get("g1", "DM"));

            repo.set("g1", "DM", "111");
            assertEquals(Optional.of("111"), repo.get("g1", "DM"));

            repo.set("g1", "DM", "222"); // upsert, no duplicate row
            assertEquals(Optional.of("222"), repo.get("g1", "DM"));

            assertEquals(Optional.empty(), repo.get("g2", "DM")); // guild-scoped
        }
    }
}
