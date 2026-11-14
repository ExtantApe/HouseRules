package com.extantape.mayor.rules;

import org.bukkit.entity.Player;

import java.util.UUID;
import java.util.function.Predicate;

/** What the rules need to know about a player: who they are, where they are, and what they're allowed. */
public interface Subject {
    UUID id();

    String name();

    String world();

    boolean has(String permission);

    static Subject of(Player player) {
        return new Subject() {
            @Override public UUID id() { return player.getUniqueId(); }
            @Override public String name() { return player.getName(); }
            @Override public String world() { return player.getWorld().getName(); }
            @Override public boolean has(String permission) { return player.hasPermission(permission); }
        };
    }

    /** A plain subject, for tests and tools. */
    static Subject of(UUID id, String name, String world, Predicate<String> permissions) {
        return new Subject() {
            @Override public UUID id() { return id; }
            @Override public String name() { return name; }
            @Override public String world() { return world; }
            @Override public boolean has(String permission) { return permissions.test(permission); }
        };
    }
}
