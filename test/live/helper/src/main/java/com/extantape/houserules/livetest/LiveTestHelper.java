package com.extantape.houserules.livetest;

import com.mojang.brigadier.arguments.StringArgumentType;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.WorldType;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Locale;

/**
 * Test only. One console command, /hrt:
 *   hrt world <name>          makes a small flat world (a name ending in _nether or _the_end gets that environment)
 *   hrt tp <player> <world>   moves a player to a world's spawn
 *   hrt tps                   prints "HRT tps <1m> mspt <avg>"
 */
public final class LiveTestHelper extends JavaPlugin {
    @Override
    public void onEnable() {
        getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, event -> event.registrar().register(Commands.literal("hrt")
                .requires(src -> src.getSender().isOp() || !(src.getSender() instanceof Player))
                .then(Commands.literal("world").then(Commands.argument("name", StringArgumentType.word()).executes(ctx -> {
                    String name = StringArgumentType.getString(ctx, "name");
                    World.Environment env = name.endsWith("_nether") ? World.Environment.NETHER
                            : name.endsWith("_the_end") ? World.Environment.THE_END : World.Environment.NORMAL;
                    World world = new WorldCreator(name).environment(env).type(WorldType.FLAT).generateStructures(false).createWorld();
                    getLogger().info("HRT world " + (world == null ? "FAILED " + name : world.getName()));
                    return 1;
                })))
                .then(Commands.literal("tp").then(Commands.argument("player", StringArgumentType.word())
                        .then(Commands.argument("world", StringArgumentType.word()).executes(ctx -> {
                            Player p = Bukkit.getPlayerExact(StringArgumentType.getString(ctx, "player"));
                            World w = Bukkit.getWorld(StringArgumentType.getString(ctx, "world"));
                            if (p == null || w == null) {
                                getLogger().info("HRT tp FAILED");
                                return 0;
                            }
                            p.teleportAsync(w.getSpawnLocation()).thenAccept(ok -> getLogger().info("HRT tp " + p.getName() + " " + w.getName() + " " + ok));
                            return 1;
                        }))))
                .then(Commands.literal("tps").executes(ctx -> {
                    double[] tps = Bukkit.getTPS();
                    getLogger().info(String.format(Locale.ROOT, "HRT tps %.2f mspt %.2f", tps[0], Bukkit.getAverageTickTime()));
                    return 1;
                }))
                .build()));
    }
}
