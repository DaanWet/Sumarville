package Commands.Framework;

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyCallbackAction;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.mockito.Mockito.*;

class CommandRouterTest {

    @Test
    void handlerExceptionIsCaughtAndUserIsAcknowledged() {
        CommandRegistry registry = new CommandRegistry();
        SlashCommand throwing = mock(SlashCommand.class);
        when(throwing.getId()).thenReturn("boom");
        when(throwing.getCommandData()).thenReturn(List.of(Commands.slash("boom", "throws")));
        doThrow(new RuntimeException("kaboom")).when(throwing).execute(any());
        registry.register(throwing);

        SlashCommandInteractionEvent event = mock(SlashCommandInteractionEvent.class);
        when(event.getName()).thenReturn("boom");
        when(event.isAcknowledged()).thenReturn(false);
        ReplyCallbackAction action = mock(ReplyCallbackAction.class);
        when(event.reply(anyString())).thenReturn(action);
        when(action.setEphemeral(anyBoolean())).thenReturn(action);

        CommandRouter router = new CommandRouter(registry);

        // Must NOT propagate the exception...
        router.onSlashCommandInteraction(event);
        // ...and MUST acknowledge the interaction so Discord doesn't show "interaction failed".
        verify(event).reply(anyString());
    }
}
