package Commands;

import Commands.Framework.CommandRegistry;
import Commands.Framework.SlashCommand;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;

import java.awt.Color;
import java.util.List;

public class HelpCommand implements SlashCommand {

    private final CommandRegistry registry;

    public HelpCommand(CommandRegistry registry) {
        this.registry = registry;
    }

    @Override
    public String getId() {
        return "help";
    }

    @Override
    public List<String> getCommandNames() {
        return List.of("help");
    }

    @Override
    public List<SlashCommandData> getCommandData() {
        return List.of(Commands.slash("help", "Shows all Sumarville commands"));
    }

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        EmbedBuilder eb = new EmbedBuilder();
        eb.setTitle("Sumarville Commands");
        eb.setColor(Color.ORANGE);
        for (SlashCommand cmd : registry.all()) {
            for (SlashCommandData data : cmd.getCommandData()) {
                StringBuilder value = new StringBuilder(data.getDescription());
                List<SubcommandData> subs = data.getSubcommands();
                if (!subs.isEmpty()) {
                    for (SubcommandData sub : subs) {
                        value.append("\n• `").append(sub.getName()).append("` — ").append(sub.getDescription());
                    }
                }
                eb.addField("/" + data.getName(), value.toString(), false);
            }
        }
        event.replyEmbeds(eb.build()).setEphemeral(true).queue();
    }
}
