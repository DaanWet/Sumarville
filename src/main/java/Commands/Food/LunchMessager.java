package Commands.Food;

import Database.Repositories;
import Domain.FoodItem;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.emoji.Emoji;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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

    public static void makeMessage(LocalDateTime date, Guild g, Repositories repos) {
        LocalDateTime messagedate = (date == null) ? LocalDateTime.now() : date;
        long diff = ChronoUnit.MILLIS.between(LocalDateTime.now(), messagedate);
        TextChannel ch = repos.config().get(g.getId(), "FoodChannel").map(g::getTextChannelById).orElse(null);
        if (ch == null) {
            return;
        }
        List<FoodItem> foodList = repos.food().findAll(g.getId());
        List<String> emojis = new ArrayList<>();
        foodList.forEach(f -> emojis.add(f.emoji()));
        ScheduledFuture<?> task = ch.sendMessage(buildText(date, foodList)).queueAfter(
                diff > 0 ? diff : 0, TimeUnit.MILLISECONDS,
                message -> emojis.forEach(s -> message.addReaction(Emoji.fromFormatted(s)).queue()));
        map.put(key(g.getId(), date), task);
    }

    private static String buildText(LocalDateTime date, List<FoodItem> foodList) {
        StringBuilder sb = new StringBuilder();
        sb.append("Wat eten we ?").append(date != null ? " Session: " + sdf.format(date) : "");
        foodList.forEach(food -> sb.append("\n").append(food.name()).append(": ").append(food.emoji()));
        return sb.toString();
    }

    public static void cancelMessage(String guildId, LocalDateTime date) {
        String k = key(guildId, date);
        ScheduledFuture<?> task = map.remove(k);
        if (task != null) {
            task.cancel(false);
        }
    }
}
