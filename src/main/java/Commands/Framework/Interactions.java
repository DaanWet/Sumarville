package Commands.Framework;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.interactions.callbacks.IReplyCallback;

public final class Interactions {
    private Interactions() {}

    /** Replies ephemerally and returns null if the interaction is not in a guild. */
    public static Guild requireGuild(IReplyCallback event) {
        Guild guild = event.getGuild();
        if (guild == null) {
            event.reply("This command only works in a server.").setEphemeral(true).queue();
        }
        return guild;
    }
}
