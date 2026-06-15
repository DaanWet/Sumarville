package Players;

import DataHandlers.ConfigHandler;
import net.dv8tion.jda.api.entities.Guild;

import java.awt.Color;

public class Player extends Person {

    public Player(Guild g) {
        String playerId = new ConfigHandler(g).getPlayerRoleID();
        if (playerId.equals("0")) {
            // .complete() blocks until the role exists, fixing the old async NPE/role-spam race.
            this.role = g.createRole().setColor(Color.orange).setName("Dungeon Delvers").complete();
            new ConfigHandler(g).setConfig(this.role.getId(), "Player");
        } else {
            this.role = g.getRoleById(playerId);
        }
    }
}
