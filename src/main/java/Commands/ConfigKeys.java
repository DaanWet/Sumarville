package Commands;

import java.util.Set;

public final class ConfigKeys {

    public static final Set<String> ROLE_KEYS = Set.of("DM", "Player");
    public static final Set<String> CHANNEL_KEYS = Set.of("DMChannel", "CalendarChannel", "FoodChannel", "MemeChannel");

    private ConfigKeys() {
    }

    public static boolean isRoleKey(String key) {
        return ROLE_KEYS.contains(key);
    }

    public static boolean isChannelKey(String key) {
        return CHANNEL_KEYS.contains(key);
    }
}
