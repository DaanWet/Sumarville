package Players;

import Database.ConfigRepository;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;

import java.awt.Color;

public class DM extends Person {

    public DM(Guild g, ConfigRepository config) {
        resolveOrCreateRole(g, config, "DM", "DM", Color.MAGENTA);
    }

    public boolean isHeldBy(Member member) {
        return member != null && role != null && member.getRoles().contains(role);
    }

    /** Read-only DM check: true iff a DM role is configured and {@code member} has it. Never creates the role. */
    public static boolean isHeldBy(Member member, Guild g, ConfigRepository config) {
        if (member == null) {
            return false;
        }
        return resolveRole(g, config, "DM").map(role -> member.getRoles().contains(role)).orElse(false);
    }
}
