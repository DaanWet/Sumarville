package Commands.Food;

import Commands.Framework.SlashCommand;
import DataHandlers.FoodHandler;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class FoodCommands implements SlashCommand {

    @Override
    public String getId() {
        return "food";
    }

    @Override
    public List<String> getCommandNames() {
        return List.of("food");
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
        Guild guild = event.getGuild();
        if (guild == null) {
            event.reply("This command only works in a server.").setEphemeral(true).queue();
            return;
        }
        FoodHandler food = new FoodHandler(guild);
        switch (event.getSubcommandName()) {
            case "add" -> add(event, food);
            case "remove" -> remove(event, food);
            case "list" -> list(event, food);
            case "poll" -> poll(event, guild);
            default -> event.reply("Unknown subcommand.").setEphemeral(true).queue();
        }
    }

    private void add(SlashCommandInteractionEvent event, FoodHandler food) {
        String name = event.getOption("name").getAsString();
        String emoji = event.getOption("emoji").getAsString();
        if (food.checkFood(emoji) == -1) {
            food.addFood(name, emoji);
            event.reply(String.format("Successfully added %s to the lunch-list with %s as emoji", name, emoji)).queue();
        } else {
            event.reply(String.format("The emoji %s is already part of the lunch-list", emoji)).setEphemeral(true).queue();
        }
    }

    private void remove(SlashCommandInteractionEvent event, FoodHandler food) {
        Integer index = event.getOption("index", null, OptionMapping::getAsInt);
        String emoji = event.getOption("emoji", null, OptionMapping::getAsString);
        List<Map<String, String>> list = food.getFood();
        int target;
        if (index != null) {
            if (index < 1 || index > list.size()) {
                event.reply(String.format("The lunch-list only contains %d items", list.size())).setEphemeral(true).queue();
                return;
            }
            target = index - 1;
        } else if (emoji != null) {
            target = food.checkFood(emoji);
            if (target == -1) {
                event.reply(emoji + " is not an emoji on the lunch-list").setEphemeral(true).queue();
                return;
            }
        } else {
            event.reply("Provide an emoji or an index.").setEphemeral(true).queue();
            return;
        }
        String name = list.get(target).get("Name");
        String foundEmoji = list.get(target).get("Emoji");
        food.removeFood(target);
        event.reply(String.format("Successfully removed %s with %s as emoji", name, foundEmoji)).queue();
    }

    private void list(SlashCommandInteractionEvent event, FoodHandler food) {
        List<Map<String, String>> list = food.getFood();
        EmbedBuilder eb = new EmbedBuilder();
        eb.setColor(Color.ORANGE);
        if (list.isEmpty()) {
            eb.addField("Food", "The lunch-list is empty", true);
        } else {
            StringBuilder sb = new StringBuilder();
            List<Map<String, String>> items = new ArrayList<>(list);
            for (int i = 0; i < items.size(); i++) {
                sb.append(i == 0 ? "" : "\n").append(items.get(i).get("Name")).append(" ").append(items.get(i).get("Emoji"));
            }
            eb.addField("Food", sb.toString(), true);
        }
        event.replyEmbeds(eb.build()).queue();
    }

    private void poll(SlashCommandInteractionEvent event, Guild guild) {
        LunchMessager.makeMessage(null, guild);
        event.reply("Lunch poll posted.").setEphemeral(true).queue();
    }
}
