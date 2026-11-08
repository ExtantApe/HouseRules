package com.extantape.houserules.api;

import org.bukkit.entity.Player;

/**
 * HouseRules' answer to "may A reach B?", for other plugins. Get it from Bukkit's services manager:
 * {@code Bukkit.getServicesManager().load(ReachService.class)}. Safe to call from any thread.
 * Every answer follows the same rules as chat, including circles, circle-only worlds and houserules.bypass.
 */
public interface ReachService {
    /** Whether anything {@code from} says or sends can reach {@code to}. Always true for the same player. */
    boolean mayReach(Player from, Player to);

    /** Whether the player may chat at all where they are. False in a circle-only world without a circle, or while chat is paused. */
    boolean maySpeak(Player player);
}
