package eu.koolfreedom;

import eu.koolfreedom.api.GroupCosmetics;
import eu.koolfreedom.banning.BanManager;
import eu.koolfreedom.config.ConfigEntry;
import eu.koolfreedom.extensions.DiscordSRVExtension;
import eu.koolfreedom.log.FLog;
import eu.koolfreedom.punishment.RecordKeeper;
import eu.koolfreedom.util.FUtil;
import io.papermc.paper.event.player.AsyncChatEvent;
import lombok.Getter;
import net.luckperms.api.LuckPerms;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabExecutor;
import org.bukkit.plugin.java.*;
import org.bukkit.plugin.*;
import eu.koolfreedom.command.impl.*;
import eu.koolfreedom.listener.*;
import com.earth2me.essentials.*;
import org.bukkit.event.*;
import java.io.InputStream;
import org.bukkit.*;
import java.util.*;

import org.bukkit.entity.*;

public class KoolSMPCore extends JavaPlugin implements Listener
{
    @Getter
    private static KoolSMPCore instance;
    private static LuckPerms luckPermsAPI = null;
    public static final BuildProperties build = new BuildProperties();

    public BanManager banManager;
    public MuteManager muteManager;
    public RecordKeeper recordKeeper;

    public LoginListener loginListener;
    public ServerListener serverListener;
    public GroupCosmetics groupCosmetics;
    public ExploitListener exploitListener;
    public ChatFilter chatFilter;

    public DiscordSRVExtension discordSrv;

    public static LuckPerms getLuckPermsAPI()
    {
        if (luckPermsAPI == null)
        {
            RegisteredServiceProvider<LuckPerms> provider = Bukkit.getServicesManager().getRegistration(LuckPerms.class);
            if (provider != null)
            {
                FLog.info("Successfully loaded the LuckPerms API.");
                luckPermsAPI = provider.getProvider();
            }
            else
            {
                FLog.error("The LuckPerms API was not loaded successfully. The plugin will not function properly.");
            }
        }

        return luckPermsAPI;
    }

    @Override
    public void onLoad()
    {
        instance = this;

        build.load(instance);
    }

    @Override
    public void onEnable()
    {
        FLog.info("Created by gamingto12 and 0x7694C9");
        FLog.info("Version " + build.version);
        FLog.info("Compiled " + build.date + " by " + build.author);
        Bukkit.getPluginManager().registerEvents(this, this);
        loadCommands();
        FLog.info("Loaded commands");
        loadListeners();
        FLog.info("Loaded listeners");
        loadExtensions();
        FLog.info("Loaded plugin extensions");
        groupCosmetics = new GroupCosmetics();
        loadBansConfig();
        FLog.info("Loaded configurations");

        if (ConfigEntry.ANNOUNCER_ENABLED.getBoolean())
        {
            announcerRunnable();
        }

        getLuckPermsAPI();
    }

    @Override
    public void onDisable()
    {
        FLog.info("KoolSMPCore has been disabled");

        banManager.save();
    }

    public void loadBansConfig()
    {
        banManager = new BanManager();
        banManager.load();
        recordKeeper = new RecordKeeper();
    }

    public void loadListeners()
    {
        muteManager = new MuteManager();
        serverListener = new ServerListener(this);
        exploitListener = new ExploitListener(this);
        loginListener = new LoginListener();
        chatFilter = new ChatFilter();
    }

    public void loadCommands()
    {
        registerCommand("adminchat", new AdminChatCommand());
        registerCommand("ban", new BanCommand());
        registerCommand("banip", new BanIPCommand());
        registerCommand("banlist", new BanListCommand());
        registerCommand("clearchat", new ClearChatCommand());
        registerCommand("commandspy", new CommandSpyCommand());
        registerCommand("crash", new CrashCommand());
        registerCommand("cry", new CryCommand());
        registerCommand("doom", new DoomCommand());
        registerCommand("hug", new HugCommand());
        registerCommand("kick", new KickCommand());
        registerCommand("kiss", new KissCommand());
        registerCommand("koolsmpcore", new KoolSMPCoreCommand());
        registerCommand("mute", new MuteCommand());
        registerCommand("orbit", new OrbitCommand());
        registerCommand("pat", new PatCommand());
        registerCommand("poke", new PokeCommand());
        registerCommand("rawsay", new RawSayCommand());
        registerCommand("report", new ReportCommand());
        registerCommand("satisfyall", new SatisfyAllCommand());
        registerCommand("say", new SayCommand());
        registerCommand("ship", new ShipCommand());
        registerCommand("slap", new SlapCommand());
        registerCommand("smite", new SmiteCommand());
        registerCommand("spectate", new SpectateCommand());
        registerCommand("unban", new UnbanCommand());
        registerCommand("warn", new WarnCommand());
    }

    public void loadExtensions()
    {
        discordSrv = new DiscordSRVExtension();
    }

    @SuppressWarnings("deprecation")
    public static class BuildProperties
    {
        public String author;
        public String version;
        public String number;
        public String date;

        void load(KoolSMPCore plugin)
        {
            try
            {
                final Properties props;

                try (InputStream in = plugin.getResource("build.properties"))
                {
                    props = new Properties();
                    props.load(in);
                }

                author = props.getProperty("buildAuthor", "gamingto12");
                version = props.getProperty("buildVersion", plugin.getDescription().getVersion());
                number = props.getProperty("buildNumber", "1");
                date = props.getProperty("buildDate", "unknown");
            }
            catch (Exception ex)
            {
                FLog.error("Could not load build properties! Did you compile with NetBeans/Maven?", ex);
            }
        }
    }

    private void registerCommand(String name, TabExecutor executor)
    {
        final PluginCommand cmd = Objects.requireNonNull(getCommand(name));

        cmd.setExecutor(executor);
        cmd.setTabCompleter(executor);
    }

    public boolean isVanished(Player player)
    {
        Plugin essentials = Bukkit.getServer().getPluginManager().getPlugin("Essentials");

        if (essentials == null) {
            return false;
        }

        return essentials.isEnabled() && ((Essentials) essentials).getUser(player).isVanished();
    }

    private void announcerRunnable()
    {
        Bukkit.getScheduler().scheduleSyncRepeatingTask(this, () ->
        {
            List<String> messages = ConfigEntry.ANNOUNCER_MESSAGES.getStringList();

            // Messages aren't configured, so we're not going to bother
            if (messages.isEmpty())
            {
                return;
            }

            Bukkit.broadcast(FUtil.miniMessage(messages.get(FUtil.randomNumber(0, messages.size()))));
        }, 0L, ConfigEntry.ANNOUNCER_DELAY.getInteger());
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    private void onChat(AsyncChatEvent event)
    {
        final Player player = event.getPlayer();
        final String message = event.signedMessage().message();

        // In-game pinging
        if (message.contains("@"))
        {
            Arrays.stream(message.split(" ")).filter(word -> word.startsWith("@")).filter(word ->
                    !word.equalsIgnoreCase("@everyone") || player.hasPermission("kf.admin"))
                    .map(ping -> ping.replaceAll("@", "")).map(Bukkit::getPlayer)
                    .filter(Objects::nonNull).distinct().forEach(target -> target.playSound(target.getLocation(),
                            Sound.BLOCK_NOTE_BLOCK_PLING, 1.0F, 1.0F));
        }
    }
}
