package eu.koolfreedom.extensions;

import eu.koolfreedom.KoolSMPCore;
import eu.koolfreedom.api.GroupCosmetics;
import eu.koolfreedom.log.FLog;
import eu.koolfreedom.util.FUtil;
import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.api.Subscribe;
import github.scarsz.discordsrv.api.events.DiscordGuildMessagePreProcessEvent;
import github.scarsz.discordsrv.dependencies.jda.api.EmbedBuilder;
import github.scarsz.discordsrv.dependencies.jda.api.entities.TextChannel;
import github.scarsz.discordsrv.util.DiscordUtil;
import github.scarsz.discordsrv.util.MessageUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.awt.*;
import java.time.ZonedDateTime;

public class DiscordSRVExtension
{
	private final GroupCosmetics.Group group = GroupCosmetics.Group.createGroup("discord", "Discord",
			Component.text("Discord").color(TextColor.color(0x5865F2)), TextColor.color(0x5865F2));

	private Plugin srv = null;

	public DiscordSRVExtension()
	{
		if (Bukkit.getPluginManager().isPluginEnabled("DiscordSRV"))
		{
			srv = DiscordSRV.getPlugin();
			DiscordSRV.api.subscribe(this);
		}
	}

	private DiscordSRV getPluginAsDiscordSrv()
	{
		return (DiscordSRV) srv;
	}

	public void report(Player reporter, OfflinePlayer reported, String reason)
	{
		if (srv == null || !srv.isEnabled() || channelDoesNotExist("reports"))
		{
			return;
		}

		try
		{
			getPluginAsDiscordSrv().getOptionalTextChannel("reports").sendMessageEmbeds(new EmbedBuilder()
					.setTitle("Report for " + reported.getName() + (!reported.isOnline() ? " (offline)" : ""))
					.setColor(Color.RED)
					.setDescription(reason)
					.setFooter("Reported by " + reporter.getName(), "https://minotar.net/helm/" + reporter.getName() + ".png")
					.setTimestamp(ZonedDateTime.now())
					.build()).queue();
		}
		catch (Exception ex)
		{
			FLog.error("Failed to send report data to Discord", ex);
		}
	}

	public void adminChat(CommandSender sender, String message)
	{
		if (srv == null || !srv.isEnabled() || channelDoesNotExist("adminchat"))
		{
			return;
		}

		if (sender instanceof Player player)
		{
			getPluginAsDiscordSrv().processChatMessage(player, message,
					getPluginAsDiscordSrv().getOptionalChannel("adminchat"), false, null);
		}
		else
		{
			DiscordUtil.queueMessage(getPluginAsDiscordSrv().getOptionalTextChannel("adminchat"),
					sender.getName() + " » " + message, false);
		}
	}

	public void broadcast(Component message)
	{
		if (srv == null || !srv.isEnabled())
		{
			return;
		}

		final TextChannel channel = getPluginAsDiscordSrv().getOptionalTextChannel("broadcasts");
		final github.scarsz.discordsrv.dependencies.kyori.adventure.text.Component converted = convert(message);

		DiscordUtil.queueMessage(channel, MessageUtil.reserializeToDiscord(converted), false);
	}

	private github.scarsz.discordsrv.dependencies.kyori.adventure.text.Component convert(Component input)
	{
		return github.scarsz.discordsrv.dependencies.kyori.adventure.text.serializer.gson.GsonComponentSerializer.gson()
				.deserialize(GsonComponentSerializer.gson().serialize(input));
	}

	private boolean channelDoesNotExist(String name)
	{
		return !getPluginAsDiscordSrv().getChannels().containsKey(name);
	}

	@Subscribe
	@SuppressWarnings("unused")
	public void onAdminChatMessageFromDiscord(DiscordGuildMessagePreProcessEvent event)
	{
		if (!event.getChannel().equals(getPluginAsDiscordSrv().getOptionalTextChannel("adminchat")) ||
				event.getAuthor().equals(getPluginAsDiscordSrv().getJda().getSelfUser()))
		{
			return;
		}

		FUtil.adminChat(event.getAuthor().getEffectiveName(),
				KoolSMPCore.getInstance().groupCosmetics.getGroupByNameOrDefault("discord", group),
				event.getMessage().getContentRaw());
	}
}
