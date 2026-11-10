package Commands.Users;

import Commands.Framework.SlashCommand;
import Database.Repositories;
import Players.DM;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;

import java.util.List;

public class DmCommands implements SlashCommand {

    private final Repositories repos;

    public DmCommands(Repositories repos) {
        this.repos = repos;
    }

    @Override
    public String getId() {
        return "dm";
    }

    @Override
    public List<SlashCommandData> getCommandData() {
        return List.of(Commands.slash("dm", "Claim or pass the Dungeon Master role")
                .addSubcommands(
                        new SubcommandData("claim", "Take the DM role (needs the Manage Roles permission)"),
                        new SubcommandData("pass", "Pass the DM role to someone else")
                                .addOptions(new OptionData(OptionType.USER, "user", "New Dungeon Master", true))));
    }

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        Guild guild = event.getGuild();
        Member member = event.getMember();
        if (guild == null || member == null) {
            event.reply("This command only works in a server.").setEphemeral(true).queue();
            return;
        }
        Role dmRole = new DM(guild, repos.config()).getRole();

        String sub = event.getSubcommandName();
        if (sub == null) {
            event.reply("Unknown subcommand.").setEphemeral(true).queue();
            return;
        }
        // The bot has no GUILD_MEMBERS intent, so it cannot list who holds the DM role. It only looks at
        // the caller, whose roles and permissions arrive with the interaction.
        if (sub.equals("claim")) {
            if (member.hasPermission(Permission.MANAGE_ROLES)) {
                guild.addRoleToMember(member, dmRole).queue();
                event.reply("You are now the Dungeon Master.").queue();
            } else {
                event.reply("Only members with the Manage Roles permission can claim the DM role. "
                        + "Ask the current DM to use /dm pass.").setEphemeral(true).queue();
            }
            return;
        }

        // pass
        if (!member.getRoles().contains(dmRole)) {
            event.reply("You are not the DM, so you can't pass the role.").setEphemeral(true).queue();
            return;
        }
        Member newDm = event.getOption("user").getAsMember();
        if (newDm == null) {
            event.reply("That user is not part of this server.").setEphemeral(true).queue();
            return;
        }
        guild.addRoleToMember(newDm, dmRole).queue();
        guild.removeRoleFromMember(member, dmRole).queue();
        event.reply("Passed the Dungeon Master role to " + newDm.getEffectiveName()).queue();
    }
}
