package Retention;

import Commands.Calendar.SessionReminder;
import Commands.Food.LunchMessager;
import net.dv8tion.jda.api.events.guild.GuildJoinEvent;
import net.dv8tion.jda.api.events.guild.GuildLeaveEvent;
import net.dv8tion.jda.api.events.guild.UnavailableGuildLeaveEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

/** Feeds live join/leave events into {@link GuildRetention}; downtime is covered by its reconcile. */
public class GuildRetentionListener extends ListenerAdapter {

    private final GuildRetention retention;

    public GuildRetentionListener(GuildRetention retention) {
        this.retention = retention;
    }

    @Override
    public void onGuildLeave(GuildLeaveEvent event) {
        left(event.getGuild().getId());
    }

    @Override
    public void onUnavailableGuildLeave(UnavailableGuildLeaveEvent event) {
        left(event.getGuildId());
    }

    @Override
    public void onGuildJoin(GuildJoinEvent event) {
        retention.onJoined(event.getGuild().getId());
    }

    private void left(String guildId) {
        SessionReminder.cancelAll(guildId);
        LunchMessager.cancelAll(guildId);
        retention.onLeft(guildId);
    }
}
