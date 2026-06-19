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
}
