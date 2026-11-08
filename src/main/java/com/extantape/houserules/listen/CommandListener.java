package com.extantape.houserules.listen;

import com.extantape.houserules.HouseRulesPlugin;
import com.extantape.houserules.rules.CommandRules;
import com.extantape.houserules.rules.Subject;
import com.extantape.houserules.text.Messages;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

import java.util.UUID;

/**
 * Blocked commands, and private messages and replies to players out of reach. Works on the typed command line
 * before any plugin sees it, so it covers other plugins' commands and their namespaced forms.
 */
public final class CommandListener implements Listener {
    private final HouseRulesPlugin plugin;

    public CommandListener(HouseRulesPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onCommand(PlayerCommandPreprocessEvent event) {
        Player from = event.getPlayer();
        HouseRulesPlugin.State state = plugin.state();
        boolean paused = state == null;
        CommandRules rules = (paused ? plugin.fallback() : state).settings().commands();
        CommandRules.Decision decision = rules.decide(Subject.of(from), event.getMessage(), paused,
                name -> {
                    Player p = Bukkit.getPlayerExact(name);
                    return p == null ? null : Subject.of(p);
                },
                plugin.partners().get(from.getUniqueId()),
                id -> {
                    Player p = Bukkit.getPlayer(id);
                    return p == null ? null : Subject.of(p);
                });
        switch (decision.kind()) {
            case PASS -> { }
            case BLOCK -> {
                event.setCancelled(true);
                if (decision.text() != null) {
                    if (!decision.text().isBlank()) from.sendMessage(Messages.parse(decision.text(), decision.vars()));
                } else plugin.tell(from, decision.key(), decision.vars());
            }
            case DELIVER -> {
                UUID to = decision.partner().id();
                plugin.partners().put(from.getUniqueId(), to);
                plugin.partners().put(to, from.getUniqueId());
            }
        }
    }
}
