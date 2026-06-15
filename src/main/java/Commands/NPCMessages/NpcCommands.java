package Commands.NPCMessages;

import Commands.Framework.SlashCommand;
import DataHandlers.NPCMessageHandler;
import Players.DM;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;

import java.awt.Color;
import java.util.List;
import java.util.Map;

public class NpcCommands implements SlashCommand {

    @Override
    public String getId() {
        return "npc";
    }

    @Override
    public List<String> getCommandNames() {
        return List.of("npc");
    }

    @Override
    public List<SlashCommandData> getCommandData() {
        return List.of(Commands.slash("npc", "Manage NPC messages")
                .addSubcommands(
                        new SubcommandData("add", "Add an NPC message")
                                .addOptions(
                                        new OptionData(OptionType.STRING, "message", "The message text", true),
                                        new OptionData(OptionType.STRING, "npc", "NPC name (optional)"),
                                        new OptionData(OptionType.STRING, "type", "Message type")
                                                .addChoice("Basic", "Basic")
                                                .addChoice("Specific (next session, DM only)", "Specific"),
                                        new OptionData(OptionType.BOOLEAN, "private", "Reply only to you")),
                        new SubcommandData("remove", "Remove a basic NPC message")
                                .addOptions(new OptionData(OptionType.INTEGER, "index", "0-based index", true)),
                        new SubcommandData("list", "Show the basic NPC messages")));
    }

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        Guild guild = event.getGuild();
        if (guild == null) {
            event.reply("This command only works in a server.").setEphemeral(true).queue();
            return;
        }
        switch (event.getSubcommandName()) {
            case "add" -> add(event, guild);
            case "remove" -> remove(event, guild);
            case "list" -> list(event, guild);
            default -> event.reply("Unknown subcommand.").setEphemeral(true).queue();
        }
    }

    private void add(SlashCommandInteractionEvent event, Guild guild) {
        String message = event.getOption("message").getAsString();
        String npc = event.getOption("npc", "", OptionMapping::getAsString);
        String type = event.getOption("type", "Basic", OptionMapping::getAsString);
        boolean isPrivate = event.getOption("private", false, OptionMapping::getAsBoolean);

        if (("Specific".equals(type) || isPrivate) && !isDm(event.getMember(), guild)) {
            event.reply("Only the DM can add specific/private NPC messages.").setEphemeral(true).queue();
            return;
        }
        new NPCMessageHandler(guild).addMessage(message, type, npc);
        event.reply(String.format("Added '%s' to the %s NPC-message list", message, type)).setEphemeral(isPrivate).queue();
    }

    private void remove(SlashCommandInteractionEvent event, Guild guild) {
        int index = event.getOption("index").getAsInt();
        NPCMessageHandler handler = new NPCMessageHandler(guild);
        if (index >= 0 && index < handler.getBasicMessages().size()) {
            handler.removeMessage(index);
            event.reply("Removed message #" + index).queue();
        } else {
            event.reply("There is no message #" + index).setEphemeral(true).queue();
        }
    }

    private void list(SlashCommandInteractionEvent event, Guild guild) {
        List<Map<String, String>> messages = new NPCMessageHandler(guild).getBasicMessages();
        EmbedBuilder eb = new EmbedBuilder();
        eb.setColor(Color.ORANGE);
        eb.setTitle("Basic NPC messages");
        for (Map<String, String> m : messages) {
            String name = m.get("npc");
            eb.addField(name != null && !name.isEmpty() ? name : "Sumarville", m.get("message"), false);
        }
        event.replyEmbeds(eb.build()).queue();
    }

    private boolean isDm(Member member, Guild guild) {
        if (member == null) {
            return false;
        }
        Role dmRole = new DM(guild).getRole();
        return dmRole != null && member.getRoles().contains(dmRole);
    }
}
