package Players;

import DataHandlers.ConfigHandler;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;

import java.awt.Color;

public class DM extends Person {

    public DM(Guild g) {
        String dmId = new ConfigHandler(g).getDMRoleID();
        if (dmId.equals("0")) {
            this.role = g.createRole().setColor(Color.MAGENTA).setName("DM").complete();
            new ConfigHandler(g).setConfig(this.role.getId(), "DM");
        } else {
            this.role = g.getRoleById(dmId);
        }
    }

    public boolean isHeldBy(Member member) {
        return member != null && role != null && member.getRoles().contains(role);
    }
}
