package Commands.Framework;

import io.sentry.Sentry;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.callbacks.IReplyCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CommandRouter extends ListenerAdapter {

    private static final Logger log = LoggerFactory.getLogger(CommandRouter.class);

    private final CommandRegistry registry;

    public CommandRouter(CommandRegistry registry) {
        this.registry = registry;
    }

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        SlashCommand cmd = registry.byCommandName(event.getName());
        if (cmd != null) {
            dispatch(event, () -> cmd.execute(event), "slash /" + event.getName());
        }
    }

    @Override
    public void onButtonInteraction(ButtonInteractionEvent event) {
        SlashCommand cmd = registry.byComponentId(event.getComponentId());
        if (cmd != null) {
            dispatch(event, () -> cmd.onButton(event), "button " + event.getComponentId());
        }
    }

    @Override
    public void onModalInteraction(ModalInteractionEvent event) {
        SlashCommand cmd = registry.byComponentId(event.getModalId());
        if (cmd != null) {
            dispatch(event, () -> cmd.onModal(event), "modal " + event.getModalId());
        }
    }

    /** Runs a handler and guarantees the interaction is answered even if the handler throws. */
    private void dispatch(IReplyCallback event, Runnable handler, String label) {
        Sentry.configureScope(scope -> {
            scope.setTag("command", label);
            Guild guild = event.getGuild();
            if (guild != null) {
                scope.setTag("guild", guild.getId());
            } else {
                scope.removeTag("guild");
            }
        });
        try {
            handler.run();
        } catch (RuntimeException e) {
            log.error("Unhandled error while handling {}", label, e);
            String msg = "Something went wrong while handling that.";
            if (event.isAcknowledged()) {
                event.getHook().sendMessage(msg).setEphemeral(true).queue();
            } else {
                event.reply(msg).setEphemeral(true).queue();
            }
        }
    }
}
