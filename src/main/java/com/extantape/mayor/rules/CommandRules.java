package com.extantape.mayor.rules;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;

/**
 * Decides what happens to a command a player types: blocked, a private message to check, a reply to check, or
 * nothing to do with Mayor. Labels match in any letter case, and a label without a namespace also matches its
 * namespaced forms: "msg" covers "minecraft:msg" and "other:msg". A label with a namespace matches only itself.
 */
public final class CommandRules {
    /** A list of commands that are off, and the message players see when they try one. */
    public record Blocked(Set<String> commands, String message) {
        public Blocked {
            commands = lower(commands);
        }
    }

    public enum Kind { PASS, BLOCK, DELIVER }

    /**
     * The outcome. For BLOCK, either {@code key} names a message in messages, or {@code text} is the message itself.
     * For DELIVER, {@code partner} is who the message goes to, remembered for replies.
     */
    public record Decision(Kind kind, String key, String text, Map<String, String> vars, Subject partner) {
        static final Decision PASS = new Decision(Kind.PASS, null, null, Map.of(), null);

        static Decision block(String key, Map<String, String> vars) {
            return new Decision(Kind.BLOCK, key, null, vars, null);
        }

        static Decision blockText(String text) {
            return new Decision(Kind.BLOCK, null, text, Map.of(), null);
        }

        static Decision deliver(Subject partner) {
            return new Decision(Kind.DELIVER, null, null, Map.of(), partner);
        }

        public boolean blocked() {
            return kind == Kind.BLOCK;
        }
    }

    private final Reach reach;
    private final WordFilter filter;
    private final boolean filterMessages;
    private final Set<String> messageCommands;
    private final Set<String> replyCommands;
    private final Set<String> broadcastCommands;
    private final List<Blocked> blocked;

    public CommandRules(Reach reach, WordFilter filter, boolean filterMessages, Set<String> messageCommands,
                        Set<String> replyCommands, Set<String> broadcastCommands, List<Blocked> blocked) {
        this.reach = reach;
        this.filter = filter;
        this.filterMessages = filterMessages;
        this.messageCommands = lower(messageCommands);
        this.replyCommands = lower(replyCommands);
        this.broadcastCommands = lower(broadcastCommands);
        this.blocked = List.copyOf(blocked);
    }

    /**
     * @param from    who typed it
     * @param line    the command line, with or without its leading slash
     * @param paused  true while the config is broken and chat is paused
     * @param online  finds an online player by exact name, in any letter case, or null
     * @param partner who {@code from} last messaged or was messaged by, or null
     * @param byId    finds an online player by id, or null
     */
    public Decision decide(Subject from, String line, boolean paused, Function<String, Subject> online,
                           UUID partner, Function<UUID, Subject> byId) {
        String trimmed = line.startsWith("/") ? line.substring(1) : line;
        String[] parts = trimmed.trim().split("\\s+");
        String label = parts[0].toLowerCase(Locale.ROOT);
        if (label.isEmpty()) return Decision.PASS;
        String[] args = Arrays.copyOfRange(parts, 1, parts.length);

        for (Blocked entry : blocked) if (matches(entry.commands(), label)) return Decision.blockText(entry.message());
        if (reach.active() && !from.has(Reach.BYPASS) && matches(broadcastCommands, label)) {
            return Decision.block("broadcast-off", Map.of("command", "/" + parts[0]));
        }

        if (matches(messageCommands, label)) {
            if (args.length == 0) return Decision.PASS;
            if (paused) return Decision.block("chat-paused", Map.of());
            if (args[0].startsWith("@") && reach.active() && !from.has(Reach.BYPASS)) return Decision.block("message-selector", Map.of());
            if (!reach.maySpeak(from)) return Decision.block("chat-closed-here", Map.of());
            Subject to = online.apply(args[0]);
            if (to == null || to.id().equals(from.id())) return Decision.PASS;
            if (!reach.mayReach(from, to)) return Decision.block("message-cant-reach", Map.of("player", to.name()));
            if (flagged(from, args, 1)) return Decision.block("filtered-message", Map.of());
            return Decision.deliver(to);
        }

        if (matches(replyCommands, label)) {
            if (paused) return Decision.block("chat-paused", Map.of());
            if (!reach.maySpeak(from)) return Decision.block("chat-closed-here", Map.of());
            if (partner == null) return Decision.block("reply-nobody", Map.of());
            Subject to = byId.apply(partner);
            if (to == null) return Decision.block("reply-offline", Map.of());
            if (!reach.mayReach(from, to)) return Decision.block("reply-cant-reach", Map.of("player", to.name()));
            if (flagged(from, args, 0)) return Decision.block("filtered-message", Map.of());
            return Decision.deliver(to);
        }
        return Decision.PASS;
    }

    private boolean flagged(Subject sender, String[] args, int from) {
        return filterMessages && from < args.length && !sender.has(WordFilter.BYPASS)
                && filter.flags(String.join(" ", Arrays.copyOfRange(args, from, args.length)));
    }

    static boolean matches(Set<String> entries, String label) {
        if (entries.contains(label)) return true;
        int colon = label.indexOf(':');
        return colon >= 0 && entries.contains(label.substring(colon + 1));
    }

    private static Set<String> lower(Set<String> labels) {
        return Set.copyOf(labels.stream().map(s -> {
            String l = s.trim().toLowerCase(Locale.ROOT);
            return l.startsWith("/") ? l.substring(1) : l;
        }).toList());
    }
}
