package Database;

import Domain.FoodItem;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class FoodRepositoryTest {

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
