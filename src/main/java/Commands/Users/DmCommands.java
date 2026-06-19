package Commands.Users;

import Commands.Framework.SlashCommand;
import Database.Repositories;
import Players.DM;
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
                        new SubcommandData("claim", "Claim the DM role if nobody has it"),
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
        List<Member> currentDms = guild.getMembersWithRoles(dmRole);

        if (event.getSubcommandName().equals("claim")) {
            if (currentDms.isEmpty()) {
                guild.addRoleToMember(member, dmRole).queue();
                event.reply("You are now the Dungeon Master.").queue();
            } else {
                event.reply("There already is a Dungeon Master.").setEphemeral(true).queue();
            }
            return;
        }

        // pass
        if (currentDms.isEmpty() || !currentDms.get(0).equals(member)) {
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
