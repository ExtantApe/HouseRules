package com.extantape.houserules.listen;

import com.extantape.blueminimap.api.CoordinateShareEvent;
import com.extantape.houserules.HouseRulesPlugin;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

/**
 * BlueMinimap's /hud share follows the same rules as chat: recipients the sender can't reach are removed, and a sender
 * who can't chat where they are sends nothing. BlueMinimap tells the sender who got it. Only loaded when BlueMinimap is
 * installed, because this is the only class that names its event. {@link LegacyShareHook} does the same for
 * BlueMapHUD 0.1.x, its name before 2026-11-09.
 */
public final class ShareHook implements Listener {
    private final HouseRulesPlugin plugin;

    public ShareHook(HouseRulesPlugin plugin) {
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
