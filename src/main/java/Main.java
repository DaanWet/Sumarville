import Commands.ConfigCommands;
import Commands.HelpCommand;
import Commands.Calendar.SessionCommands;
import Commands.Calendar.SessionReminder;
import Commands.Dice.DiceCommands;
import Commands.Food.FoodCommands;
import Commands.Framework.CommandRegistry;
import Commands.Framework.CommandRouter;
import Commands.NPCMessages.NpcCommands;
import Commands.Users.CharacterCommands;
import Commands.Users.DmCommands;
import Database.Database;
import Database.LegacyDataImporter;
import Database.Repositories;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.MemberCachePolicy;

import java.util.EnumSet;

public class Main {

    public static void main(String[] args) throws Exception {
        String dbPath = System.getenv().getOrDefault("DB_PATH", "./data/sumarville.db");
        String dbKey = System.getenv("DB_ENCRYPTION_KEY");
        Database db = new Database(dbPath, dbKey);
        LegacyDataImporter.run(db, "./Data.json");
        Repositories repos = new Repositories(db);

        CommandRegistry registry = new CommandRegistry();
        registry.register(new DiceCommands());
        registry.register(new SessionCommands(repos));
        registry.register(new FoodCommands(repos));
        registry.register(new NpcCommands(repos));
        registry.register(new CharacterCommands(repos));
        registry.register(new DmCommands(repos));
        registry.register(new ConfigCommands(repos));
        registry.register(new HelpCommand(registry));

        String token = System.getenv("BOT_TOKEN");
        JDA jda = JDABuilder.createDefault(token, EnumSet.of(GatewayIntent.GUILD_MEMBERS))
                .setMemberCachePolicy(MemberCachePolicy.ALL)
                .setActivity(Activity.listening("/help"))
                .addEventListeners(new CommandRouter(registry))
                .build();
        jda.awaitReady();

        String testGuildId = System.getenv("TEST_GUILD_ID");
        if (testGuildId != null && !testGuildId.isBlank()) {
            Guild g = jda.getGuildById(testGuildId);
            if (g != null) {
                g.updateCommands().addCommands(registry.allCommandData()).queue();
            }
        } else {
            jda.updateCommands().addCommands(registry.allCommandData()).queue();
        }

        jda.getGuilds().forEach(g -> SessionReminder.onRestart(g, repos));
    }
}
