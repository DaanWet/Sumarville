package Commands.Framework;

import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CommandRegistry {

    private final List<SlashCommand> commands = new ArrayList<>();
    private final Map<String, SlashCommand> byCommandName = new HashMap<>();
    private final Map<String, SlashCommand> byId = new HashMap<>();

    public void register(SlashCommand cmd) {
        commands.add(cmd);
        byId.put(cmd.getId(), cmd);
        for (String name : cmd.getCommandNames()) {
            byCommandName.put(name, cmd);
        }
    }

    public List<SlashCommand> all() {
        return commands;
    }

    public SlashCommand byCommandName(String name) {
        return byCommandName.get(name);
    }

    public SlashCommand byComponentId(String componentId) {
        int sep = componentId.indexOf(':');
        String ns = sep == -1 ? componentId : componentId.substring(0, sep);
        return byId.get(ns);
    }

    public List<SlashCommandData> allCommandData() {
        List<SlashCommandData> data = new ArrayList<>();
        for (SlashCommand c : commands) {
            data.addAll(c.getCommandData());
        }
        return data;
    }
}
