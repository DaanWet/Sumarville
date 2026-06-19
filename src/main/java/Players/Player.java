package Players;

import Database.ConfigRepository;
import net.dv8tion.jda.api.entities.Guild;

import java.awt.Color;

public class Player extends Person {

    public Player(Guild g, ConfigRepository config) {
        resolveOrCreateRole(g, config, "Player", "Dungeon Delvers", Color.orange);
    }
}
