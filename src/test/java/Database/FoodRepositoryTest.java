package Database;

import Domain.FoodItem;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class FoodRepositoryTest {

    @Test
    void addRejectsDuplicateEmojiPerGuildButAllowsAcrossGuilds(@TempDir Path dir) {
        try (Database db = new Database(dir.resolve("f.db").toString(), null)) {
            FoodRepository repo = new FoodRepository(db);
            assertTrue(repo.add("g1", "Pizza", "🍕"));      // first insert succeeds
            assertFalse(repo.add("g1", "Calzone", "🍕"));    // same emoji, same guild -> rejected, no second row
            assertTrue(repo.add("g2", "Pizza", "🍕"));       // same emoji, different guild -> allowed
            List<FoodItem> g1 = repo.findAll("g1");
            assertEquals(1, g1.size());
            assertEquals("Pizza", g1.get(0).name());
        }
    }

    @Test
    void addFindAllOrderedThenRemoveById(@TempDir Path dir) {
        try (Database db = new Database(dir.resolve("f.db").toString(), null)) {
            FoodRepository repo = new FoodRepository(db);
            repo.add("g1", "Pizza", "🍕");
            repo.add("g1", "Sushi", "🍣");
            repo.add("g2", "Other", "❓");

            List<FoodItem> list = repo.findAll("g1");
            assertEquals(2, list.size());
            assertEquals("Pizza", list.get(0).name());
            assertEquals("🍣", list.get(1).emoji());

            repo.remove(list.get(0).id());
            List<FoodItem> after = repo.findAll("g1");
            assertEquals(1, after.size());
            assertEquals("Sushi", after.get(0).name());
        }
    }
}
