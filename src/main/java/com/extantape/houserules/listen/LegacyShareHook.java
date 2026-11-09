package com.extantape.houserules.listen;

import com.extantape.bluemaphud.api.CoordinateShareEvent;
import com.extantape.houserules.HouseRulesPlugin;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

/**
 * The same as {@link ShareHook}, for BlueMapHUD 0.1.x, BlueMinimap's name before 2026-11-09. Kept for one release so a
 * server that updates HouseRules before BlueMinimap still filters shares. Only loaded when BlueMapHUD is installed.
 */
public final class LegacyShareHook implements Listener {
    private final HouseRulesPlugin plugin;

    public LegacyShareHook(HouseRulesPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onShare(CoordinateShareEvent event) {
        Player sender = event.getSender();
        if (!plugin.maySpeak(sender)) {
            event.setCancelled(true);
            plugin.tell(sender, plugin.paused() ? "chat-paused" : "share-closed-here");
            return;
        }
        event.getRecipients().removeIf(to -> !plugin.mayReach(sender, to));
    }
}
