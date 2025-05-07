package eu.koolfreedom.util;

import eu.koolfreedom.KoolSMPCore;
import eu.koolfreedom.api.GroupCosmetics;
import eu.koolfreedom.config.ConfigEntry;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.Modifying;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class FUtil // the f stands for fuck
{
    private static final Random RANDOM = new Random();
    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();

    public static void staffAction(CommandSender sender, String message, TagResolver... placeholders)
    {
        broadcast("<gray><i>[<sender>: <action>]", Placeholder.unparsed("sender", sender.getName()),
                Placeholder.component("action", FUtil.miniMessage(message, placeholders)));
    }

    public static void broadcast(Component component)
    {
        Bukkit.broadcast(component);
        KoolSMPCore.getInstance().discordSrv.broadcast(component.decorate(TextDecoration.BOLD));
    }

    public static void broadcast(Component component, String permission)
    {
        Bukkit.broadcast(component, permission);
    }

    public static void broadcast(String message, TagResolver... placeholders)
    {
        Bukkit.broadcast(miniMessage(message, placeholders));

        // TODO: Do this better because this sucks lol
        KoolSMPCore.getInstance().discordSrv.broadcast(miniMessage(message, placeholders).decorate(TextDecoration.BOLD));
    }

    public static void broadcast(String permission, String message, TagResolver... placeholders)
    {
        Bukkit.broadcast(miniMessage(message, placeholders), permission);
    }

    public static Component miniMessage(String message, TagResolver... placeholders)
    {
        return MINI_MESSAGE.deserialize(message, placeholders);
    }

    public static int randomNumber()
    {
        return RANDOM.nextInt();
    }

    public static int randomNumber(int min, int max)
    {
        return RANDOM.nextInt(min, max);
    }

    public static void adminChat(CommandSender sender, String message)
    {
        Component formattedMessage = miniMessage(ConfigEntry.FORMATS_ADMIN_CHAT.getString(),
                Placeholder.unparsed("name", sender.getName()),
                Placeholder.component("rank", KoolSMPCore.getInstance().groupCosmetics.getSenderGroup(sender).getDisplayName()),
                Placeholder.unparsed("message", message));

        broadcast(formattedMessage, "kf.admin");
        KoolSMPCore.getInstance().discordSrv.adminChat(sender, message);
    }

    public static void adminChat(String sender, GroupCosmetics.Group group, String message)
    {
        Component formattedMessage = miniMessage(ConfigEntry.FORMATS_ADMIN_CHAT.getString(),
                Placeholder.unparsed("name", sender),
                Placeholder.component("rank", group.getDisplayName()),
                Placeholder.unparsed("message", message));

        broadcast(formattedMessage, "kf.admin");
    }

    public static String getIp(Player player)
    {
        return player.getAddress().getAddress().getHostAddress().trim();
    }

    public static class RandomColorTag implements Modifying
    {
        public static final RandomColorTag INSTANCE = new RandomColorTag();

        @Override
        public Component apply(@NotNull Component current, int depth)
        {
            if (current instanceof TextComponent textComponent)
            {
                return Component.join(JoinConfiguration.spaces(), Arrays.stream(textComponent.content().split(" "))
                        .map(text -> Component.text(text).color(TextColor.color(randomNumber(0, 255),
                                randomNumber(0, 255), randomNumber(0, 255)))).toList());
            }

            return current;
        }
    }
}
