package Commands.NPCMessages;

import Commands.Framework.Interactions;
import Commands.Framework.SlashCommand;
import Database.Repositories;
import Domain.NpcMessage;
import Domain.NpcMessageType;
import Players.DM;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;

import java.awt.Color;
import java.util.List;

public class NpcCommands implements SlashCommand {

    private final Repositories repos;

    public NpcCommands(Repositories repos) {
        this.repos = repos;
    }

    @Override
    public String getId() {
        return "npc";
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
        Guild guild = Interactions.requireGuild(event);
        if (guild == null) return;
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

        if (("Specific".equals(type) || isPrivate) && !new DM(guild, repos.config()).isHeldBy(event.getMember())) {
            event.reply("Only the DM can add specific/private NPC messages.").setEphemeral(true).queue();
            return;
        }
        repos.npcMessages().add(guild.getId(), NpcMessageType.fromLegacy(type), npc, message);
        event.reply(String.format("Added '%s' to the %s NPC-message list", message, type)).setEphemeral(isPrivate).queue();
    }

    private void remove(SlashCommandInteractionEvent event, Guild guild) {
        int index = event.getOption("index").getAsInt();
        List<NpcMessage> basic = repos.npcMessages().findByType(guild.getId(), NpcMessageType.BASIC);
        if (index >= 0 && index < basic.size()) {
            repos.npcMessages().remove(basic.get(index).id());
            event.reply("Removed message #" + index).queue();
        } else {
            event.reply("There is no message #" + index).setEphemeral(true).queue();
        }
    }

    private void list(SlashCommandInteractionEvent event, Guild guild) {
        List<NpcMessage> messages = repos.npcMessages().findByType(guild.getId(), NpcMessageType.BASIC);
        EmbedBuilder eb = new EmbedBuilder();
        eb.setColor(Color.ORANGE);
        eb.setTitle("Basic NPC messages");
        for (NpcMessage m : messages) {
            String name = m.npc();
            eb.addField(name != null && !name.isEmpty() ? name : "Sumarville", m.message(), false);
        }
        event.replyEmbeds(eb.build()).queue();
    }
}
