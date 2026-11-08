package com.extantape.houserules.rules;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Who can hear whom. Two players reach each other if they're in the same world group, unless it's a circle-only
 * world, or if they share a circle. With no circles configured, everyone reaches everyone, as in vanilla.
 */
public final class Reach {
    public static final String CIRCLE_PERMISSION = "houserules.circle.";
    public static final String BYPASS = "houserules.bypass";

    /** Why a player can or can't reach another, for /houserules check. */
    public enum Why {
        RULES_OFF(true), SELF(true), BYPASS(true), SAME_WORLD(true), SHARED_CIRCLE(true),
        CIRCLE_ONLY_WORLD(false), NOT_SHARED(false);

        public final boolean reaches;

        Why(boolean reaches) {
            this.reaches = reaches;
        }
    }

    private final List<String> circles;
    private final Map<String, String> groupOf;
    private final List<String> suffixes;
    private final Set<String> circleOnly = new HashSet<>();

    /**
     * @param circles          circle names; each is the permission {@code houserules.circle.<name>}
     * @param circleOnlyWorlds worlds where only a shared circle counts
     * @param groupOf          world name to world group name, for worlds listed under world-groups
     * @param suffixes         suffixes that join a world to its base world, such as {@code _nether}
     */
    public Reach(List<String> circles, Set<String> circleOnlyWorlds, Map<String, String> groupOf, List<String> suffixes) {
        this.circles = List.copyOf(circles);
        this.groupOf = Map.copyOf(groupOf);
        this.suffixes = List.copyOf(suffixes);
        for (String world : circleOnlyWorlds) circleOnly.add(group(world));
    }

    /** Whether any rule applies. False means vanilla chat. */
    public boolean active() {
        return !circles.isEmpty();
    }

    public List<String> circles() {
        return circles;
    }

    /** The world group a world belongs to: listed groups first, then its base world by suffix, then itself. */
    public String group(String world) {
        String listed = groupOf.get(world);
        if (listed != null) return listed;
        for (String suffix : suffixes) {
            if (world.length() > suffix.length() && world.endsWith(suffix)) {
                String base = world.substring(0, world.length() - suffix.length());
                return groupOf.getOrDefault(base, base);
            }
        }
        return world;
    }

    public boolean isCircleOnly(String world) {
        return circleOnly.contains(group(world));
    }

    public List<String> circlesOf(Subject player) {
        List<String> in = new ArrayList<>();
        for (String circle : circles) if (player.has(CIRCLE_PERMISSION + circle)) in.add(circle);
        return in;
    }

    /** Whether the player may say anything at all where they stand. Only false in a circle-only world with no circle. */
    public boolean maySpeak(Subject player) {
        if (!active() || player.has(BYPASS)) return true;
        return !isCircleOnly(player.world()) || !circlesOf(player).isEmpty();
    }

    public boolean mayReach(Subject from, Subject to) {
        return why(from, to).reaches;
    }

    public Why why(Subject from, Subject to) {
        if (!active()) return Why.RULES_OFF;
        if (from.id().equals(to.id())) return Why.SELF;
        if (from.has(BYPASS) || to.has(BYPASS)) return Why.BYPASS;
        String world = group(from.world());
        boolean sameWorld = world.equals(group(to.world()));
        if (sameWorld && !circleOnly.contains(world)) return Why.SAME_WORLD;
        if (sharedCircle(from, to) != null) return Why.SHARED_CIRCLE;
        return sameWorld ? Why.CIRCLE_ONLY_WORLD : Why.NOT_SHARED;
    }

    /** The first circle both players are in, or null. */
    public String sharedCircle(Subject from, Subject to) {
        for (String circle : circles) {
            String node = CIRCLE_PERMISSION + circle;
            if (from.has(node) && to.has(node)) return circle;
        }
        return null;
    }
}
