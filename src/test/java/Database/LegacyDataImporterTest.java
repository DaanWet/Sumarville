package Database;

import ch.qos.logback.classic.Level;
import Domain.CharacterSheet;
import Domain.FoodItem;
import Domain.NpcMessageType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class LegacyDataImporterTest {

    private static final String SAMPLE = """
            {
              "111": {
                "Config": { "DM": "900", "FoodChannel": "800" },
                "Dates": [ "25/12/2031" ],
                "Food": [ { "Name": "Pizza", "Emoji": "🍕" } ],
                "Characters": [
                  { "userid": "u1", "name": "Aragorn", "picture": "pic" },
                  { "userid": "", "name": "Goblin", "picture": "" }
                ],
                "Messages": {
                  "Basic": [ { "npc": "Bob", "message": "Hi" } ],
                  "Specific": [ { "npc": "", "message": "Soon" } ]
                }
              }
            }
            """;

    @Test
    void importsEveryBucketThenIsIdempotent(@TempDir Path dir) throws Exception {
        Path json = dir.resolve("Data.json");
        Files.writeString(json, SAMPLE);

        try (Database db = new Database(dir.resolve("imp.db").toString(), null)) {
            LegacyDataImporter.run(db, json.toString(), false);

            assertEquals("900", new ConfigRepository(db).get("111", "DM").orElseThrow());

            List<LocalDateTime> sessions = new SessionRepository(db).find("111", true);
            assertEquals(1, sessions.size());
            assertEquals(LocalDateTime.of(2031, 12, 25, 0, 0), sessions.get(0));

            List<FoodItem> food = new FoodRepository(db).findAll("111");
            assertEquals(1, food.size());
            assertEquals("Pizza", food.get(0).name());
            assertEquals("🍕", food.get(0).emoji());

            CharacterRepository chars = new CharacterRepository(db);
            assertFalse(chars.findByName("111", "Aragorn").orElseThrow().isNpc());
            assertTrue(chars.findByName("111", "Goblin").orElseThrow().isNpc()); // "" -> NPC

            assertEquals(1, new NpcMessageRepository(db).findByType("111", NpcMessageType.BASIC).size());
            assertEquals(1, new NpcMessageRepository(db).findByType("111", NpcMessageType.SPECIFIC).size());

            // file renamed
            assertFalse(Files.exists(json));
            assertTrue(Files.exists(dir.resolve("Data.json.imported")));

            // idempotent: second run does nothing, no duplicate rows
            LegacyDataImporter.run(db, json.toString(), false);
            assertEquals(1, new FoodRepository(db).findAll("111").size());
        }
    }

    @Test
    void successfulImportReportsGuildCountAndSourceFile(@TempDir Path dir) throws Exception {
        Path json = dir.resolve("Data.json");
        Files.writeString(json, "{ \"1\": {}, \"2\": {}, \"3\": {} }");
        try (Database db = new Database(dir.resolve("log.db").toString(), null);
             LogCapture logs = new LogCapture(LegacyDataImporter.class)) {
            LegacyDataImporter.run(db, json.toString(), false);

            List<String> info = logs.messagesAt(Level.INFO);
            assertEquals(1, info.size());
            assertTrue(info.get(0).contains("3 guilds"));
            assertTrue(info.get(0).contains(json.toString()));
        }
    }

    @Test
    void guildWithMissingBucketsImportsWithoutError(@TempDir Path dir) throws Exception {
        Path json = dir.resolve("Data.json");
        Files.writeString(json, "{ \"222\": {} }");
        try (Database db = new Database(dir.resolve("m.db").toString(), null)) {
            LegacyDataImporter.run(db, json.toString(), false); // must not throw
            assertTrue(new FoodRepository(db).findAll("222").isEmpty());
            assertEquals(0, new SessionRepository(db).find("222", true).size());
        }
    }

    @Test
    void missingFileIsRefusedWithItsAbsolutePath(@TempDir Path dir) {
        String relative = "no-such-dir/Data.json";
        try (Database db = new Database(dir.resolve("r.db").toString(), null)) {
            IllegalStateException e = assertThrows(IllegalStateException.class,
                    () -> LegacyDataImporter.run(db, relative, false));

            assertTrue(e.getMessage().contains(Path.of(relative).toAbsolutePath().normalize().toString()));
        }
    }

    @Test
    void refusedImportRunsOnceTheFileAppears(@TempDir Path dir) throws Exception {
        Path json = dir.resolve("Data.json");
        try (Database db = new Database(dir.resolve("late.db").toString(), null)) {
            assertThrows(IllegalStateException.class, () -> LegacyDataImporter.run(db, json.toString(), false));

            Files.writeString(json, SAMPLE);
            LegacyDataImporter.run(db, json.toString(), false);

            assertEquals(1, new FoodRepository(db).findAll("111").size());
        }
    }

    @Test
    void freshInstallWithoutFileSetsFlagSoLaterImportIsSkipped(@TempDir Path dir) throws Exception {
        Path json = dir.resolve("Data.json");
        try (Database db = new Database(dir.resolve("nf.db").toString(), null)) {
            // First run: no file present, fresh install allowed -> sets the imported flag, imports nothing
            LegacyDataImporter.run(db, json.toString(), true);
            // A Data.json now appears, but the flag must cause it to be skipped, even with the opt-in removed
            Files.writeString(json, "{ \"333\": { \"Food\": [ { \"Name\": \"X\", \"Emoji\": \"x\" } ] } }");
            LegacyDataImporter.run(db, json.toString(), false);
            assertTrue(new FoodRepository(db).findAll("333").isEmpty());
        }
    }

    @Test
    void freshInstallLogsAWarning(@TempDir Path dir) {
        try (Database db = new Database(dir.resolve("fresh.db").toString(), null);
             LogCapture logs = new LogCapture(LegacyDataImporter.class)) {
            LegacyDataImporter.run(db, dir.resolve("Data.json").toString(), true);

            assertEquals(1, logs.messagesAt(Level.WARN).size());
        }
    }

    @Test
    void alreadyImportedWarnsOnlyWhenADataJsonIsStillPresent(@TempDir Path dir) throws Exception {
        Path json = dir.resolve("Data.json");
        Files.writeString(json, SAMPLE);
        try (Database db = new Database(dir.resolve("done.db").toString(), null);
             LogCapture logs = new LogCapture(LegacyDataImporter.class)) {
            LegacyDataImporter.run(db, json.toString(), false); // imports, then renames the file

            LegacyDataImporter.run(db, json.toString(), false); // normal restart: nothing to warn about
            assertTrue(logs.messagesAt(Level.WARN).isEmpty());

            Files.writeString(json, SAMPLE); // a Data.json shows up again next to an imported database
            LegacyDataImporter.run(db, json.toString(), false);

            List<String> warnings = logs.messagesAt(Level.WARN);
            assertEquals(1, warnings.size());
            assertTrue(warnings.get(0).contains(json.toString()));
            assertEquals(1, new SessionRepository(db).find("111", true).size()); // ignored, not imported twice
        }
    }

    @Test
    void unparseableSessionDateIsSkippedWithoutThrowing(@TempDir Path dir) throws Exception {
        Path json = dir.resolve("Data.json");
        Files.writeString(json, "{ \"444\": { \"Dates\": [ \"not-a-date\" ] } }");
        try (Database db = new Database(dir.resolve("bad.db").toString(), null)) {
            LegacyDataImporter.run(db, json.toString(), false); // must not throw
            assertEquals(0, new SessionRepository(db).find("444", true).size());
        }
    }
}
