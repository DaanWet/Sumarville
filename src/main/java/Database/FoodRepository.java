package Database;

import Domain.FoodItem;

import java.util.List;

public class FoodRepository {

    private final Database db;

    public FoodRepository(Database db) {
        this.db = db;
    }

    public void add(String guildId, String name, String emoji) {
        db.insert("INSERT INTO food(guild_id, name, emoji) VALUES(?,?,?)", guildId, name, emoji);
    }

    public void remove(long id) {
        db.update("DELETE FROM food WHERE id = ?", id);
    }

    public List<FoodItem> findAll(String guildId) {
        return db.query(
                "SELECT id, name, emoji FROM food WHERE guild_id = ? ORDER BY id",
                rs -> new FoodItem(rs.getLong("id"), rs.getString("name"), rs.getString("emoji")),
                guildId);
    }
}
