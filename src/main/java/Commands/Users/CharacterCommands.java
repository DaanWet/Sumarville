package Commands.Users;

import Commands.Framework.Interactions;
import Commands.Framework.SlashCommand;
import Database.Repositories;
import Domain.CharacterSheet;
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
import java.util.List;
import java.util.Optional;

public class CharacterCommands implements SlashCommand {

    private final Repositories repos;

    public CharacterCommands(Repositories repos) {
        this.repos = repos;
    }

    @Override
    public String getId() {
        return "character";
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
        Guild guild = Interactions.requireGuild(event);
        if (guild == null) return;
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

        if (!asNpc) {
            if (member != null && new DM(guild, repos.config()).isHeldBy(member)) {
                event.reply("Character creation cancelled — you are the DM.").setEphemeral(true).queue();
                return;
            }
            if (member != null && repos.characters().findByUserId(guild.getId(), member.getId()).isPresent()) {
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
                repos.characters().findByUserId(guild.getId(), event.getMember().getId())
                        .ifPresent(c -> repos.characters().remove(c.id()));
            }
            event.replyModal(buildModal(false)).queue();
        }
    }

    @Override
    public void onModal(ModalInteractionEvent event) {
        Guild guild = Interactions.requireGuild(event);
        if (guild == null) return;
        Member member = event.getMember();
        if (member == null) return;
        boolean asNpc = event.getModalId().equals("character:create:npc");
        String name = event.getValue("name").getAsString();
        String picture = event.getValue("picture") != null ? event.getValue("picture").getAsString() : "";

        repos.characters().add(guild.getId(), asNpc ? null : member.getId(), name, picture);
        if (!asNpc) {
            guild.addRoleToMember(member, new Player(guild, repos.config()).getRole()).queue();
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
        Optional<CharacterSheet> existing = repos.characters().findByName(guild.getId(), name);
        if (existing.isEmpty()) {
            event.reply("No character named " + name).setEphemeral(true).queue();
            return;
        }
        repos.characters().editAttribute(existing.get().id(), attribute, value);
        if (attribute.equals("name")) {
            String userId = existing.get().userId();
            if (userId != null) {
                Member m = guild.getMemberById(userId);
                if (m != null) {
                    guild.modifyNickname(m, value).queue();
                }
            }
        }
        event.reply(String.format("Edited %s of %s to %s", attribute, name, value)).queue();
    }

    private void remove(SlashCommandInteractionEvent event, Guild guild) {
        String name = event.getOption("name").getAsString();
        Optional<CharacterSheet> character = repos.characters().findByName(guild.getId(), name);
        if (character.isEmpty()) {
            event.reply("No character named " + name).setEphemeral(true).queue();
            return;
        }
        repos.characters().remove(character.get().id());
        String userId = character.get().userId();
        if (userId != null) {
            Member m = guild.getMemberById(userId);
            if (m != null) {
                guild.modifyNickname(m, null).queue();
            }
        }
        event.reply("Successfully removed " + name).queue();
    }

    private void show(SlashCommandInteractionEvent event, Guild guild) {
        String target = event.getOption("target", "all", OptionMapping::getAsString);
        List<CharacterSheet> list;
        if (target.equalsIgnoreCase("all")) {
            list = repos.characters().findAll(guild.getId(), false);
        } else if (target.equalsIgnoreCase("npc")) {
            list = repos.characters().findAll(guild.getId(), true);
        } else {
            Optional<CharacterSheet> character = repos.characters().findByName(guild.getId(), target);
            if (character.isEmpty()) {
                character = repos.characters().findByUserId(guild.getId(), target);
            }
            list = character.map(List::of).orElse(null);
        }
        if (list == null) {
            event.reply("No character found for " + target).setEphemeral(true).queue();
            return;
        }
        EmbedBuilder eb = new EmbedBuilder();
        eb.setColor(Color.ORANGE);
        if (list.size() == 1) {
            CharacterSheet c = list.get(0);
            eb.addField("Name", c.name(), true);
            eb.addField("Picture", c.picture() == null || c.picture().isEmpty() ? "—" : c.picture(), true);
        } else {
            eb.setTitle("Players");
            for (CharacterSheet c : list) {
                Member m = c.isNpc() ? null : guild.getMemberById(c.userId());
                String who = c.isNpc() ? "NPC" : (m != null ? m.getAsMention() : "Unknown");
                eb.addField(c.name(), who, true);
            }
        }
        event.replyEmbeds(eb.build()).queue();
    }
}
