package Database;

import Domain.CharacterSheet;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class CharacterRepositoryTest {

    @Test
    void playerVsNpcFindAndEdit(@TempDir Path dir) {
        try (Database db = new Database(dir.resolve("ch.db").toString(), null)) {
            CharacterRepository repo = new CharacterRepository(db);
            repo.add("g1", "user-1", "Aragorn", "pic1");
            repo.add("g1", null, "Goblin", "");        // NPC

            Optional<CharacterSheet> byName = repo.findByName("g1", "aragorn"); // case-insensitive
            assertTrue(byName.isPresent());
            assertEquals("user-1", byName.get().userId());
            assertFalse(byName.get().isNpc());

            assertTrue(repo.findByUserId("g1", "user-1").isPresent());

            List<CharacterSheet> npcs = repo.findAll("g1", true);
            assertEquals(1, npcs.size());
            assertEquals("Goblin", npcs.get(0).name());
            assertTrue(npcs.get(0).isNpc());

            assertEquals(2, repo.findAll("g1", false).size());

            long aragornId = byName.get().id();
            repo.editAttribute(aragornId, "name", "Strider");
            assertTrue(repo.findByName("g1", "Strider").isPresent());

            repo.remove(aragornId);
            assertTrue(repo.findByName("g1", "Strider").isEmpty());
        }
    }
}
