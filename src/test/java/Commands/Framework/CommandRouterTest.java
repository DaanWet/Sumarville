package Commands.Framework;

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyCallbackAction;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.entities.Guild;
import io.sentry.Sentry;
import Observability.Observability;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.AfterEach;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.*;

class CommandRouterTest {

    @AfterEach
    void resetSentry() {
        Sentry.close();
    }

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

    @Test
    void dispatchTagsSentryScopeWithGuildAndCommand() {
        Observability.initSentry("https://public@example.com/1", "test");

        CommandRegistry registry = new CommandRegistry();
        SlashCommand ok = mock(SlashCommand.class);
        when(ok.getId()).thenReturn("ping");
        when(ok.getCommandData()).thenReturn(List.of(Commands.slash("ping", "pings")));
        registry.register(ok);

        SlashCommandInteractionEvent event = mock(SlashCommandInteractionEvent.class);
        when(event.getName()).thenReturn("ping");
        Guild guild = mock(Guild.class);
        when(guild.getId()).thenReturn("42");
        when(event.getGuild()).thenReturn(guild);

        new CommandRouter(registry).onSlashCommandInteraction(event);

        AtomicReference<Map<String, String>> tags = new AtomicReference<>();
        Sentry.configureScope(scope -> tags.set(scope.getTags()));
        assertEquals("42", tags.get().get("guild"));
        assertEquals("slash /ping", tags.get().get("command"));
    }

    @Test
    void dispatchClearsStaleGuildTagWhenSubsequentEventHasNoGuild() {
        Observability.initSentry("https://public@example.com/1", "test");

        CommandRegistry registry = new CommandRegistry();
        SlashCommand ok = mock(SlashCommand.class);
        when(ok.getId()).thenReturn("ping");
        when(ok.getCommandData()).thenReturn(List.of(Commands.slash("ping", "pings")));
        registry.register(ok);

        CommandRouter router = new CommandRouter(registry);

        SlashCommandInteractionEvent guildEvent = mock(SlashCommandInteractionEvent.class);
        when(guildEvent.getName()).thenReturn("ping");
        Guild guild = mock(Guild.class);
        when(guild.getId()).thenReturn("42");
        when(guildEvent.getGuild()).thenReturn(guild);

        router.onSlashCommandInteraction(guildEvent);

        AtomicReference<Map<String, String>> afterGuild = new AtomicReference<>();
        Sentry.configureScope(scope -> afterGuild.set(scope.getTags()));
        assertEquals("42", afterGuild.get().get("guild"));

        SlashCommandInteractionEvent dmEvent = mock(SlashCommandInteractionEvent.class);
        when(dmEvent.getName()).thenReturn("ping");
        when(dmEvent.getGuild()).thenReturn(null);

        router.onSlashCommandInteraction(dmEvent);

        AtomicReference<Map<String, String>> afterDm = new AtomicReference<>();
        Sentry.configureScope(scope -> afterDm.set(scope.getTags()));
        assertNull(afterDm.get().get("guild"));
    }
}
