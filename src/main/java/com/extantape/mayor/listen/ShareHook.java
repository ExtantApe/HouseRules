package com.extantape.mayor.listen;

import com.extantape.bluemapmini.api.CoordinateShareEvent;
import com.extantape.mayor.MayorPlugin;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

/**
 * BlueMapMini's /hud share follows the same rules as chat: recipients the sender can't reach are removed, and a sender
 * who can't chat where they are sends nothing. BlueMapMini tells the sender who got it. Only loaded when BlueMapMini is
 * installed, because this is the only class that names its event.
 */
public final class ShareHook implements Listener {
    private final MayorPlugin plugin;

    public ShareHook(MayorPlugin plugin) {
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
