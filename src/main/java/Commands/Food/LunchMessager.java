package Commands.Food;

import DataHandlers.ConfigHandler;
import DataHandlers.FoodHandler;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.emoji.Emoji;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class LunchMessager {

    private static final DateTimeFormatter sdf = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    // Keyed per (guild, date) so two guilds with a session on the same day no longer collide.
    private static final Map<String, ScheduledFuture<?>> map = new HashMap<>();

    private static String key(String guildId, LocalDateTime date) {
        return guildId + "|" + (date == null ? "now" : date.format(sdf));
    }

    public static void makeMessage(LocalDateTime date, Guild g) {
        LocalDateTime messagedate = (date == null) ? LocalDateTime.now() : date;
        long diff = ChronoUnit.MILLIS.between(LocalDateTime.now(), messagedate);
        TextChannel ch = g.getTextChannelById(new ConfigHandler(g).getChannel("FoodChannel"));
        if (ch == null) {
            return;
        }
        ScheduledFuture<?> task = ch.sendMessage(getFood(date, g)).queueAfter(
                diff > 0 ? diff : 0, TimeUnit.MILLISECONDS,
                message -> getEmojis(g).forEach(s -> message.addReaction(Emoji.fromFormatted(s)).queue()));
        map.put(key(g.getId(), date), task);
    }

    public static void cancelMessage(String guildId, LocalDateTime date) {
        String k = key(guildId, date);
        ScheduledFuture<?> task = map.remove(k);
        if (task != null) {
            task.cancel(false);
        }
    }

    private static String getFood(LocalDateTime date, Guild g) {
        FoodHandler f = new FoodHandler(g);
        StringBuilder sb = new StringBuilder();
        sb.append("Wat eten we ?").append(date != null ? " Session: " + sdf.format(date) : "");
        f.getFood().forEach(food -> sb.append("\n").append(food.get("Name")).append(": ").append(food.get("Emoji")));
        return sb.toString();
    }

    private static ArrayList<String> getEmojis(Guild g) {
        FoodHandler f = new FoodHandler(g);
        ArrayList<String> emoji = new ArrayList<>();
        f.getFood().forEach(s -> emoji.add(s.get("Emoji")));
        return emoji;
    }
}
