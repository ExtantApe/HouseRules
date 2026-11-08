package com.extantape.houserules.text;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class QuipsTest {
    private final UUID player = UUID.randomUUID();

    @Test
    void everyNoteBeforeAnyRepeatAndNeverTwiceInARow() {
        List<String> notes = List.of("a", "b", "c", "d", "e");
        Quips quips = new Quips(notes, 0, new Random(7));
        String previous = null;
        for (int round = 0; round < 50; round++) {
            Set<String> seen = new HashSet<>();
            for (int i = 0; i < notes.size(); i++) {
                String note = quips.next(player, round * 100L + i);
                assertNotEquals(previous, note);
                seen.add(note);
                previous = note;
            }
            assertEquals(Set.copyOf(notes), seen);
        }
    }

    @Test
    void cooldown() {
        Quips quips = new Quips(List.of("a", "b"), 180, new Random(1));
        quips.next(player, 0);
        assertNull(quips.next(player, 179_999));
        assertEquals(1, quips.next(player, 180_000).length());
    }

    @Test
    void emptyIsQuiet() {
        assertNull(new Quips(List.of(), 0, new Random()).next(player, 0));
    }
}
