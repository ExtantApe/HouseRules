package com.extantape.mayor.config;

import com.extantape.mayor.rules.Reach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SettingsLoaderTest {
    private SettingsLoader.Result load(String config, String filter, String quips) {
        return new SettingsLoader(n -> SettingsLoader.class.getResourceAsStream("/" + n)).load(config, filter, quips);
    }

    @Test
    void noConfigIsVanillaChat() {
        SettingsLoader.Result r = load(null, null, null);
        assertTrue(r.ok(), r.errors().toString());
        assertTrue(r.warnings().isEmpty(), r.warnings().toString());
        assertFalse(r.settings().reach().active());
        assertTrue(r.settings().quips().size() >= 50, "a large pool of notes");
    }

    @Test
    void circlesAndGroups() {
        SettingsLoader.Result r = load("""
                circles: [Red, blue]
                circle-only-worlds: [hall]
                world-groups:
                  home: [meadow, meadow_flat]
                blocked-commands:
                  - commands: [sort]
                    message: "<gray>No."
                """, null, null);
        assertTrue(r.ok(), r.errors().toString());
        Reach reach = r.settings().reach();
        assertEquals(java.util.List.of("red", "blue"), reach.circles());
        assertEquals("home", reach.group("meadow_flat"));
        assertTrue(reach.isCircleOnly("hall_nether"));
    }

    @Test
    void brokenYamlIsAnErrorWithNoSettings() {
        SettingsLoader.Result r = load("circles: [red\nfoo: : :", null, null);
        assertFalse(r.ok());
        assertTrue(r.errors().getFirst().startsWith("config.yml: isn't valid YAML"), r.errors().toString());
    }

    @Test
    void unclearRulesAreErrors() {
        assertFalse(load("circles: [\"has space\"]", null, null).ok());
        assertFalse(load("circles: red", null, null).ok());
        assertFalse(load("world-groups:\n  a: [x]\n  b: [x]\n", null, null).ok());
        assertFalse(load("blocked-commands:\n  - commands: [x]\n", null, null).ok());
        assertFalse(load("nobody-heard:\n  enabled: maybe\n", null, null).ok());
        assertFalse(load(null, "words: nope", null).ok());
    }

    @Test
    void smallSlipsAreWarnings() {
        SettingsLoader.Result r = load("circls: [red]\nmessages:\n  chat-pausd: hi\n", null, null);
        assertTrue(r.ok());
        assertEquals(2, r.warnings().size(), r.warnings().toString());
    }

    @Test
    void messagesOverrideDefaults() {
        SettingsLoader.Result r = load("messages:\n  chat-closed-here: \"<gray>Try talking to the guide!\"\n", null, null);
        assertEquals("<gray>Try talking to the guide!", r.settings().messages().get("chat-closed-here"));
        assertTrue(r.settings().messages().get("filtered-chat").contains("wasn't sent"));
    }

    @Test
    void filterCanBeTurnedOff() {
        assertTrue(load(null, "enabled: false", null).settings().filter().isEmpty());
    }
}
