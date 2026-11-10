package com.extantape.houserules.command;

import com.extantape.signalbox.Request;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** The --json answers for /houserules, with a stand-in for the server. */
class ConsoleTest {
    static final Console.Who ANN = new Console.Who("Ann", "00000000-0000-0000-0000-00000000000a");
    static final Console.Who BEN = new Console.Who("Ben", "00000000-0000-0000-0000-00000000000b");

    final List<String> asked = new ArrayList<>();
    boolean paused;
    Console.Checked checked = new Console.Checked(ANN, BEN, true, true, "shared-circle", "they share the circle red", false);

    final Console.Host host = new Console.Host() {
        @Override public String version() { return "0.3.0"; }
        @Override public Console.Status status() { return new Console.Status(paused, paused ? List.of() : List.of("red", "blue"), true, List.of()); }
        @Override public Console.Reloaded reload() { asked.add("reload"); return new Console.Reloaded(true, false, List.of("a warning")); }
        @Override public Console.Checked check(String from, String to) { asked.add(from + ">" + to); return checked; }
    };

    String ask(String line) throws Request.Bad { return Console.answer(host, Request.parse(line.split(" "))); }

    @Test void describeListsEveryCommandWithFormatOne() throws Exception {
        String a = ask("describe --json d1");
        assertTrue(a.startsWith("ea-result d1 {"), a);
        for (String c : List.of("\"status\"", "\"reload\"", "\"check\"", "\"describe\"")) assertTrue(a.contains(c), c + " in " + a);
        assertTrue(a.contains("\"format\":1") && a.contains("\"plugin\":\"HouseRules\"") && a.contains("\"version\":\"0.3.0\""), a);
        assertTrue(a.contains("{\"name\":\"from\",\"type\":\"player\"}"), a);
    }

    @Test void statusSaysPausedCirclesFilterAndProblems() throws Exception {
        String a = ask("status --json s1");
        assertTrue(a.contains("\"ok\":true") && a.contains("\"paused\":false") && a.contains("\"circles\":[\"red\",\"blue\"]") && a.contains("\"filter\":true"), a);
        assertEquals(a, ask("--json s1"), "no subcommand means status");
    }

    @Test void reloadAppliesAndListsWhatItFound() throws Exception {
        String a = ask("reload --json r1");
        assertEquals(List.of("reload"), asked);
        assertTrue(a.contains("\"applied\":true") && a.contains("\"problems\":[\"a warning\"]"), a);
    }

    @Test void checkGivesBothPlayersWithUuidsAndAStableReasonCode() throws Exception {
        String a = ask("check Ann Ben --json c1");
        assertTrue(a.contains("\"reaches\":true") && a.contains("\"why\":\"shared-circle\"") && a.contains("\"uuid\":\"" + ANN.uuid() + "\""), a);
    }

    @Test void someoneWhoCantSpeakThereReachesNobodyEvenInACircle() throws Exception {
        checked = new Console.Checked(ANN, BEN, true, false, "shared-circle", "they share the circle red", false);
        String a = ask("check Ann Ben --json c2");
        assertTrue(a.contains("\"reaches\":false") && a.contains("\"maySpeak\":false"), a);
    }

    @Test void anOfflinePlayerIsSaidPlainly() throws Exception {
        checked = null;
        assertTrue(ask("check Ann Zed --json c3").contains("\"code\":\"needs-online-player\""));
    }

    @Test void whilePausedCheckRefusesInsteadOfGuessing() throws Exception {
        paused = true;
        String a = ask("check Ann Ben --json c4");
        assertTrue(a.contains("\"ok\":false") && a.contains("\"code\":\"paused\""), a);
        assertTrue(asked.isEmpty(), "nobody was looked up");
    }

    @Test void everyBadRequestStillGetsExactlyOneAnswer() throws Exception {
        for (String line : List.of("nonsense --json e1", "check Ann --json e2", "check Ann B@d --json e3", "status extra --json e4", "reload now --json e5")) {
            String a = ask(line);
            assertTrue(a.startsWith("ea-result e") && a.contains("\"ok\":false") && !a.contains("\n"), line + " -> " + a);
        }
        assertTrue(ask("nonsense --json e1").contains("unknown-command"));
        assertTrue(ask("check Ann --json e2").contains("bad-argument"));
        assertTrue(ask("check Ann B@d --json e3").contains("bad-argument"));
        assertTrue(asked.isEmpty(), "nothing was reloaded or looked up for a bad request");
    }
}
