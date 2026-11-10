package Database;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class GuildDepartureRepositoryTest {

    private static final Instant T0 = Instant.parse("2026-11-01T12:00:00Z");

    private static void seed(Repositories repos, String guildId) {
        repos.config().set(guildId, "CalendarChannel", "123");
        repos.sessions().add(guildId, java.time.LocalDateTime.of(2026, 12, 1, 19, 0));
        repos.food().add(guildId, "Pizza", "🍕");
        repos.characters().add(guildId, "u1", "Aragorn", null);
        repos.npcMessages().add(guildId, Domain.NpcMessageType.BASIC, "Gandalf", "Hello");
    }

    @Test
    void guildsWithDataCoversEveryTable(@TempDir Path dir) {
        try (Database db = new Database(dir.resolve("g.db").toString(), null)) {
            Repositories repos = new Repositories(db);
            repos.config().set("c", "k", "v");
            repos.sessions().add("s", java.time.LocalDateTime.of(2026, 12, 1, 19, 0));
            repos.food().add("f", "Pizza", "🍕");
            repos.characters().add("ch", "u1", "Aragorn", null);
            repos.npcMessages().add("n", Domain.NpcMessageType.BASIC, "Gandalf", "Hello");

            assertEquals(Set.of("c", "s", "f", "ch", "n"), repos.departures().guildsWithData());
        }
    }

    @Test
    void markDepartedKeepsTheFirstDate(@TempDir Path dir) {
        try (Database db = new Database(dir.resolve("g.db").toString(), null)) {
            GuildDepartureRepository repo = new GuildDepartureRepository(db);
            repo.markDeparted("g1", T0);
            repo.markDeparted("g1", T0.plusSeconds(3600));

            assertEquals(Set.of("g1"), repo.departedGuilds());
            // Still purged by a cutoff just after T0: the later mark did not reset the clock.
            assertEquals(List.of("g1"), repo.purgeDepartedBefore(T0.plusSeconds(1)));
        }
    }

    @Test
    void clearDepartureCancelsThePurge(@TempDir Path dir) {
        try (Database db = new Database(dir.resolve("g.db").toString(), null)) {
            Repositories repos = new Repositories(db);
            seed(repos, "g1");
            repos.departures().markDeparted("g1", T0);
            repos.departures().clearDeparture("g1");

            assertEquals(List.of(), repos.departures().purgeDepartedBefore(T0.plusSeconds(86_400 * 30)));
            assertEquals(Set.of("g1"), repos.departures().guildsWithData());
        }
    }

    @Test
    void purgeDeletesAllDataOfExpiredGuildsOnly(@TempDir Path dir) {
        try (Database db = new Database(dir.resolve("g.db").toString(), null)) {
            Repositories repos = new Repositories(db);
            seed(repos, "old");
            seed(repos, "recent");
            seed(repos, "stays");
            repos.departures().markDeparted("old", T0);
            repos.departures().markDeparted("recent", T0.plusSeconds(86_400 * 6));

            List<String> purged = repos.departures().purgeDepartedBefore(T0.plusSeconds(86_400 * 5));

            assertEquals(List.of("old"), purged);
            assertEquals(Set.of("recent", "stays"), repos.departures().guildsWithData());
            assertEquals(Set.of("recent"), repos.departures().departedGuilds());
            assertTrue(repos.food().findAll("old").isEmpty());
            assertFalse(repos.food().findAll("recent").isEmpty());
        }
    }
}
