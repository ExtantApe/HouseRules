package com.extantape.mayor.text;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

/**
 * Hands out the "nobody heard you" notes. Each player goes through the whole list in a shuffled order before any
 * note comes round again, and never gets the same note twice in a row. A cooldown keeps it from nagging.
 * Safe to call from the async chat thread.
 */
public final class Quips {
    private final List<String> notes;
    private final long cooldownMillis;
    private final Random random;
    private final Map<UUID, Deque<Integer>> bags = new HashMap<>();
    private final Map<UUID, Integer> lastShown = new HashMap<>();
    private final Map<UUID, Long> lastTime = new HashMap<>();

    public Quips(List<String> notes, int cooldownSeconds, Random random) {
        this.notes = List.copyOf(notes);
        this.cooldownMillis = cooldownSeconds * 1000L;
        this.random = random;
    }

    /** The next note for this player, or null if there are none or the cooldown hasn't passed. */
    public synchronized String next(UUID player, long now) {
        if (notes.isEmpty()) return null;
        Long last = lastTime.get(player);
        if (last != null && now - last < cooldownMillis) return null;
        Deque<Integer> bag = bags.computeIfAbsent(player, id -> new ArrayDeque<>());
        if (bag.isEmpty()) refill(bag, lastShown.get(player));
        int index = bag.pop();
        lastShown.put(player, index);
        lastTime.put(player, now);
        return notes.get(index);
    }

    public synchronized void forget(UUID player) {
        bags.remove(player);
        lastShown.remove(player);
        lastTime.remove(player);
    }

    private void refill(Deque<Integer> bag, Integer previous) {
        List<Integer> order = new ArrayList<>();
        for (int i = 0; i < notes.size(); i++) order.add(i);
        Collections.shuffle(order, random);
        if (previous != null && order.size() > 1 && order.getFirst().equals(previous)) {
            Collections.swap(order, 0, 1 + random.nextInt(order.size() - 1));
        }
        bag.addAll(order);
    }
}
