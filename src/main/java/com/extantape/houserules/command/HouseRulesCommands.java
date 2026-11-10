package com.extantape.houserules.command;

import com.extantape.houserules.HouseRulesPlugin;
import com.extantape.houserules.rules.Reach;
import com.extantape.houserules.rules.Subject;
import com.extantape.houserules.rules.WordFilter;
import com.extantape.signalbox.Answer;
import com.extantape.signalbox.Describe;
import com.extantape.signalbox.Request;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import io.papermc.paper.command.brigadier.Commands;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/** /circles for players, and /houserules status, reload, check and describe for admins, each with a --json form. */
public final class HouseRulesCommands {
    public static final String CIRCLES = "houserules.circles";
    public static final String CIRCLES_NAMES = "houserules.circles.names";
    private final HouseRulesPlugin plugin;

    public HouseRulesCommands(HouseRulesPlugin plugin) {
        this.plugin = plugin;
    }

    public void register(Commands commands) {
        commands.register(Commands.literal("circles")
                .requires(src -> src.getSender() instanceof Player p && p.hasPermission(CIRCLES))
                .executes(ctx -> circles((Player) ctx.getSource().getSender()))
                .build(), "Shows your circles and who can hear you right now.");

        // One greedy argument, read by Signalbox, so every subcommand also takes --json <id> (format 1). Players without
        // houserules.admin don't see the command at all; the console always can.
        commands.register(Commands.literal("houserules")
                .requires(src -> src.getSender().hasPermission(HouseRulesPlugin.ADMIN))
                .executes(ctx -> status(ctx.getSource().getSender()))
                .then(Commands.argument("args", StringArgumentType.greedyString())
                        .suggests((ctx, builder) -> suggest(builder))
                        .executes(ctx -> run(ctx.getSource().getSender(), ctx.getArgument("args", String.class))))
                .build(), "HouseRules admin: status, reload, check whether one player can reach another, and describe.");
    }

    private CompletableFuture<Suggestions> suggest(SuggestionsBuilder builder) {
        String[] words = builder.getRemaining().split(" ", -1);
        String last = words[words.length - 1].toLowerCase(Locale.ROOT);
        SuggestionsBuilder at = builder.createOffset(builder.getStart() + builder.getRemaining().length() - last.length());
        if (words.length == 1) {
            for (String sub : List.of("status", "reload", "check", "describe")) if (sub.startsWith(last)) at.suggest(sub);
        } else if (words[0].equalsIgnoreCase("check") && words.length <= 3) {
            for (Player p : Bukkit.getOnlinePlayers()) if (p.getName().toLowerCase(Locale.ROOT).startsWith(last)) at.suggest(p.getName());
        }
        return at.buildFuture();
    }

    private int run(CommandSender sender, String input) {
        Request r;
        try {
            r = Request.parse(input.trim().isEmpty() ? new String[0] : input.trim().split(" +"));
        } catch (Request.Bad bad) {
            if (bad.id != null) Answer.send(sender::sendMessage, Answer.error(bad.id, bad.code, bad.getMessage()));
            else plugin.tell(sender, Component.text(bad.getMessage(), NamedTextColor.RED));
            return Command.SINGLE_SUCCESS;
        }
        if (r.json()) {
            Answer.send(sender::sendMessage, Console.answer(host, r));
            return Command.SINGLE_SUCCESS;
        }
        List<String> t = r.tokens();
        String sub = t.isEmpty() ? "status" : t.get(0).toLowerCase(Locale.ROOT);
        switch (sub) {
            case "status" -> status(sender);
            case "reload" -> reload(sender);
            case "check" -> {
                Player from = t.size() == 3 ? online(t.get(1)) : null, to = t.size() == 3 ? online(t.get(2)) : null;
                if (t.size() != 3) plugin.tell(sender, Component.text("Usage: /houserules check <from> <to>", NamedTextColor.RED));
                else if (from == null || to == null) plugin.tell(sender, Component.text("Both players need to be online.", NamedTextColor.RED));
                else check(sender, from, to);
            }
            case "describe" -> {
                for (Describe.Command d : Console.described()) {
                    StringBuilder line = new StringBuilder("/houserules " + d.name());
                    for (Describe.Arg a : d.args()) line.append(" <").append(a.name()).append('>');
                    plugin.tell(sender, Component.text(line.toString(), NamedTextColor.GRAY));
                }
            }
            default -> plugin.tell(sender, Component.text("There's no /houserules " + sub + ". Try status, reload, check or describe.", NamedTextColor.RED));
        }
        return Command.SINGLE_SUCCESS;
    }

    /** A player by exact name or uuid, or null if they aren't online. */
    static Player online(String nameOrUuid) {
        try {
            return Bukkit.getPlayer(UUID.fromString(nameOrUuid));
        } catch (IllegalArgumentException notAUuid) {
            return Bukkit.getPlayerExact(nameOrUuid);
        }
    }

