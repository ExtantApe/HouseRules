package com.extantape.mayor.rules;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static com.extantape.mayor.rules.Players.circle;
import static com.extantape.mayor.rules.Players.player;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class CommandRulesTest {
    private final Reach reach = new Reach(List.of("red"), Set.of("hall"), Map.of(), List.of("_nether"));
    private final CommandRules rules = new CommandRules(reach, WordFilter.of(List.of("heck"), List.of()), true,
            Set.of("msg", "tell", "w"), Set.of("r", "reply"), Set.of("me"),
            List.of(new CommandRules.Blocked(Set.of("/Sort", "other:only"), "<gray>Sorting stays on.")));
    private final Map<String, Subject> online = new HashMap<>();

    private final Subject alex = add(player("Alex", "forest"));
    private final Subject sam = add(player("Sam", "forest_nether"));
    private final Subject far = add(player("Far", "meadow"));
    private final Subject redFar = add(player("RedFar", "meadow", circle("red")));
    private final Subject redHere = add(player("RedHere", "forest", circle("red")));
    private final Subject newbie = add(player("Newbie", "hall"));

    private Subject add(Subject s) {
        online.put(s.name().toLowerCase(), s);
        return s;
    }

    private CommandRules.Decision run(Subject from, String line) {
        return run(from, line, null);
    }

    private CommandRules.Decision run(Subject from, String line, UUID partner) {
        return rules.decide(from, line, false, n -> online.get(n.toLowerCase()), partner,
                id -> online.values().stream().filter(s -> s.id().equals(id)).findFirst().orElse(null));
    }

    @Test
    void blockedCommandsInEveryForm() {
        for (String line : List.of("/sort", "/SORT", "/SoRt chest", "/chestsort:sort", "/Any:Sort", "/other:only")) {
            CommandRules.Decision d = run(alex, line);
            assertEquals(CommandRules.Kind.BLOCK, d.kind(), line);
            assertEquals("<gray>Sorting stays on.", d.text(), line);
        }
        for (String line : List.of("/only", "/sorted", "/isort", "/spawn", "/x:isort")) assertEquals(CommandRules.Kind.PASS, run(alex, line).kind(), line);
    }

    @Test
    void broadcastCommandsOffWhileCirclesAreOn() {
        assertEquals("broadcast-off", run(alex, "/me waves").key());
        assertEquals("broadcast-off", run(alex, "/minecraft:me waves").key());
        assertEquals(CommandRules.Kind.PASS, run(player("Mod", "forest", Reach.BYPASS), "/me waves").kind());
    }

    @Test
    void privateMessages() {
        assertEquals(CommandRules.Kind.DELIVER, run(alex, "/msg Sam hi").kind());
        assertEquals(CommandRules.Kind.DELIVER, run(alex, "/minecraft:tell sam hi").kind());
        CommandRules.Decision blocked = run(alex, "/w Far hi");
        assertEquals("message-cant-reach", blocked.key());
        assertEquals("Far", blocked.vars().get("player"));
        assertEquals(CommandRules.Kind.DELIVER, run(redHere, "/msg RedFar hi").kind());
        assertEquals("filtered-message", run(alex, "/msg Sam what the HECK").key());
        assertEquals(CommandRules.Kind.PASS, run(alex, "/msg Nobody hi").kind(), "unknown players are left to the command itself");
        assertEquals(CommandRules.Kind.PASS, run(alex, "/msg").kind());
        assertEquals("message-selector", run(alex, "/msg @a hi").key());
        assertEquals("chat-closed-here", run(newbie, "/msg Alex hi").key());
    }

    @Test
    void replies() {
        assertEquals("reply-nobody", run(alex, "/r hi").key());
        assertEquals(CommandRules.Kind.DELIVER, run(alex, "/r hi", sam.id()).kind());
        assertEquals("reply-cant-reach", run(alex, "/reply hi", far.id()).key());
        assertEquals("reply-offline", run(alex, "/r hi", UUID.randomUUID()).key());
        assertEquals("filtered-message", run(alex, "/r heck", sam.id()).key());
    }

    @Test
    void filterBypassSkipsTheFilterForItsHolderOnly() {
        Subject adult = add(player("Adult", "forest", WordFilter.BYPASS));
        assertEquals(CommandRules.Kind.DELIVER, run(adult, "/msg Sam what the heck").kind());
        assertEquals(CommandRules.Kind.DELIVER, run(adult, "/r heck", sam.id()).kind());
        assertEquals("filtered-message", run(sam, "/msg Adult what the heck").key(), "the other player is still filtered");
        assertEquals("message-cant-reach", run(adult, "/msg Far heck").key(), "the filter bypass doesn't widen reach");
    }

    @Test
    void filterBypassIsReadAtEachMessage() {
        Set<String> perms = new HashSet<>();
        Subject changing = add(Subject.of(UUID.randomUUID(), "Changing", "forest", perms::contains));
        assertEquals("filtered-message", run(changing, "/msg Sam heck").key());
        perms.add(WordFilter.BYPASS);
        assertEquals(CommandRules.Kind.DELIVER, run(changing, "/msg Sam heck").kind());
        perms.remove(WordFilter.BYPASS);
        assertEquals("filtered-message", run(changing, "/msg Sam heck").key());
    }

    @Test
    void pausedBlocksMessagesButNotOtherCommands() {
        var d = rules.decide(alex, "/msg Sam hi", true, n -> online.get(n.toLowerCase()), null, id -> null);
        assertEquals("chat-paused", d.key());
        assertEquals(CommandRules.Kind.PASS, rules.decide(alex, "/spawn", true, n -> null, null, id -> null).kind());
        assertNull(rules.decide(alex, "/spawn", true, n -> null, null, id -> null).key());
    }
}
