package com.extantape.houserules.livetest.fakeminimap;

import com.extantape.bluemapmini.api.CoordinateShareEvent;
import com.mojang.brigadier.arguments.StringArgumentType;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.TreeSet;

/**
 * Test only. Console command "fakeshare <player>" fires CoordinateShareEvent the way BlueMapMini does, with every
 * other online player as a candidate, then logs "FAKESHARE <sender> cancelled=<bool> to=<names>".
 */
public final class FakeMinimap extends JavaPlugin {
    @Override
    public void onEnable() {
        getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, event -> event.registrar().register(Commands.literal("fakeshare")
                .requires(src -> !(src.getSender() instanceof Player))
                .then(Commands.argument("player", StringArgumentType.word()).executes(ctx -> {
                    Player sender = Bukkit.getPlayerExact(StringArgumentType.getString(ctx, "player"));
                    if (sender == null) {
                        getLogger().info("FAKESHARE FAILED no such player");
                        return 0;
                    }
                    Set<Player> recipients = new LinkedHashSet<>(Bukkit.getOnlinePlayers());
                    recipients.remove(sender);
                    Location at = sender.getLocation();
                    CoordinateShareEvent share = new CoordinateShareEvent(sender, sender.getWorld(), at.getBlockX(), at.getBlockY(), at.getBlockZ(), recipients);
                    Bukkit.getPluginManager().callEvent(share);
                    Set<String> names = new TreeSet<>();
                    for (Player p : share.getRecipients()) names.add(p.getName());
                    getLogger().info("FAKESHARE " + sender.getName() + " cancelled=" + share.isCancelled() + " listeners=" + CoordinateShareEvent.hasListeners() + " to=" + String.join(",", names));
                    return 1;
                }))
                .build()));
    }
}
