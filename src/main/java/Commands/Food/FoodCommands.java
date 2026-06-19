package Commands.Food;

import Commands.Framework.Interactions;
import Commands.Framework.SlashCommand;
import Database.Repositories;
import Domain.FoodItem;
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

public class FoodCommands implements SlashCommand {

    private final Repositories repos;

    public FoodCommands(Repositories repos) {
        this.repos = repos;
    }

    @Override
    public String getId() {
        return "food";
    }

    @Override
    public List<SlashCommandData> getCommandData() {
        return List.of(Commands.slash("food", "Manage the lunch list")
                .addSubcommands(
                        new SubcommandData("add", "Add food to the lunch list")
                                .addOptions(
                                        new OptionData(OptionType.STRING, "name", "Name of the food", true),
                                        new OptionData(OptionType.STRING, "emoji", "Emoji to react with", true)),
                        new SubcommandData("remove", "Remove food from the lunch list")
                                .addOptions(
                                        new OptionData(OptionType.STRING, "emoji", "Emoji of the food"),
                                        new OptionData(OptionType.INTEGER, "index", "1-based index in the list")),
                        new SubcommandData("list", "Show the lunch list"),
                        new SubcommandData("poll", "Post the lunch poll in the food channel")));
    }

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        Guild guild = Interactions.requireGuild(event);
        if (guild == null) return;
        switch (event.getSubcommandName()) {
            case "add" -> add(event, guild);
            case "remove" -> remove(event, guild);
            case "list" -> list(event, guild);
            case "poll" -> poll(event, guild);
            default -> event.reply("Unknown subcommand.").setEphemeral(true).queue();
        }
    }

    private void add(SlashCommandInteractionEvent event, Guild guild) {
        String name = event.getOption("name").getAsString();
        String emoji = event.getOption("emoji").getAsString();
        List<FoodItem> list = repos.food().findAll(guild.getId());
        if (checkFood(list, emoji) == -1) {
            repos.food().add(guild.getId(), name, emoji);
            event.reply(String.format("Successfully added %s to the lunch-list with %s as emoji", name, emoji)).queue();
        } else {
            event.reply(String.format("The emoji %s is already part of the lunch-list", emoji)).setEphemeral(true).queue();
        }
    }

    private void remove(SlashCommandInteractionEvent event, Guild guild) {
        Integer index = event.getOption("index", null, OptionMapping::getAsInt);
        String emoji = event.getOption("emoji", null, OptionMapping::getAsString);
        List<FoodItem> list = repos.food().findAll(guild.getId());
        int target;
        if (index != null) {
            if (index < 1 || index > list.size()) {
                event.reply(String.format("The lunch-list only contains %d items", list.size())).setEphemeral(true).queue();
                return;
            }
            target = index - 1;
        } else if (emoji != null) {
            target = checkFood(list, emoji);
            if (target == -1) {
                event.reply(emoji + " is not an emoji on the lunch-list").setEphemeral(true).queue();
                return;
            }
        } else {
            event.reply("Provide an emoji or an index.").setEphemeral(true).queue();
            return;
        }
        FoodItem item = list.get(target);
        repos.food().remove(item.id());
        event.reply(String.format("Successfully removed %s with %s as emoji", item.name(), item.emoji())).queue();
    }

    private void list(SlashCommandInteractionEvent event, Guild guild) {
        List<FoodItem> list = repos.food().findAll(guild.getId());
        EmbedBuilder eb = new EmbedBuilder();
        eb.setColor(Color.ORANGE);
        if (list.isEmpty()) {
            eb.addField("Food", "The lunch-list is empty", true);
        } else {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < list.size(); i++) {
                sb.append(i == 0 ? "" : "\n").append(list.get(i).name()).append(" ").append(list.get(i).emoji());
            }
            eb.addField("Food", sb.toString(), true);
        }
        event.replyEmbeds(eb.build()).queue();
    }

    private void poll(SlashCommandInteractionEvent event, Guild guild) {
        LunchMessager.makeMessage(null, guild, repos);
        event.reply("Lunch poll posted.").setEphemeral(true).queue();
    }

    /**
     * Preserves the legacy {@code FoodHandler.checkFood} contract verbatim, including its
     * off-by-one: returns {@code matchIndex - 1} on a match and {@code -1} when not found
     * (so a match at index 0 also returns -1). Bug fix deferred to roadmap item 4.
     */
    private static int checkFood(List<FoodItem> list, String emoji) {
        int i = 0;
        while (i < list.size() && !list.get(i).emoji().equalsIgnoreCase(emoji)) {
            i++;
        }
        return i < list.size() ? i - 1 : -1;
    }
}
