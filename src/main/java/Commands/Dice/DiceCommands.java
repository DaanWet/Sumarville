package Commands.Dice;

import Commands.Framework.SlashCommand;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

public class DiceCommands implements SlashCommand {

    private static final int[] DICE = {4, 6, 8, 10, 12, 20, 100};
    private static final int MAX_ROLLS = 50;

    private final DiceRoller roller = new DiceRoller();

    @Override
    public String getId() {
        return "dice";
    }

    @Override
    public List<String> getCommandNames() {
        List<String> names = new ArrayList<>();
        for (int s : DICE) {
            names.add("d" + s);
        }
        names.add("roll");
        return names;
    }

    @Override
    public List<SlashCommandData> getCommandData() {
        List<SlashCommandData> data = new ArrayList<>();
        for (int s : DICE) {
            data.add(Commands.slash("d" + s, "Rolls a d" + s)
                    .addOptions(amountOption(), modeOption()));
        }
        data.add(Commands.slash("roll", "Rolls a die with a custom number of sides")
                .addOptions(
                        new OptionData(OptionType.INTEGER, "sides", "Number of sides", true)
                                .setRequiredRange(2, 1000),
                        amountOption(), modeOption()));
        return data;
    }

    private OptionData amountOption() {
        return new OptionData(OptionType.INTEGER, "amount", "How many times to roll (max 50)")
                .setRequiredRange(1, MAX_ROLLS);
    }

    private OptionData modeOption() {
        return new OptionData(OptionType.STRING, "mode", "Advantage or disadvantage")
                .addChoice("Advantage", "adv")
                .addChoice("Disadvantage", "dis");
    }

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        int sides = event.getName().equals("roll")
                ? event.getOption("sides").getAsInt()
                : Integer.parseInt(event.getName().substring(1));

        String mode = event.getOption("mode", null, OptionMapping::getAsString);
        int amount = event.getOption("amount", 1, OptionMapping::getAsInt);

        String name = event.getMember() != null ? event.getMember().getEffectiveName()
                : event.getUser().getName();
        EmbedBuilder eb = new EmbedBuilder();
        eb.setColor(Color.ORANGE);
        eb.setThumbnail(imageFor(sides));

        if (mode != null) {
            int first = roller.roll(sides);
            int second = roller.roll(sides);
            boolean adv = mode.equals("adv");
            int chosen = adv ? Math.max(first, second) : Math.min(first, second);
            eb.addField(
                    String.format("%s rolled a d%d with %svantage and got:", name, sides, adv ? "ad" : "disad"),
                    String.format("%d & %d => %d", first, second, chosen),
                    true);
        } else if (amount > 1) {
            int[] rolls = roller.rollMany(sides, amount);
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < rolls.length; i++) {
                sb.append(i == 0 ? "" : " + ").append(rolls[i]);
            }
            sb.append(" = ").append(roller.sum(rolls));
            eb.addField(String.format("%s rolled %d times a d%d and got:", name, amount, sides), sb.toString(), true);
        } else {
            eb.addField(String.format("%s rolled a d%d and got:", name, sides),
                    Integer.toString(roller.roll(sides)), true);
        }
        event.replyEmbeds(eb.build()).queue();
    }

    private String imageFor(int sides) {
        if (sides == 100) {
            return "https://www.dnddice.com/media/wysiwyg/d10_.jpg";
        }
        if (sides == 4 || sides == 6 || sides == 8 || sides == 10 || sides == 12 || sides == 20) {
            return "https://www.dnddice.com/media/wysiwyg/d" + sides + ".jpg";
        }
        return null;
    }
}
