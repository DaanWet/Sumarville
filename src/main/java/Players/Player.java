package Players;

import DataHandlers.ConfigHandler;
import net.dv8tion.jda.api.entities.Guild;

import java.awt.Color;

public class Player extends Person {

    public Player(Guild g) {
        resolveOrCreateRole(g, new ConfigHandler(g).getPlayerRoleID(), "Dungeon Delvers", Color.orange, "Player");
    }
}
