package com.extantape.houserules.command;

import com.extantape.houserules.HouseRulesPlugin;
import com.extantape.houserules.rules.Reach;
import com.extantape.houserules.rules.Subject;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.command.brigadier.argument.ArgumentTypes;
import io.papermc.paper.command.brigadier.argument.resolvers.selector.PlayerSelectorArgumentResolver;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** /circles for players, and /houserules reload, status and check for admins. */
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

        commands.register(Commands.literal("houserules")
                .requires(src -> src.getSender().hasPermission(HouseRulesPlugin.ADMIN))
                .executes(ctx -> status(ctx.getSource().getSender()))
                .then(Commands.literal("reload").executes(ctx -> reload(ctx.getSource().getSender())))
                .then(Commands.literal("status").executes(ctx -> status(ctx.getSource().getSender())))
                .then(Commands.literal("check")
                        .then(Commands.argument("from", ArgumentTypes.player())
                                .then(Commands.argument("to", ArgumentTypes.player())
                                        .executes(this::check))))
                .build(), "HouseRules admin: reload, status, and check whether one player can reach another.");
    }

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

    private int check(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        Player from = ctx.getArgument("from", PlayerSelectorArgumentResolver.class).resolve(ctx.getSource()).getFirst();
        Player to = ctx.getArgument("to", PlayerSelectorArgumentResolver.class).resolve(ctx.getSource()).getFirst();
        CommandSender sender = ctx.getSource().getSender();
        HouseRulesPlugin.State state = plugin.state();
        if (state == null) {
            plugin.tell(sender, "admin-paused");
            return Command.SINGLE_SUCCESS;
        }
        Reach reach = state.settings().reach();
        Subject a = Subject.of(from), b = Subject.of(to);
        Reach.Why why = reach.why(a, b);
        String reason = switch (why) {
            case RULES_OFF -> "no circles are set up, so everyone reaches everyone";
            case SELF -> "that's the same player";
            case BYPASS -> "one of them has houserules.bypass";
            case SAME_WORLD -> "they're in the same world group, " + reach.group(from.getWorld().getName());
            case SHARED_CIRCLE -> "they share the circle " + reach.sharedCircle(a, b);
            case CIRCLE_ONLY_WORLD -> "they're in a circle-only world and share no circle";
            case NOT_SHARED -> "they're in different world groups and share no circle";
        };
        if (!reach.maySpeak(a)) reason += ", and " + from.getName() + " can't chat where they are";
        plugin.tell(sender, why.reaches && reach.maySpeak(a) ? "admin-check-yes" : "admin-check-no",
                Map.of("from", from.getName(), "to", to.getName(), "reason", reason));
        return Command.SINGLE_SUCCESS;
    }
}
