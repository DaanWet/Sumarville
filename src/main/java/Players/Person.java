package Players;

import Database.ConfigRepository;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Role;

import java.awt.Color;
import java.util.Optional;

public abstract class Person {

    protected Role role;

    public Role getRole() {
        return role;
    }

    /**
     * Resolves the configured role, (re)creating it when it is missing — never configured,
     * stored as the legacy "0", or configured but since deleted. The blocking
     * {@code .complete()} is a one-time bootstrap cost; the new id is persisted immediately.
     */
    protected void resolveOrCreateRole(Guild g, ConfigRepository config, String configKey, String name, Color color) {
        Optional<String> configId = config.get(g.getId(), configKey).filter(id -> !id.equals("0"));
        Role existing = configId.map(g::getRoleById).orElse(null);
        if (existing != null) {
            this.role = existing;
        } else {
            this.role = g.createRole().setColor(color).setName(name).complete();
            config.set(g.getId(), configKey, this.role.getId());
        }
    }
}
