package Players;

import Database.ConfigRepository;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Role;

import java.awt.Color;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public abstract class Person {

    /** Per (guild, role-key) monitors so concurrent first-use can't create duplicate roles. */
    private static final ConcurrentMap<String, Object> CREATE_LOCKS = new ConcurrentHashMap<>();

    protected Role role;

    public Role getRole() {
        return role;
    }

    /** Read-only: the configured role, or empty when never configured, stored as "0", or since deleted. Never creates. */
    protected static Optional<Role> resolveRole(Guild g, ConfigRepository config, String configKey) {
        return config.get(g.getId(), configKey)
                .filter(id -> !id.equals("0"))
                .map(g::getRoleById)
                .filter(Objects::nonNull);
    }

    /**
     * Resolves the configured role, (re)creating it when missing. The blocking {@code .complete()} is a
     * one-time bootstrap cost; the new id is persisted immediately. Serialized per (guild, key) so two
     * concurrent first-uses don't each create a duplicate role.
     */
    protected void resolveOrCreateRole(Guild g, ConfigRepository config, String configKey, String name, Color color) {
        Object lock = CREATE_LOCKS.computeIfAbsent(g.getId() + "|" + configKey, k -> new Object());
        synchronized (lock) {
            Optional<Role> existing = resolveRole(g, config, configKey);
            if (existing.isPresent()) {
                this.role = existing.get();
            } else {
                this.role = g.createRole().setColor(color).setName(name).complete();
                config.set(g.getId(), configKey, this.role.getId());
            }
        }
    }
}
