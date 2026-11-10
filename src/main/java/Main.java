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
import Observability.Observability;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.Guild;
import Retention.GuildRetention;
import Retention.GuildRetentionListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Clock;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class Main {

    private static final Logger LOG = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) throws Exception {
        Observability.initSentry(
                System.getenv("SENTRY_DSN"),
                System.getenv().getOrDefault("SENTRY_ENVIRONMENT", "production"));

        String dbPath = System.getenv().getOrDefault("DB_PATH", "./data/sumarville.db");
        String dbKey = System.getenv("DB_ENCRYPTION_KEY");
        boolean allowUnencrypted = "true".equalsIgnoreCase(System.getenv("DB_ALLOW_UNENCRYPTED"));
        Database db = Database.open(dbPath, dbKey, allowUnencrypted);
        boolean allowFreshInstall = "true".equalsIgnoreCase(System.getenv("DB_ALLOW_FRESH_INSTALL"));
        LegacyDataImporter.run(db, "./Data.json", allowFreshInstall);
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

        GuildRetention retention = new GuildRetention(repos.departures(), Clock.systemUTC());

        String token = System.getenv("BOT_TOKEN");
        // No privileged intents: every command is a slash command, and the caller's member data
        // (roles, permissions) arrives with the interaction itself.
        JDA jda = JDABuilder.createDefault(token)
                .setActivity(Activity.listening("/help"))
                .addEventListeners(new CommandRouter(registry), new GuildRetentionListener(retention))
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

        retention.reconcile(
                jda.getGuilds().stream().map(Guild::getId).collect(Collectors.toSet()),
                jda.getUnavailableGuilds());
        ScheduledExecutorService purger = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "guild-data-purge");
            t.setDaemon(true);
            return t;
        });
        purger.scheduleAtFixedRate(() -> {
            try {
                retention.purgeExpired();
            } catch (RuntimeException e) {
                // An escaped exception would cancel every later run.
                LOG.error("Purging data of departed guilds failed", e);
            }
        }, 0, 1, TimeUnit.DAYS);
    }
}
