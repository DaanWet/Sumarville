package Commands.Calendar;

import Commands.Food.LunchMessager;
import DataHandlers.CalendarHandler;
import DataHandlers.CharacterHandler;
import DataHandlers.ConfigHandler;
import DataHandlers.NPCMessageHandler;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class SessionReminder {

    private static final DateTimeFormatter sdf = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final Map<String, ScheduledFuture<?>> map = new HashMap<>();
    private static final Random random = new Random();

    private static String key(String guildId, LocalDateTime date) {
        return guildId + "|" + date.format(sdf);
    }

    public static void makeMessage(LocalDateTime date, Guild g) {
        LocalDateTime messageDate = date.minusHours(6);
        long diff = ChronoUnit.MILLIS.between(LocalDateTime.now(), messageDate);

        NPCMessageHandler npch = new NPCMessageHandler(g);
        ArrayList<Map<String, String>> messages = npch.getSpecificMessages();
        if (messages.isEmpty()) {
            messages = npch.getBasicMessages();
        }
        int i = !messages.isEmpty() ? random.nextInt(messages.size()) : -1;
        EmbedBuilder eb = new EmbedBuilder();
        String name = i != -1 ? messages.get(i).get("npc") : "";
        String custommessage = i != -1 ? messages.get(i).get("message") : "It's time to go on an adventure!";
        eb.addField(custommessage, "Don't forget our session tomorrow!", false);

        CharacterHandler chh = new CharacterHandler(g);
        String pic = name.isEmpty() ? "" : chh.getPicture(name, "name");
        if (!name.isEmpty() && !pic.isEmpty()) {
            eb.setAuthor(name, pic, pic);
        } else {
            eb.setAuthor(!name.isEmpty() ? name : g.getSelfMember().getEffectiveName());
        }

        TextChannel ch = g.getTextChannelById(new ConfigHandler(g).getChannel("CalendarChannel"));
        if (ch == null) {
            return;
        }
        ScheduledFuture<?> task = ch.sendMessageEmbeds(eb.build()).queueAfter(
                diff > 0 ? diff : 0, TimeUnit.MILLISECONDS,
                message -> {
                    LunchMessager.makeMessage(date, g);
                    npch.clearSpecific();
                });
        map.put(key(g.getId(), date), task);
    }

    public static void cancelSession(String guildId, LocalDateTime date) {
        String k = key(guildId, date);
        ScheduledFuture<?> task = map.remove(k);
        if (task != null) {
            task.cancel(false);
        }
    }

    public static void onRestart(Guild g) {
        CalendarHandler calendarHandler = new CalendarHandler(g);
        ArrayList<LocalDateTime> sessions = calendarHandler.getSessions(false);
        sessions.forEach(session -> {
            LocalDateTime messageDate = session.minusHours(4);
            long diff = ChronoUnit.MILLIS.between(LocalDateTime.now(), messageDate);
            long difffood = ChronoUnit.MILLIS.between(LocalDateTime.now(), session);
            if (diff > 0) {
                makeMessage(session, g);
            } else if (difffood > 0) {
                LunchMessager.makeMessage(session, g);
            }
        });
    }
}
