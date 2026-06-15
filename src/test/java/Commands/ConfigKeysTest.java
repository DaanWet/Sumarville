package Commands;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ConfigKeysTest {

    @Test
    void roleKeysAreRecognised() {
        assertTrue(ConfigKeys.isRoleKey("DM"));
        assertTrue(ConfigKeys.isRoleKey("Player"));
        assertFalse(ConfigKeys.isRoleKey("FoodChannel"));
    }

    @Test
    void channelKeysAreRecognised() {
        assertTrue(ConfigKeys.isChannelKey("DMChannel"));
        assertTrue(ConfigKeys.isChannelKey("CalendarChannel"));
        assertTrue(ConfigKeys.isChannelKey("FoodChannel"));
        assertTrue(ConfigKeys.isChannelKey("MemeChannel"));
        assertFalse(ConfigKeys.isChannelKey("DM"));
    }
}
