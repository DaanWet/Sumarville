package Database;

import Domain.CharacterSheet;

import java.util.List;
import java.util.Optional;

public class CharacterRepository {

    private static final RowMapper<CharacterSheet> MAPPER = rs -> new CharacterSheet(
            rs.getLong("id"), rs.getString("user_id"), rs.getString("name"), rs.getString("picture"));

    private final Database db;

    public CharacterRepository(Database db) {
        this.db = db;
    }

    /** {@code userId} null or empty stores an NPC (NULL user_id). */
    public void add(String guildId, String userId, String name, String picture) {
        db.insert("INSERT INTO characters(guild_id, user_id, name, picture) VALUES(?,?,?,?)",
                guildId, (userId == null || userId.isEmpty()) ? null : userId, name, picture);
    }

    public Optional<CharacterSheet> findByName(String guildId, String name) {
        return db.query(
                "SELECT id, user_id, name, picture FROM characters WHERE guild_id = ? AND name = ? COLLATE NOCASE",
                MAPPER, guildId, name).stream().findFirst();
    }

    public Optional<CharacterSheet> findByUserId(String guildId, String userId) {
        return db.query(
                "SELECT id, user_id, name, picture FROM characters WHERE guild_id = ? AND user_id = ?",
                MAPPER, guildId, userId).stream().findFirst();
    }

    public void remove(long id) {
        db.update("DELETE FROM characters WHERE id = ?", id);
    }

    /** Only "name" and "picture" are editable (matching the slash-command choices). */
    public void editAttribute(long id, String attribute, String value) {
        String column = switch (attribute) {
            case "name" -> "name";
            case "picture" -> "picture";
            default -> throw new IllegalArgumentException("Unknown attribute: " + attribute);
        };
        db.update("UPDATE characters SET " + column + " = ? WHERE id = ?", value, id);
    }

    public List<CharacterSheet> findAll(String guildId, boolean npcOnly) {
        String sql = "SELECT id, user_id, name, picture FROM characters WHERE guild_id = ?"
                + (npcOnly ? " AND user_id IS NULL" : "") + " ORDER BY id";
        return db.query(sql, MAPPER, guildId);
    }
}
