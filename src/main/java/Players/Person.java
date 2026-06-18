package Players;

import DataHandlers.ConfigHandler;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Role;

import java.awt.Color;

public abstract class Person {

    protected Role role;

    public Role getRole(){
        return role;
    }

    /**
     * Resolves the configured role, (re)creating it when it is missing — i.e. never configured
     * ("0") or configured but since deleted from the guild (so {@code getRoleById} returns null).
     * The blocking {@code .complete()} is a one-time bootstrap cost: the new id is persisted
     * immediately, so later constructions are cheap cache reads. That one-off is why it is
     * acceptable here even though the constructor runs on the interaction thread.
     */
    protected void resolveOrCreateRole(Guild g, String configId, String name, Color color, String configKey) {
        Role existing = configId.equals("0") ? null : g.getRoleById(configId);
        if (existing != null) {
            this.role = existing;
        } else {
            this.role = g.createRole().setColor(color).setName(name).complete();
            new ConfigHandler(g).setConfig(this.role.getId(), configKey);
        }
    }
}
