package Commands.Users;

import Commands.Framework.SlashCommand;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;

import java.util.List;

public class DmCommands implements SlashCommand {
    @Override public String getId() { return "dm"; }
    @Override public List<String> getCommandNames() { return List.of(); }
    @Override public List<SlashCommandData> getCommandData() { return List.of(); }
    @Override public void execute(SlashCommandInteractionEvent event) {}
}
