package com.extantape.mayor.listen;

import com.extantape.mayor.MayorPlugin;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.Map;

/** Tells admins about config problems when they join, and forgets a player's reply partner when they leave. */
public final class PlayerListener implements Listener {
    private final MayorPlugin plugin;

    public PlayerListener(MayorPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (!player.hasPermission(MayorPlugin.ADMIN)) return;
        if (plugin.paused()) plugin.tell(player, "admin-paused");
        else if (!plugin.problems().isEmpty()) plugin.tell(player, "admin-warnings", Map.of("count", String.valueOf(plugin.problems().size())));
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        plugin.partners().remove(event.getPlayer().getUniqueId());
        MayorPlugin.State state = plugin.state();
        if (state != null) state.quips().forget(event.getPlayer().getUniqueId());
    }
}
