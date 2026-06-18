package Players;

import DataHandlers.ConfigHandler;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;

import java.awt.Color;

public class DM extends Person {

    public DM(Guild g) {
        resolveOrCreateRole(g, new ConfigHandler(g).getDMRoleID(), "DM", Color.MAGENTA, "DM");
    }

    public boolean isHeldBy(Member member) {
        return member != null && role != null && member.getRoles().contains(role);
    }
}
