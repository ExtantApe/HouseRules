package com.extantape.houserules.rules;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.extantape.houserules.rules.Players.circle;
import static com.extantape.houserules.rules.Players.player;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReachTest {
    private final Reach reach = new Reach(List.of("red", "blue"), Set.of("hall"),
            Map.of("meadow", "home", "meadow_flat", "home"), List.of("_nether", "_the_end"));

    @Test
    void noCirclesMeansVanilla() {
        Reach off = new Reach(List.of(), Set.of("hall"), Map.of(), List.of("_nether"));
        assertTrue(off.mayReach(player("a", "one"), player("b", "two")));
        assertTrue(off.maySpeak(player("a", "hall")));
    }

    @Test
    void worldGroups() {
        assertEquals("home", reach.group("meadow"));
        assertEquals("home", reach.group("meadow_nether"));
        assertEquals("home", reach.group("meadow_flat"));
        assertEquals("forest", reach.group("forest_the_end"));
        assertEquals("forest", reach.group("forest"));
        assertEquals("_nether", reach.group("_nether"));
        assertTrue(reach.isCircleOnly("hall_nether"));
    }

    @Test
    void sameWorldGroupReaches() {
        assertTrue(reach.mayReach(player("a", "forest"), player("b", "forest_nether")));
        assertTrue(reach.mayReach(player("a", "meadow_flat"), player("b", "meadow_the_end")));
        assertFalse(reach.mayReach(player("a", "forest"), player("b", "meadow")));
    }

    @Test
    void sharedCircleReachesAcrossWorlds() {
        Subject a = player("a", "forest", circle("red"));
        Subject b = player("b", "meadow", circle("red"), circle("blue"));
        Subject c = player("c", "meadow", circle("blue"));
        assertTrue(reach.mayReach(a, b));
        assertTrue(reach.mayReach(b, c));
        assertFalse(reach.mayReach(a, c));
        assertEquals(Reach.Why.SHARED_CIRCLE, reach.why(a, b));
    }

    @Test
    void circleOnlyWorld() {
        Subject none = player("none", "hall");
        Subject other = player("other", "hall");
        Subject red = player("red1", "hall", circle("red"));
        Subject red2 = player("red2", "forest", circle("red"));
        assertFalse(reach.maySpeak(none));
        assertTrue(reach.maySpeak(red));
        assertFalse(reach.mayReach(none, other));
        assertFalse(reach.mayReach(red, other), "same world doesn't count in a circle-only world");
        assertTrue(reach.mayReach(red, red2), "a shared circle still reaches across worlds");
        assertEquals(Reach.Why.CIRCLE_ONLY_WORLD, reach.why(none, other));
    }

    @Test
    void bypassReachesAndIsReached() {
        Subject mod = player("mod", "forest", Reach.BYPASS);
        Subject kid = player("kid", "meadow", circle("red"));
        assertTrue(reach.mayReach(mod, kid));
        assertTrue(reach.mayReach(kid, mod));
        assertTrue(reach.maySpeak(player("mod2", "hall", Reach.BYPASS)));
    }

    /** Every pair, both ways: reach is symmetric. */
    @Test
    void reachIsSymmetric() {
        List<Subject> all = List.of(
                player("r1", "forest", circle("red")), player("r2", "meadow_nether", circle("red")),
                player("b1", "forest_the_end", circle("blue")), player("b2", "hall", circle("blue")),
                player("n1", "forest"), player("n2", "hall"), player("n3", "meadow"));
        for (Subject a : all) for (Subject b : all) assertEquals(reach.mayReach(a, b), reach.mayReach(b, a), a.name() + "/" + b.name());
    }

    // --- a viewer who isn't in game, such as the web map (D54) ---

    @Test
    void awayViewersSeeOnlyTheirCircle() {
        java.util.function.Predicate<String> kidInRed = circle("red")::equals;
        assertTrue(reach.reachesWhileAway(kidInRed, player("mum", "forest", circle("red"))));
        assertFalse(reach.reachesWhileAway(kidInRed, player("stranger", "forest")), "the same world doesn't count");
        assertFalse(reach.reachesWhileAway(kidInRed, player("blue", "meadow", circle("blue"))));
        assertFalse(reach.reachesWhileAway(kidInRed, player("admin", "forest", Reach.BYPASS)), "bypass doesn't count either");
    }

    @Test
    void anAwayViewerWithNoCircleOrNoRulesSeesNobody() {
        assertFalse(reach.reachesWhileAway(p -> false, player("mum", "forest", circle("red"))));
        assertFalse(reach.reachesWhileAway(p -> true, player("stranger", "forest")));
        Reach off = new Reach(List.of(), Set.of(), Map.of(), List.of());
        assertFalse(off.reachesWhileAway(p -> true, player("anyone", "forest", circle("red"))), "no circles: nothing to share");
    }

    @Test
    void anAwayViewersOwnPermissionsAreTheOnlyOnesAsked() {
        java.util.List<String> asked = new java.util.ArrayList<>();
        reach.reachesWhileAway(p -> { asked.add(p); return false; }, player("mum", "forest", circle("red"), circle("blue")));
        assertTrue(asked.stream().allMatch(p -> p.startsWith(Reach.CIRCLE_PERMISSION)), asked.toString());
    }
}
