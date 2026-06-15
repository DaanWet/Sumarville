package Commands.Framework;

import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;

import java.util.List;

/** A unit of slash-command behaviour. Components (buttons/modals) it owns must use ids
 *  prefixed with {@code getId() + ":"} so the router can dispatch them back here. */
public interface SlashCommand {

    /** Stable namespace for this command's component ids (e.g. "character"). */
    String getId();

    /** Registration payload (one entry per top-level command). */
    List<SlashCommandData> getCommandData();

    void execute(SlashCommandInteractionEvent event);

    default void onButton(ButtonInteractionEvent event) {}

    default void onModal(ModalInteractionEvent event) {}
}
