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
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.MemberCachePolicy;

import java.util.EnumSet;

public class Main {

    public static void main(String[] args) throws Exception {
        CommandRegistry registry = new CommandRegistry();
        registry.register(new DiceCommands());
        registry.register(new SessionCommands());
        registry.register(new FoodCommands());
        registry.register(new NpcCommands());
        registry.register(new CharacterCommands());
        registry.register(new DmCommands());
        registry.register(new ConfigCommands());
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

        jda.getGuilds().forEach(SessionReminder::onRestart);
    }
}
