package com.extantape.houserules;

import com.extantape.houserules.api.ReachService;
import com.extantape.houserules.command.HouseRulesCommands;
import com.extantape.houserules.config.Settings;
import com.extantape.houserules.config.SettingsLoader;
import com.extantape.houserules.listen.ChatListener;
import com.extantape.houserules.listen.CommandListener;
import com.extantape.houserules.listen.PlayerListener;
import com.extantape.houserules.listen.ShareHook;
import com.extantape.houserules.listen.TextListener;
import com.extantape.houserules.rules.Reach;
import com.extantape.houserules.rules.Subject;
import com.extantape.houserules.text.Messages;
import com.extantape.houserules.text.Quips;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * HouseRules: who can hear whom, which commands are off, and a word filter. See README.md.
 * <p>
 * If the config is broken at startup there are no rules to follow, so chat and private messages are paused, and
 * admins are told why, until it's fixed and reloaded. A broken reload keeps the rules that were working.
 */
public final class HouseRulesPlugin extends JavaPlugin implements ReachService {
    public static final String ADMIN = "houserules.admin";

    /** Everything that changes on reload, swapped in one go so the chat thread never sees half of it. */
    public record State(Settings settings, Messages messages, Quips quips) {
    }

    private volatile State state;
    /** The built-in defaults, used only to recognise commands and word messages while chat is paused. */
    private State fallback;
    private volatile List<String> problems = List.of();
    /** Who each player last messaged or was messaged by, for /r. */
    private final Map<UUID, UUID> partners = new ConcurrentHashMap<>();

    @Override
    public void onEnable() {
        for (String file : List.of(SettingsLoader.CONFIG, SettingsLoader.FILTER, SettingsLoader.QUIPS)) {
            if (!getDataFolder().toPath().resolve(file).toFile().exists()) saveResource(file, false);
        }
        Settings defaults = new SettingsLoader(this::getResource).load(null, null, null).settings();
        fallback = new State(defaults, new Messages(defaults.messages()), new Quips(List.of(), 0, new Random()));
        reload(Bukkit.getConsoleSender());

        var plugins = getServer().getPluginManager();
        plugins.registerEvents(new ChatListener(this), this);
        plugins.registerEvents(new CommandListener(this), this);
        plugins.registerEvents(new TextListener(this), this);
        plugins.registerEvents(new PlayerListener(this), this);
        // Only touch a share event's class when its plugin is installed. The hooks are the only classes that name them.
        if (plugins.getPlugin("BlueMapMini") != null) {
            try {
                plugins.registerEvents(new ShareHook(this), this);
                getLogger().info("BlueMapMini found: coordinate shares follow the same rules as chat.");
            } catch (LinkageError e) {
                getLogger().warning("BlueMapMini is installed but has no share event this version can use, so shares aren't filtered. Update BlueMapMini and HouseRules together.");
            }
        }
        getServer().getServicesManager().register(ReachService.class, this, this, ServicePriority.Normal);
        HouseRulesCommands commands = new HouseRulesCommands(this);
        getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, event -> commands.register(event.registrar()));
    }

    @Override
    public void onDisable() {
        getServer().getServicesManager().unregisterAll(this);
        partners.clear();
    }

    /** Reads the config files again. Returns true if the new rules were applied. */
    public boolean reload(CommandSender sender) {
        SettingsLoader.Result result = new SettingsLoader(this::getResource).load(getDataFolder().toPath());
        for (String warning : result.warnings()) getLogger().warning(warning);
        if (!result.ok()) {
            for (String error : result.errors()) getLogger().severe(error);
            problems = result.errors();
            if (state == null) {
                getLogger().severe("HouseRules couldn't read its config, so chat and private messages are paused until it's fixed. Then run /houserules reload.");
            } else {
                getLogger().severe("The config has problems, so HouseRules kept the rules it already had.");
            }
            return false;
        }
        Settings settings = result.settings();
        state = new State(settings, new Messages(settings.messages()),
                new Quips(settings.quips(), settings.nobodyHeard().cooldownSeconds(), new Random()));
        problems = result.warnings();
        registerCirclePermissions(settings.reach());
        Reach reach = settings.reach();
        getLogger().info(reach.active()
                ? "Chat rules are on, with " + reach.circles().size() + (reach.circles().size() == 1 ? " circle. " : " circles. ") + settings.filterSummary()
                : "No circles are set up, so chat reaches everyone, as in vanilla. " + settings.filterSummary());
        return true;
    }

    /** Lists each circle's permission, so permission plugins can suggest it. Nobody has one by default. */
    private void registerCirclePermissions(Reach reach) {
        var plugins = getServer().getPluginManager();
        for (String circle : reach.circles()) {
            String node = Reach.CIRCLE_PERMISSION + circle;
            if (plugins.getPermission(node) == null) {
                plugins.addPermission(new Permission(node, "Puts a player in the " + circle + " circle.", PermissionDefault.FALSE));
            }
        }
    }

    /** The current rules, or null while chat is paused because the config couldn't be read. */
    public State state() {
        return state;
    }

    public boolean paused() {
        return state == null;
    }

    public Messages messages() {
        State s = state;
        return s == null ? fallback.messages() : s.messages();
    }

    /** The built-in defaults, for recognising private message commands while chat is paused. Never for reach. */
    public State fallback() {
        return fallback;
    }

    public List<String> problems() {
        return problems;
    }

    public Map<UUID, UUID> partners() {
        return partners;
    }

    public void tell(CommandSender to, String key, Map<String, String> vars) {
        Messages messages = messages();
        if (!messages.silenced(key)) to.sendMessage(messages.get(key, vars));
    }

    public void tell(CommandSender to, String key) {
        tell(to, key, Map.of());
    }

    public void tell(CommandSender to, Component message) {
        to.sendMessage(message);
    }

    @Override
    public boolean mayReach(Player from, Player to) {
        if (from.equals(to)) return true;
        State s = state;
        return s != null && s.settings().reach().mayReach(Subject.of(from), Subject.of(to));
    }

    @Override
    public boolean maySpeak(Player player) {
        State s = state;
        return s != null && s.settings().reach().maySpeak(Subject.of(player));
    }

    @Override
    public boolean sharesCircle(java.util.function.Predicate<String> viewerHas, Player to) {
        State s = state;
        if (s == null || viewerHas == null || to == null) return false;
        try {
            return s.settings().reach().reachesWhileAway(viewerHas, Subject.of(to));
        } catch (RuntimeException e) {
            return false;
        }
    }
}
