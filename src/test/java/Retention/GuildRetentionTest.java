package Retention;

import Database.GuildDepartureRepository;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class GuildRetentionTest {

    private static final Instant NOW = Instant.parse("2026-11-10T12:00:00Z");
    private final GuildDepartureRepository repo = mock(GuildDepartureRepository.class);
    private final GuildRetention retention = new GuildRetention(repo, Clock.fixed(NOW, ZoneOffset.UTC));

    @Test
    void reconcileMarksGuildsWithDataThatTheBotIsNoLongerIn() {
        when(repo.guildsWithData()).thenReturn(Set.of("a", "b", "c", "d", "e", "gone"));
        when(repo.departedGuilds()).thenReturn(Set.of());

        retention.reconcile(Set.of("a", "b", "c", "d", "e"), Set.of());

        verify(repo).markDeparted("gone", NOW);
        verify(repo, never()).markDeparted(eq("a"), any());
    }

    @Test
    void reconcileSkipsUnavailableGuilds() {
        when(repo.guildsWithData()).thenReturn(Set.of("a", "outage"));
        when(repo.departedGuilds()).thenReturn(Set.of());

        retention.reconcile(Set.of("a"), Set.of("outage"));

        verify(repo, never()).markDeparted(anyString(), any());
    }

    @Test
    void reconcileClearsDeparturesOfGuildsTheBotIsBackIn() {
        when(repo.guildsWithData()).thenReturn(Set.of("back"));
        when(repo.departedGuilds()).thenReturn(Set.of("back", "stillGone"));

        retention.reconcile(Set.of("back"), Set.of());

        verify(repo).clearDeparture("back");
        verify(repo, never()).clearDeparture("stillGone");
    }

    @Test
    void reconcileRefusesWhenMostGuildsSeemGone() {
        // e.g. a wrong BOT_TOKEN for another application: never schedule mass deletion.
        when(repo.guildsWithData()).thenReturn(Set.of("a", "b", "c", "d", "e", "f", "g", "h", "i", "j", "k", "l"));
        when(repo.departedGuilds()).thenReturn(Set.of());

        retention.reconcile(Set.of("a"), Set.of());

        verify(repo, never()).markDeparted(anyString(), any());
    }

    @Test
    void purgeUsesTheSevenDayGracePeriod() {
        when(repo.purgeDepartedBefore(any())).thenReturn(List.of());

        retention.purgeExpired();

        verify(repo).purgeDepartedBefore(NOW.minus(Duration.ofDays(7)));
    }

    @Test
    void leaveAndJoinUpdateTheDeparture() {
        retention.onLeft("x");
        retention.onJoined("y");

        verify(repo).markDeparted("x", NOW);
        verify(repo).clearDeparture("y");
    }
}
