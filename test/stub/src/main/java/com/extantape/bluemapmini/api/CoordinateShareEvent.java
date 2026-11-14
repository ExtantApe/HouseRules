package com.extantape.bluemapmini.api;

import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import java.util.Set;

/**
 * A copy of BlueMapMini's share event, so HouseRules compiles
 * without BlueMapMini. The live tests also ship it in a stand-in plugin. Fired on the main thread just before a share.
 */
public final class CoordinateShareEvent extends Event implements Cancellable {
    private static final HandlerList HANDLERS = new HandlerList();
    private final Player sender;
    private final World world;
    private final int x, y, z;
    private final Set<Player> recipients;
    private boolean cancelled;

    public CoordinateShareEvent(Player sender, World world, int x, int y, int z, Set<Player> recipients) {
        this.sender = sender;
        this.world = world;
        this.x = x;
        this.y = y;
        this.z = z;
        this.recipients = recipients;
    }

    public Player getSender() { return sender; }
    public World getWorld() { return world; }
    public int getX() { return x; }
    public int getY() { return y; }
    public int getZ() { return z; }
    public Set<Player> getRecipients() { return recipients; }
    @Override public boolean isCancelled() { return cancelled; }
    @Override public void setCancelled(boolean cancel) { cancelled = cancel; }
    @Override public HandlerList getHandlers() { return HANDLERS; }
    public static HandlerList getHandlerList() { return HANDLERS; }
    public static boolean hasListeners() { return HANDLERS.getRegisteredListeners().length > 0; }
}
