package Commands.Calendar;

import Commands.Framework.SlashCommand;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;

import java.util.List;

public class SessionCommands implements SlashCommand {
    @Override public String getId() { return "session"; }
    @Override public List<String> getCommandNames() { return List.of(); }
    @Override public List<SlashCommandData> getCommandData() { return List.of(); }
    @Override public void execute(SlashCommandInteractionEvent event) {}
}
