package Retention;

import Database.GuildDepartureRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Deletes a server's data {@link #GRACE_PERIOD} after the bot was removed from it. The grace
 * period lets an admin re-invite the bot without losing anything, and absorbs Discord outages
 * in which servers briefly look gone. The privacy policy (§6) promises this behaviour.
 */
public class GuildRetention {

    public static final Duration GRACE_PERIOD = Duration.ofDays(7);

    /** Below this many missing guilds the half-gone safety check does not apply. */
    private static final int MIN_SUSPICIOUS = 5;

    private static final Logger LOG = LoggerFactory.getLogger(GuildRetention.class);

    private final GuildDepartureRepository repo;
    private final Clock clock;

    public GuildRetention(GuildDepartureRepository repo, Clock clock) {
        this.repo = repo;
        this.clock = clock;
    }

    public void onLeft(String guildId) {
        repo.markDeparted(guildId, clock.instant());
        LOG.info("Left guild {}: its data will be deleted after {} days unless the bot is re-added",
                guildId, GRACE_PERIOD.toDays());
    }

    public void onJoined(String guildId) {
        repo.clearDeparture(guildId);
    }

    /**
     * Catches up on joins and leaves that happened while the bot was offline.
     *
     * @param present     guilds the bot is in now
     * @param unavailable guilds Discord reports as unavailable (outage); never treated as gone
     */
    public void reconcile(Set<String> present, Set<String> unavailable) {
        for (String guildId : repo.departedGuilds()) {
            if (present.contains(guildId)) {
                repo.clearDeparture(guildId);
            }
        }

        Set<String> withData = repo.guildsWithData();
        Set<String> missing = new HashSet<>(withData);
        missing.removeAll(present);
        missing.removeAll(unavailable);
        if (missing.size() > Math.max(MIN_SUSPICIOUS, withData.size() / 2)) {
            LOG.error("{} of {} guilds with data look gone at startup; refusing to schedule their deletion. "
                    + "Check BOT_TOKEN (is this the right application?) and restart.", missing.size(), withData.size());
            return;
        }
        Instant now = clock.instant();
        for (String guildId : missing) {
            repo.markDeparted(guildId, now);
        }
        if (!missing.isEmpty()) {
            LOG.info("{} guild(s) were left while the bot was offline; their data will be deleted after {} days",
                    missing.size(), GRACE_PERIOD.toDays());
        }
    }

    public void purgeExpired() {
        List<String> purged = repo.purgeDepartedBefore(clock.instant().minus(GRACE_PERIOD));
        if (!purged.isEmpty()) {
            LOG.info("Deleted all data of {} guild(s) the bot left more than {} days ago",
                    purged.size(), GRACE_PERIOD.toDays());
        }
    }
}
