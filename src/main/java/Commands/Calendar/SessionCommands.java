package Commands.Calendar;

import Commands.Framework.Interactions;
import Commands.Framework.SlashCommand;
import DataHandlers.CalendarHandler;
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
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

public class SessionCommands implements SlashCommand {

    private final SessionDateParser dates = new SessionDateParser();

    @Override
    public String getId() {
        return "session";
    }

    @Override
    public List<SlashCommandData> getCommandData() {
        return List.of(Commands.slash("session", "Manage D&D sessions")
                .addSubcommands(
                        new SubcommandData("add", "Add a session to the calendar")
                                .addOptions(new OptionData(OptionType.STRING, "date", "Date as dd/MM/yyyy", true)),
                        new SubcommandData("remove", "Remove a session")
                                .addOptions(
                                        new OptionData(OptionType.STRING, "date", "Date as dd/MM/yyyy"),
                                        new OptionData(OptionType.INTEGER, "index", "1-based index in the list"),
                                        new OptionData(OptionType.BOOLEAN, "all", "Remove every session")),
                        new SubcommandData("list", "Show the calendar")));
    }

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        Guild guild = Interactions.requireGuild(event);
        if (guild == null) return;
        CalendarHandler calendar = new CalendarHandler(guild);
        switch (event.getSubcommandName()) {
            case "add" -> add(event, guild, calendar);
            case "remove" -> remove(event, guild, calendar);
            case "list" -> list(event, calendar);
            default -> event.reply("Unknown subcommand.").setEphemeral(true).queue();
        }
    }

    private void add(SlashCommandInteractionEvent event, Guild guild, CalendarHandler calendar) {
        String raw = event.getOption("date").getAsString();
        LocalDateTime date;
        try {
            date = dates.parse(raw);
        } catch (DateTimeParseException ex) {
            event.reply("Usage: /session add date:dd/MM/yyyy").setEphemeral(true).queue();
            return;
        }
        if (!dates.isInFuture(date, LocalDateTime.now())) {
            event.reply("You cannot plan a session in the past.").setEphemeral(true).queue();
            return;
        }
        if (calendar.getSessions(false).contains(date)) {
            event.reply("There already is a session planned on " + raw).setEphemeral(true).queue();
            return;
        }
        calendar.addSession(date);
        SessionReminder.makeMessage(date, guild);
        event.reply("Successfully added a session on " + raw).queue();
    }

    private void remove(SlashCommandInteractionEvent event, Guild guild, CalendarHandler calendar) {
        boolean all = event.getOption("all", false, OptionMapping::getAsBoolean);
        if (all) {
            new ArrayList<>(calendar.getSessions(true)).forEach(d -> {
                calendar.removeSession(d);
                SessionReminder.cancelSession(guild.getId(), d);
            });
            event.reply("Successfully removed all sessions.").queue();
            return;
        }
        Integer index = event.getOption("index", null, OptionMapping::getAsInt);
        String raw = event.getOption("date", null, OptionMapping::getAsString);

        LocalDateTime target = null;
        if (raw != null) {
            try {
                target = dates.parse(raw);
            } catch (DateTimeParseException ex) {
                event.reply("Usage: /session remove date:dd/MM/yyyy").setEphemeral(true).queue();
                return;
            }
            if (!calendar.getSessions(true).contains(target)) {
                event.reply("There is no session on " + raw).setEphemeral(true).queue();
                return;
            }
        } else if (index != null) {
            List<LocalDateTime> upcoming = calendar.getSessions(false);
            if (index < 1 || index > upcoming.size()) {
                event.reply("There is no session #" + index).setEphemeral(true).queue();
                return;
            }
            target = upcoming.get(index - 1);
        } else {
            event.reply("Provide a date, an index, or all:true.").setEphemeral(true).queue();
            return;
        }
        calendar.removeSession(target);
        SessionReminder.cancelSession(guild.getId(), target);
        event.reply("Successfully removed the session on " + dates.format(target)).queue();
    }

    private void list(SlashCommandInteractionEvent event, CalendarHandler calendar) {
        List<LocalDateTime> sessions = calendar.getSessions(false);
        EmbedBuilder eb = new EmbedBuilder();
        eb.setColor(Color.ORANGE);
        eb.addField("Next Session:", sessions.isEmpty() ? "No planned sessions yet" : dates.format(sessions.get(0)), true);
        if (sessions.size() > 1) {
            StringBuilder sb = new StringBuilder();
            for (int i = 1; i < sessions.size(); i++) {
                sb.append(i == 1 ? "" : "\n").append(dates.format(sessions.get(i)));
            }
            eb.addField("Planned Sessions:", sb.toString(), false);
        }
        event.replyEmbeds(eb.build()).queue();
    }
}
