package eu.koolfreedom.command.impl;

import eu.koolfreedom.KoolSMPCore;
import eu.koolfreedom.command.KoolCommand;
import eu.koolfreedom.config.ConfigEntry;

import java.util.List;

import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.apache.commons.lang3.ArrayUtils;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class ReportCommand extends KoolCommand
{
    @Override
    public boolean run(CommandSender sender, Player playerSender, Command cmd, String commandLabel, String[] args)
    {
        if (args.length <= 1)
        {
            return false;
        }

        OfflinePlayer target = Bukkit.getOfflinePlayer(args[0]);
        if (!target.isOnline() && !target.hasPlayedBefore())
        {
            msg(sender, playerNotFound);
            return true;
        }

        String reason = String.join(" ", ArrayUtils.remove(args, 0));

        broadcast("kf.admin", ConfigEntry.FORMATS_REPORT.getString(),
                Placeholder.parsed("reporter", sender.getName()),
                Placeholder.parsed("player", target.getName() != null ? target.getName() : target.getUniqueId().toString()),
                Placeholder.unparsed("reason", reason));

        KoolSMPCore.getInstance().discordSrv.report(playerSender, target, reason);

        msg(sender, "<green>Thank you. Your report has been logged.");
        msg(sender, "<yellow>Please keep in mind that spamming reports is not allowed, and you will be sanctioned if you do so.");

        if (target.equals(playerSender))
        {
            msg(sender, "<red>But why in the world would you report yourself???");
        }

        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, Command command, String commandLabel, String[] args)
    {
        return args.length == 1 ? Bukkit.getOnlinePlayers().stream().map(Player::getName).toList() : List.of();
    }
}