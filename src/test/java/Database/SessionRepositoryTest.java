package Database;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SessionRepositoryTest {

    @Test
    void addFindRemoveAndUpcomingFilter(@TempDir Path dir) {
        try (Database db = new Database(dir.resolve("s.db").toString(), null)) {
            SessionRepository repo = new SessionRepository(db);
            LocalDateTime past = LocalDateTime.now().minusDays(10).withHour(13);
            LocalDateTime future = LocalDateTime.now().plusDays(10).withHour(13);

            repo.add("g1", past);
            repo.add("g1", future);

            // stored date-only -> returned at start of day
            List<LocalDateTime> all = repo.find("g1", true);
            assertEquals(2, all.size());
            assertEquals(future.toLocalDate().atStartOfDay(), all.get(1)); // sorted ascending

            List<LocalDateTime> upcoming = repo.find("g1", false);
            assertEquals(List.of(future.toLocalDate().atStartOfDay()), upcoming);

            repo.remove("g1", future);
            assertEquals(1, repo.find("g1", true).size());
        }
    }
}
