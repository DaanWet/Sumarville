package Database;

import Domain.NpcMessage;
import Domain.NpcMessageType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class NpcMessageRepositoryTest {

    @Test
    void addFindByTypeAndClearSpecific(@TempDir Path dir) {
        try (Database db = new Database(dir.resolve("n.db").toString(), null)) {
            NpcMessageRepository repo = new NpcMessageRepository(db);
            repo.add("g1", NpcMessageType.BASIC, "Bob", "Hello");
            repo.add("g1", NpcMessageType.SPECIFIC, "", "Session soon");

            List<NpcMessage> basic = repo.findByType("g1", NpcMessageType.BASIC);
            assertEquals(1, basic.size());
            assertEquals("Bob", basic.get(0).npc());

            assertEquals(1, repo.findByType("g1", NpcMessageType.SPECIFIC).size());

            repo.remove(basic.get(0).id());
            assertTrue(repo.findByType("g1", NpcMessageType.BASIC).isEmpty());

            repo.clearSpecific("g1");
            assertTrue(repo.findByType("g1", NpcMessageType.SPECIFIC).isEmpty());
        }
    }
}
