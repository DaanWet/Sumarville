package Database;

import Domain.NpcMessageType;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.io.File;
import java.io.FileReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.logging.Logger;

/**
 * One-time importer from the legacy {@code Data.json} into the database. Idempotent:
 * guarded by a {@code meta} flag, and the source file is renamed after a successful run.
 */
public final class LegacyDataImporter {

    private static final Logger LOG = Logger.getLogger(LegacyDataImporter.class.getName());
    private static final DateTimeFormatter LEGACY_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter ISO = DateTimeFormatter.ISO_LOCAL_DATE;

    private LegacyDataImporter() {
    }

    public static void run(Database db, String dataJsonPath) {
        boolean alreadyImported = !db.query(
                "SELECT value FROM meta WHERE key = 'legacy_imported'", rs -> rs.getString(1)).isEmpty();
        if (alreadyImported) {
            return;
        }

        File file = new File(dataJsonPath);
        if (!file.exists()) {
            db.update("INSERT INTO meta(key, value) VALUES('legacy_imported', 'true')");
            return;
        }

        final JSONObject root;
        try (FileReader reader = new FileReader(file, StandardCharsets.UTF_8)) {
            root = (JSONObject) new JSONParser().parse(reader);
        } catch (Exception e) {
            throw new RuntimeException("Failed to read legacy " + dataJsonPath, e);
        }

        db.runInTransaction(() -> {
            for (Object guildKey : root.keySet()) {
                String guildId = (String) guildKey;
                JSONObject g = (JSONObject) root.get(guildId);
                importConfig(db, guildId, g);
                importDates(db, guildId, g);
                importFood(db, guildId, g);
                importCharacters(db, guildId, g);
                importMessages(db, guildId, g);
            }
            db.update("INSERT INTO meta(key, value) VALUES('legacy_imported', 'true')");
        });

        if (!file.renameTo(new File(dataJsonPath + ".imported"))) {
            LOG.warning("Imported Data.json but could not rename it to Data.json.imported");
        }
    }

    private static void importConfig(Database db, String guildId, JSONObject g) {
        JSONObject config = (JSONObject) g.getOrDefault("Config", new JSONObject());
        for (Object k : config.keySet()) {
            db.update("INSERT INTO guild_config(guild_id, config_key, config_value) VALUES(?,?,?)",
                    guildId, (String) k, String.valueOf(config.get(k)));
        }
    }

    private static void importDates(Database db, String guildId, JSONObject g) {
        JSONArray dates = (JSONArray) g.getOrDefault("Dates", new JSONArray());
        for (Object d : dates) {
            try {
                String iso = LocalDate.parse((String) d, LEGACY_DATE).format(ISO);
                db.insert("INSERT INTO sessions(guild_id, session_date) VALUES(?,?)", guildId, iso);
            } catch (Exception e) {
                LOG.warning("Skipping unparseable session date '" + d + "' for guild " + guildId);
            }
        }
    }

    private static void importFood(Database db, String guildId, JSONObject g) {
        JSONArray food = (JSONArray) g.getOrDefault("Food", new JSONArray());
        for (Object f : food) {
            JSONObject fo = (JSONObject) f;
            db.insert("INSERT INTO food(guild_id, name, emoji) VALUES(?,?,?)",
                    guildId, (String) fo.get("Name"), (String) fo.get("Emoji"));
        }
    }

    private static void importCharacters(Database db, String guildId, JSONObject g) {
        JSONArray chars = (JSONArray) g.getOrDefault("Characters", new JSONArray());
        for (Object c : chars) {
            JSONObject co = (JSONObject) c;
            String userId = (String) co.get("userid");
            db.insert("INSERT INTO characters(guild_id, user_id, name, picture) VALUES(?,?,?,?)",
                    guildId,
                    (userId == null || userId.isEmpty()) ? null : userId,
                    (String) co.get("name"),
                    (String) co.get("picture"));
        }
    }

    private static void importMessages(Database db, String guildId, JSONObject g) {
        JSONObject messages = (JSONObject) g.getOrDefault("Messages", new JSONObject());
        importMessageList(db, guildId, (JSONArray) messages.getOrDefault("Basic", new JSONArray()), NpcMessageType.BASIC);
        importMessageList(db, guildId, (JSONArray) messages.getOrDefault("Specific", new JSONArray()), NpcMessageType.SPECIFIC);
    }

    private static void importMessageList(Database db, String guildId, JSONArray list, NpcMessageType type) {
        for (Object m : list) {
            JSONObject mo = (JSONObject) m;
            db.insert("INSERT INTO npc_messages(guild_id, type, npc, message) VALUES(?,?,?,?)",
                    guildId, type.name(), (String) mo.get("npc"), (String) mo.get("message"));
        }
    }
}
