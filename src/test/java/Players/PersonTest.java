package Players;

import Database.ConfigRepository;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PersonTest {

    @Test
    void readOnlyDmCheck_trueWhenMemberHasConfiguredRole_andNeverCreates() {
        Guild guild = mock(Guild.class);
        ConfigRepository config = mock(ConfigRepository.class);
        Role role = mock(Role.class);
        Member member = mock(Member.class);
        when(guild.getId()).thenReturn("g1");
        when(config.get("g1", "DM")).thenReturn(Optional.of("role123"));
        when(guild.getRoleById("role123")).thenReturn(role);
        when(member.getRoles()).thenReturn(List.of(role));

        assertTrue(DM.isHeldBy(member, guild, config));
        verify(guild, never()).createRole();   // the membership CHECK must not create the role
    }

    @Test
    void readOnlyDmCheck_falseWhenNoRoleConfigured_andNeverCreates() {
        Guild guild = mock(Guild.class);
        ConfigRepository config = mock(ConfigRepository.class);
        Member member = mock(Member.class);
        when(guild.getId()).thenReturn("g1");
        when(config.get("g1", "DM")).thenReturn(Optional.empty());

        assertFalse(DM.isHeldBy(member, guild, config));
        verify(guild, never()).createRole();
    }

    @Test
    void readOnlyDmCheck_falseWhenConfiguredRoleWasDeleted() {
        Guild guild = mock(Guild.class);
        ConfigRepository config = mock(ConfigRepository.class);
        Member member = mock(Member.class);
        when(guild.getId()).thenReturn("g1");
        when(config.get("g1", "DM")).thenReturn(Optional.of("gone"));
        when(guild.getRoleById("gone")).thenReturn(null);

        assertFalse(DM.isHeldBy(member, guild, config));
        verify(guild, never()).createRole();
    }

    @Test
    void readOnlyDmCheck_falseForNullMember() {
        Guild guild = mock(Guild.class);
        ConfigRepository config = mock(ConfigRepository.class);
        assertFalse(DM.isHeldBy(null, guild, config));
    }
}
