package Commands.Framework;

import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

public class CommandRouter extends ListenerAdapter {

    private final CommandRegistry registry;

    public CommandRouter(CommandRegistry registry) {
        this.registry = registry;
    }

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        SlashCommand cmd = registry.byCommandName(event.getName());
        if (cmd != null) {
            cmd.execute(event);
        }
    }

    @Override
    public void onButtonInteraction(ButtonInteractionEvent event) {
        SlashCommand cmd = registry.byComponentId(event.getComponentId());
        if (cmd != null) {
            cmd.onButton(event);
        }
    }

    @Override
    public void onModalInteraction(ModalInteractionEvent event) {
        SlashCommand cmd = registry.byComponentId(event.getModalId());
        if (cmd != null) {
            cmd.onModal(event);
        }
    }
}
