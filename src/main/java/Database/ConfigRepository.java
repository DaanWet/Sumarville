package Database;

import java.util.Optional;

public class ConfigRepository {

    private final Database db;

    public ConfigRepository(Database db) {
        this.db = db;
    }

    public Optional<String> get(String guildId, String key) {
        return db.query(
                "SELECT config_value FROM guild_config WHERE guild_id = ? AND config_key = ?",
                rs -> rs.getString(1), guildId, key).stream().findFirst();
    }

    public void set(String guildId, String key, String value) {
        db.update(
                "INSERT INTO guild_config(guild_id, config_key, config_value) VALUES(?,?,?) " +
                        "ON CONFLICT(guild_id, config_key) DO UPDATE SET config_value = excluded.config_value",
                guildId, key, value);
    }
}
