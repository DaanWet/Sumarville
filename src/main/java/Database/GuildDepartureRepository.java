package Database;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Tracks servers the bot has left, so their data can be deleted after a grace period
 * (see {@code Retention.GuildRetention}). {@code left_at} is stored as epoch millis so
 * the purge cutoff is a plain numeric comparison.
 */
public class GuildDepartureRepository {

    /** Every table that holds per-guild data; a purge deletes from all of them. */
    private static final List<String> GUILD_TABLES =
            List.of("guild_config", "sessions", "food", "characters", "npc_messages");

    private final Database db;

    public GuildDepartureRepository(Database db) {
        this.db = db;
    }

    /** Records that the bot left {@code guildId}; an earlier departure date is kept. */
    public void markDeparted(String guildId, Instant at) {
        db.update("INSERT INTO guild_departures (guild_id, left_at) VALUES (?, ?) ON CONFLICT(guild_id) DO NOTHING",
                guildId, at.toEpochMilli());
    }

    /** The bot is back in {@code guildId}: its data is kept. */
    public void clearDeparture(String guildId) {
        db.update("DELETE FROM guild_departures WHERE guild_id = ?", guildId);
    }

    public Set<String> departedGuilds() {
        return new HashSet<>(db.query("SELECT guild_id FROM guild_departures", rs -> rs.getString(1)));
    }

    /** Every guild that has at least one row in any per-guild table. */
    public Set<String> guildsWithData() {
        String sql = String.join(" UNION ",
                GUILD_TABLES.stream().map(t -> "SELECT guild_id FROM " + t).toList());
        return new HashSet<>(db.query(sql, rs -> rs.getString(1)));
    }

    /**
     * Deletes all data of guilds that left before {@code cutoff}, in one transaction.
     *
     * @return the purged guild ids
     */
    public List<String> purgeDepartedBefore(Instant cutoff) {
        List<String> expired = db.query("SELECT guild_id FROM guild_departures WHERE left_at < ? ORDER BY guild_id",
                rs -> rs.getString(1), cutoff.toEpochMilli());
        if (expired.isEmpty()) {
            return expired;
        }
        db.runInTransaction(() -> {
            for (String guildId : expired) {
                for (String table : GUILD_TABLES) {
                    db.update("DELETE FROM " + table + " WHERE guild_id = ?", guildId);
                }
                db.update("DELETE FROM guild_departures WHERE guild_id = ?", guildId);
            }
        });
        return expired;
    }
}
