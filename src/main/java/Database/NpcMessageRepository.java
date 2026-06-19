package Database;

import Domain.NpcMessage;
import Domain.NpcMessageType;

import java.util.List;

public class NpcMessageRepository {

    private final Database db;

    public NpcMessageRepository(Database db) {
        this.db = db;
    }

    public void add(String guildId, NpcMessageType type, String npc, String message) {
        db.insert("INSERT INTO npc_messages(guild_id, type, npc, message) VALUES(?,?,?,?)",
                guildId, type.name(), npc, message);
    }

    public void remove(long id) {
        db.update("DELETE FROM npc_messages WHERE id = ?", id);
    }

    public List<NpcMessage> findByType(String guildId, NpcMessageType type) {
        return db.query(
                "SELECT id, type, npc, message FROM npc_messages WHERE guild_id = ? AND type = ? ORDER BY id",
                rs -> new NpcMessage(rs.getLong("id"), NpcMessageType.valueOf(rs.getString("type")),
                        rs.getString("npc"), rs.getString("message")),
                guildId, type.name());
    }

    public void clearSpecific(String guildId) {
        db.update("DELETE FROM npc_messages WHERE guild_id = ? AND type = ?",
                guildId, NpcMessageType.SPECIFIC.name());
    }
}
