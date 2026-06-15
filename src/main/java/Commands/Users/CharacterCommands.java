package Commands.Users;

import Commands.Framework.SlashCommand;
import DataHandlers.CharacterHandler;
import Players.DM;
import Players.Player;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.components.label.Label;
import net.dv8tion.jda.api.components.textinput.TextInput;
import net.dv8tion.jda.api.components.textinput.TextInputStyle;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.dv8tion.jda.api.modals.Modal;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class CharacterCommands implements SlashCommand {

    @Override
    public String getId() {
        return "character";
    }

    @Override
    public List<String> getCommandNames() {
        return List.of("character");
    }

    @Override
    public List<SlashCommandData> getCommandData() {
        return List.of(Commands.slash("character", "Manage character sheets")
                .addSubcommands(
                        new SubcommandData("create", "Create your character sheet")
                                .addOptions(new OptionData(OptionType.BOOLEAN, "as_npc", "Create an NPC instead of your own character")),
                        new SubcommandData("edit", "Edit a character attribute")
                                .addOptions(
                                        new OptionData(OptionType.STRING, "name", "Character name", true),
                                        new OptionData(OptionType.STRING, "attribute", "Attribute to change", true)
                                                .addChoice("name", "name")
                                                .addChoice("picture", "picture"),
                                        new OptionData(OptionType.STRING, "value", "New value", true)),
                        new SubcommandData("remove", "Remove a character")
                                .addOptions(new OptionData(OptionType.STRING, "name", "Character name", true)),
                        new SubcommandData("show", "Show a character (or all)")
                                .addOptions(new OptionData(OptionType.STRING, "target", "all, npc, or a character name"))));
    }

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        Guild guild = event.getGuild();
        if (guild == null) {
            event.reply("This command only works in a server.").setEphemeral(true).queue();
            return;
        }
        switch (event.getSubcommandName()) {
            case "create" -> create(event, guild);
            case "edit" -> edit(event, guild);
            case "remove" -> remove(event, guild);
            case "show" -> show(event, guild);
            default -> event.reply("Unknown subcommand.").setEphemeral(true).queue();
        }
    }

    private void create(SlashCommandInteractionEvent event, Guild guild) {
        boolean asNpc = event.getOption("as_npc", false, OptionMapping::getAsBoolean);
        Member member = event.getMember();
        CharacterHandler handler = new CharacterHandler(guild);

        if (!asNpc) {
            if (member != null && isDm(member, guild)) {
                event.reply("Character creation cancelled — you are the DM.").setEphemeral(true).queue();
                return;
            }
            if (member != null && handler.getCharacter(member.getId(), "userid") != null) {
                event.reply("You already have a character. Overwrite it?")
                        .setEphemeral(true)
                        .addComponents(ActionRow.of(
                                Button.danger("character:overwrite:self", "Overwrite"),
                                Button.secondary("character:cancel", "Cancel")))
                        .queue();
                return;
            }
        }
        event.replyModal(buildModal(asNpc)).queue();
    }

    private Modal buildModal(boolean asNpc) {
        TextInput name = TextInput.create("name", TextInputStyle.SHORT)
                .setPlaceholder("Character name")
                .setRequired(true)
                .setMaxLength(100)
                .build();
        TextInput picture = TextInput.create("picture", TextInputStyle.SHORT)
                .setPlaceholder("Image URL (optional)")
                .setRequired(false)
                .build();
        return Modal.create(asNpc ? "character:create:npc" : "character:create:self", "Create character")
                .addComponents(Label.of("Name", name), Label.of("Picture", picture))
                .build();
    }

    @Override
    public void onButton(ButtonInteractionEvent event) {
        String id = event.getComponentId();
        if (id.equals("character:cancel")) {
            event.editMessage("Character creation cancelled.").setComponents().queue();
        } else if (id.equals("character:overwrite:self")) {
            Guild guild = event.getGuild();
            if (guild != null && event.getMember() != null) {
                new CharacterHandler(guild).removeCharacter(event.getMember().getId(), "userid");
            }
            event.replyModal(buildModal(false)).queue();
        }
    }

    @Override
    public void onModal(ModalInteractionEvent event) {
        Guild guild = event.getGuild();
        Member member = event.getMember();
        if (guild == null || member == null) {
            event.reply("This only works in a server.").setEphemeral(true).queue();
            return;
        }
        boolean asNpc = event.getModalId().equals("character:create:npc");
        String name = event.getValue("name").getAsString();
        String picture = event.getValue("picture") != null ? event.getValue("picture").getAsString() : "";

        Map<String, String> sheet = new java.util.HashMap<>();
        sheet.put("userid", asNpc ? "" : member.getId());
        sheet.put("name", name);
        sheet.put("picture", picture);

        new CharacterHandler(guild).addCharacter(sheet);
        if (!asNpc) {
            guild.addRoleToMember(member, new Player(guild).getRole()).queue();
            guild.modifyNickname(member, name).queue();
        }

        EmbedBuilder eb = new EmbedBuilder();
        eb.setColor(Color.ORANGE);
        eb.setTitle("Newly Created Character:");
        eb.addField("Name", name, true);
        eb.addField("Picture", picture.isEmpty() ? "—" : picture, true);
        event.replyEmbeds(eb.build()).queue();
    }

    private void edit(SlashCommandInteractionEvent event, Guild guild) {
        String name = event.getOption("name").getAsString();
        String attribute = event.getOption("attribute").getAsString();
        String value = event.getOption("value").getAsString();
        CharacterHandler handler = new CharacterHandler(guild);
        if (handler.getCharacter(name, "name") == null) {
            event.reply("No character named " + name).setEphemeral(true).queue();
            return;
        }
        handler.editCharacter(name, attribute, value);
        if (attribute.equals("name")) {
            Map<String, String> updated = handler.getCharacter(value, "name");
            if (updated != null && !updated.get("userid").isEmpty()) {
                Member m = guild.getMemberById(updated.get("userid"));
                if (m != null) {
                    guild.modifyNickname(m, value).queue();
                }
            }
        }
        event.reply(String.format("Edited %s of %s to %s", attribute, name, value)).queue();
    }

    private void remove(SlashCommandInteractionEvent event, Guild guild) {
        String name = event.getOption("name").getAsString();
        CharacterHandler handler = new CharacterHandler(guild);
        Map<String, String> character = handler.getCharacter(name, "name");
        if (character == null) {
            event.reply("No character named " + name).setEphemeral(true).queue();
            return;
        }
        handler.removeCharacter(name, "name");
        if (!character.get("userid").isEmpty()) {
            Member m = guild.getMemberById(character.get("userid"));
            if (m != null) {
                guild.modifyNickname(m, null).queue();
            }
        }
        event.reply("Successfully removed " + name).queue();
    }

    private void show(SlashCommandInteractionEvent event, Guild guild) {
        String target = event.getOption("target", "all", OptionMapping::getAsString);
        CharacterHandler handler = new CharacterHandler(guild);
        List<Map<String, String>> list;
        if (target.equalsIgnoreCase("all")) {
            list = handler.getAllCharacters(false);
        } else if (target.equalsIgnoreCase("npc")) {
            list = handler.getAllCharacters(true);
        } else {
            Map<String, String> character = handler.getCharacter(target, "name");
            if (character == null) {
                character = handler.getCharacter(target, "userid");
            }
            list = character == null ? null : new ArrayList<>(Collections.singletonList(character));
        }
        if (list == null) {
            event.reply("No character found for " + target).setEphemeral(true).queue();
            return;
        }
        EmbedBuilder eb = new EmbedBuilder();
        eb.setColor(Color.ORANGE);
        if (list.size() == 1) {
            for (Map.Entry<String, String> e : list.get(0).entrySet()) {
                String key = e.getKey();
                eb.addField(key.substring(0, 1).toUpperCase() + key.substring(1), e.getValue(), true);
            }
        } else {
            eb.setTitle("Players");
            for (Map<String, String> c : list) {
                String userid = c.get("userid");
                String who = userid.isEmpty() ? "NPC"
                        : (guild.getMemberById(userid) != null ? guild.getMemberById(userid).getAsMention() : "Unknown");
                eb.addField(c.get("name"), who, true);
            }
        }
        event.replyEmbeds(eb.build()).queue();
    }

    private boolean isDm(Member member, Guild guild) {
        var dmRole = new DM(guild).getRole();
        return dmRole != null && member.getRoles().contains(dmRole);
    }
}
