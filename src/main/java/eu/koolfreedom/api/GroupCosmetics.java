package eu.koolfreedom.api;

import java.awt.*;
import java.util.*;

import eu.koolfreedom.config.ConfigEntry;
import eu.koolfreedom.util.KoolSMPCoreBase;
import lombok.Getter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.luckperms.api.event.node.NodeAddEvent;
import net.luckperms.api.event.node.NodeClearEvent;
import net.luckperms.api.event.node.NodeMutateEvent;
import net.luckperms.api.event.node.NodeRemoveEvent;
import net.luckperms.api.model.user.User;
import net.luckperms.api.node.types.InheritanceNode;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

public class GroupCosmetics extends KoolSMPCoreBase
{
    private final Map<String, Group> groups = new HashMap<>();
    private final Group defaultFallbackGroup = new Group("default", "Member", NamedTextColor.GRAY);
    private final Group consoleGroup = new Group("console", "Console", NamedTextColor.BLUE);

    // EVenTsUBScRipTIon<NOdemutateeVENt> uSED wIthOuT 'TRY'-WiTh-REsouRCES sTateMent
    @SuppressWarnings("resource")
    public GroupCosmetics()
    {
        loadGroups();

        // Register an event listener for NodeMutateEvent to track when players are added or removed to groups
        //
        // I actually spent a lot of time evaluating which event to use based on how many times the event gets called,
        //  how specific the event can be, and the surface of potential cases where it gets called
        //
        // UserDataRecalculateEvent is an incredibly generic event that gets called in a lot of cases without a good way
        //  to determine *what* caused the event to happen. Leaving the server calls the event. Updating permission
        //  nodes for a particular group that the user isn't even in calls the event.
        //
        // UserTrackEvent is an event that, as the name implies, only gets called when the player is promoted/demoted
        //  from a group in a track. It does not get called if their primary group is set directly, which is something
        //  we do in the /doom command to reset their rank completely, so there would be cases where it just won't
        //  update that event was hooked into.
        //
        // NodeMutateEvent was the best fit since it only gets called when a node change happens, and we can figure out
        //  whether an update is necessary or not with the parameters it gives us.
        api.getEventBus().subscribe(main, NodeMutateEvent.class, event ->
        {
            if (event.getTarget() instanceof User user)
            {
                // Player must be online and the node changing must include an inheritance node
                boolean shouldUpdate = Bukkit.getPlayer(user.getUniqueId()) != null && switch (event)
                {
                    case NodeClearEvent clear -> clear.getNodes().stream().anyMatch(node -> node instanceof InheritanceNode);
                    case NodeRemoveEvent remove -> remove.getNode() instanceof InheritanceNode;
                    case NodeAddEvent add -> add.getNode() instanceof InheritanceNode;
                    default -> false;
                };

                if (shouldUpdate)
                {
                    Player player = Bukkit.getPlayer(user.getUniqueId());

                    if (player != null)
                    {
                        player.playerListName(getColoredName(player));
                    }
                }
            }
        });
    }

    public void loadGroups()
    {
        groups.clear();

        ConfigurationSection section = ConfigEntry.GROUPS.getConfigurationSection();
        section.getKeys(false).stream().filter(section::isConfigurationSection).forEach(entry ->
				groups.put(entry, Group.fromConfigurationSection(entry, Objects.requireNonNull(section.getConfigurationSection(entry)))));
    }

    public Group getGroupByName(String name)
    {
        return groups.getOrDefault(name, defaultFallbackGroup);
    }

    public Group getGroupByNameOrDefault(String name, Group fallback)
    {
        return groups.getOrDefault(name, fallback);
    }

    public Group getSenderGroup(CommandSender sender)
    {
        if (sender instanceof Player player)
        {
            return getGroupByName(Objects.requireNonNull(api.getUserManager().getUser(player.getUniqueId())).getPrimaryGroup());
        }
        else if (sender instanceof ConsoleCommandSender)
        {
            return consoleGroup;
        }
        else
        {
            return getGroupByName("default");
        }
    }

    public Component getColoredName(CommandSender sender)
    {
        Objects.requireNonNull(sender);
        return Component.text(sender.getName()).color(getSenderGroup(sender).getColor());
    }

    @Getter
    public static class Group
    {
        private final String internalName;
        private final String name;
        private final Component displayName;
        private final TextColor color;

        public static Group fromConfigurationSection(String internalName, ConfigurationSection section)
        {
            String name = section.getString("name", internalName);
            TextColor color;
            if (section.isString("color"))
            {
                TextColor temp = NamedTextColor.NAMES.value(Objects.requireNonNull(section.getString("color")).toLowerCase());

                // UGLY HACK COMING RIGHT UP
                if (temp == null)
                {
                    try
                    {
                        color = TextColor.color(Color.decode(Objects.requireNonNull(section.getString("color"))).getRGB());
                    }
                    catch (NumberFormatException ex)
                    {
                        color = NamedTextColor.GRAY;
                    }
                }
                else
                {
                    color = temp;
                }
            }
            else
            {
                color = TextColor.color(Math.max(Math.min(section.getInt("color", NamedTextColor.GRAY.value()), 0xFFFFFF), 0));
            }

            Component displayName = section.getRichMessage("displayName", Component.text(name).color(color));

            return new Group(internalName, name, displayName, color);
        }

        public static Group createGroup(String internalName, String name, Component displayName, TextColor color)
        {
            return new Group(internalName, name, displayName, color);
        }

        private Group(String internalName, String name, TextColor textColor)
        {
            this.internalName = internalName;
            this.name = name;
            this.displayName = Component.text(name).color(textColor);
            this.color = textColor;
        }

        private Group(String internalName, String name, Component displayName, TextColor color)
        {
            this.internalName = internalName;
            this.name = name;
            this.displayName = displayName;
            this.color = color;
        }
    }
}