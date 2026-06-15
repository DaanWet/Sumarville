package Commands;

import Commands.Framework.SlashCommand;
import DataHandlers.ConfigHandler;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;

import java.util.List;

public class ConfigCommands implements SlashCommand {

    @Override
    public String getId() {
        return "config";
    }

    @Override
    public List<String> getCommandNames() {
        return List.of("config");
    }

    @Override
    public List<SlashCommandData> getCommandData() {
        OptionData roleSetting = new OptionData(OptionType.STRING, "setting", "Which role to set", true)
                .addChoice("DM", "DM")
                .addChoice("Player", "Player");
        OptionData channelSetting = new OptionData(OptionType.STRING, "setting", "Which channel to set", true)
                .addChoice("DM channel", "DMChannel")
                .addChoice("Calendar channel", "CalendarChannel")
                .addChoice("Food channel", "FoodChannel")
                .addChoice("Meme channel", "MemeChannel");
        return List.of(Commands.slash("config", "Configure Sumarville")
                .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.MANAGE_SERVER))
                .addSubcommands(
                        new SubcommandData("role", "Map a server role to a Sumarville role")
                                .addOptions(roleSetting,
                                        new OptionData(OptionType.ROLE, "value", "The role", true)),
                        new SubcommandData("channel", "Map a channel to a Sumarville function")
                                .addOptions(channelSetting,
                                        new OptionData(OptionType.CHANNEL, "value", "The channel", true))));
    }

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        Guild guild = event.getGuild();
        if (guild == null) {
            event.reply("This command only works in a server.").setEphemeral(true).queue();
            return;
        }
        ConfigHandler config = new ConfigHandler(guild);
        String setting = event.getOption("setting").getAsString();

        if (event.getSubcommandName().equals("role")) {
            if (!ConfigKeys.isRoleKey(setting)) {
                event.reply("Unknown role setting.").setEphemeral(true).queue();
                return;
            }
            Role role = event.getOption("value").getAsRole();
            config.setConfig(role.getId(), setting);
            event.reply(String.format("Set the %s role to %s", setting, role.getName())).queue();
        } else {
            if (!ConfigKeys.isChannelKey(setting)) {
                event.reply("Unknown channel setting.").setEphemeral(true).queue();
                return;
            }
            GuildChannel channel = event.getOption("value").getAsChannel();
            config.setConfig(channel.getId(), setting);
            event.reply(String.format("Set the %s to %s", setting, channel.getName())).queue();
        }
    }
}