    /** What the console answers from: the live rules and the online players. */
    private final Console.Host host = new Console.Host() {
        @Override public String version() { return plugin.getPluginMeta().getVersion(); }

        @Override public Console.Status status() {
            HouseRulesPlugin.State state = plugin.state();
            if (state == null) return new Console.Status(true, List.of(), false, plugin.problems());
            return new Console.Status(false, state.settings().reach().circles(), !state.settings().filter().isEmpty(), plugin.problems());
        }

        @Override public Console.Reloaded reload() {
            boolean applied = plugin.reload(Bukkit.getConsoleSender());
            return new Console.Reloaded(applied, plugin.paused(), plugin.problems());
        }

        @Override public Console.Checked check(String fromArg, String toArg) {
            Player from = online(fromArg), to = online(toArg);
            HouseRulesPlugin.State state = plugin.state();
            if (from == null || to == null || state == null) return null;
            Reach reach = state.settings().reach();
            Reach.Why why = reach.why(Subject.of(from), Subject.of(to));
            return new Console.Checked(who(from), who(to), why.reaches, reach.maySpeak(Subject.of(from)),
                    why.name().toLowerCase(Locale.ROOT).replace('_', '-'), reason(reach, why, from, to), from.hasPermission(WordFilter.BYPASS));
        }

        private Console.Who who(Player p) { return new Console.Who(p.getName(), p.getUniqueId().toString()); }
    };

    private int circles(Player player) {
        HouseRulesPlugin.State state = plugin.state();
        if (state == null) {
            plugin.tell(player, "chat-paused");
            return Command.SINGLE_SUCCESS;
        }
        Reach reach = state.settings().reach();
        if (!reach.active()) {
            plugin.tell(player, "circles-off");
            return Command.SINGLE_SUCCESS;
        }
        Subject me = Subject.of(player);
        List<String> mine = reach.circlesOf(me);
        if (mine.isEmpty()) plugin.tell(player, "circles-none");
        else plugin.tell(player, "circles-yours", Map.of("circles", String.join(", ", mine)));
        if (!reach.maySpeak(me)) {
            plugin.tell(player, "chat-closed-here");
            return Command.SINGLE_SUCCESS;
        }
        List<String> names = new ArrayList<>();
        for (Player other : Bukkit.getOnlinePlayers()) {
            // Players hidden from this player, such as vanished staff, are never counted or named.
            if (other.equals(player) || !player.canSee(other)) continue;
            if (reach.mayReach(me, Subject.of(other))) names.add(other.getName());
        }
        names.sort(String.CASE_INSENSITIVE_ORDER);
        if (names.isEmpty()) plugin.tell(player, "circles-nobody");
        else if (player.hasPermission(CIRCLES_NAMES)) {
            plugin.tell(player, "circles-hearers", Map.of("count", String.valueOf(names.size()), "names", String.join(", ", names)));
        } else plugin.tell(player, "circles-hearers-count", Map.of("count", String.valueOf(names.size())));
        return Command.SINGLE_SUCCESS;
    }

    private int reload(CommandSender sender) {
        boolean applied = plugin.reload(sender);
        if (applied) {
            plugin.tell(sender, "admin-reloaded");
            for (String warning : plugin.problems()) plugin.tell(sender, "admin-problem", Map.of("problem", warning));
        } else {
            plugin.tell(sender, plugin.paused() ? "admin-reload-failed-paused" : "admin-reload-failed");
            for (String error : plugin.problems()) plugin.tell(sender, "admin-problem", Map.of("problem", error));
        }
        return Command.SINGLE_SUCCESS;
    }

    private int status(CommandSender sender) {
        HouseRulesPlugin.State state = plugin.state();
        if (state == null) plugin.tell(sender, "admin-paused");
        else {
            Reach reach = state.settings().reach();
            plugin.tell(sender, "admin-status", Map.of(
                    "circles", reach.circles().isEmpty() ? "none (vanilla chat)" : String.join(", ", reach.circles()),
                    "filter", state.settings().filter().isEmpty() ? "off" : "on"));
        }
        for (String problem : plugin.problems()) plugin.tell(sender, "admin-problem", Map.of("problem", problem));
        return Command.SINGLE_SUCCESS;
    }

    private void check(CommandSender sender, Player from, Player to) {
        HouseRulesPlugin.State state = plugin.state();
        if (state == null) {
            plugin.tell(sender, "admin-paused");
            return;
        }
        Reach reach = state.settings().reach();
        Subject a = Subject.of(from), b = Subject.of(to);
        Reach.Why why = reach.why(a, b);
        String reason = reason(reach, why, from, to);
        if (!reach.maySpeak(a)) reason += ", and " + from.getName() + " can't chat where they are";
        if (from.hasPermission(WordFilter.BYPASS)) reason += ". " + from.getName() + " skips the word filter";
        plugin.tell(sender, why.reaches && reach.maySpeak(a) ? "admin-check-yes" : "admin-check-no",
                Map.of("from", from.getName(), "to", to.getName(), "reason", reason));
    }

    private static String reason(Reach reach, Reach.Why why, Player from, Player to) {
        Subject a = Subject.of(from), b = Subject.of(to);
        return switch (why) {
            case RULES_OFF -> "no circles are set up, so everyone reaches everyone";
            case SELF -> "that's the same player";
            case BYPASS -> "one of them has houserules.bypass";
            case SAME_WORLD -> "they're in the same world group, " + reach.group(from.getWorld().getName());
            case SHARED_CIRCLE -> "they share the circle " + reach.sharedCircle(a, b);
            case CIRCLE_ONLY_WORLD -> "they're in a circle-only world and share no circle";
            case NOT_SHARED -> "they're in different world groups and share no circle";
        };
    }
}
