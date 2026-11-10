package com.extantape.houserules.command;

import com.extantape.signalbox.Answer;
import com.extantape.signalbox.Args;
import com.extantape.signalbox.Describe;
import com.extantape.signalbox.Errors;
import com.extantape.signalbox.Request;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * The machine-readable side of /houserules (Signalbox format 1): {@code status}, {@code reload}, {@code check} and
 * {@code describe}, each answering exactly one {@code ea-result} line. It's worked out here without a server, so the
 * answers can be tested on their own; the plugin supplies a {@link Host}.
 */
final class Console {
    private Console() {}

    static final String ADMIN = "houserules.admin";

    record Status(boolean paused, List<String> circles, boolean filter, List<String> problems) {}
    record Reloaded(boolean applied, boolean paused, List<String> problems) {}
    record Who(String name, String uuid) {}
    /** {@code why} is Reach.Why in lowercase with hyphens; {@code reason} is the same in plain words. */
    record Checked(Who from, Who to, boolean reaches, boolean maySpeak, String why, String reason, boolean filterBypass) {}

    interface Host {
        String version();
        Status status();
        Reloaded reload();
        /** Null if either player isn't online. Names or uuids, already checked by {@link Args#player}. */
        Checked check(String from, String to);
    }

    static List<Describe.Command> described() {
        return List.of(
                new Describe.Command("status", ADMIN, List.of(), List.of(), false),
                new Describe.Command("reload", ADMIN, List.of(), List.of(), false),
                new Describe.Command("check", ADMIN, List.of(Describe.Arg.of("from", "player"), Describe.Arg.of("to", "player")), List.of(), false),
                new Describe.Command("describe", ADMIN, List.of(), List.of(), false));
    }

    /** The one answer line for a --json request. */
    static String answer(Host host, Request r) {
        List<String> t = r.tokens();
        String sub = t.isEmpty() ? "status" : t.get(0).toLowerCase(Locale.ROOT);
        List<String> rest = t.isEmpty() ? List.of() : t.subList(1, t.size());
        try {
            return switch (sub) {
                case "status" -> { none(rest, "status"); yield status(r.id(), host.status()); }
                case "reload" -> { none(rest, "reload"); yield reloaded(r.id(), host.reload()); }
                case "describe" -> { none(rest, "describe"); yield Answer.ok(r.id(), Describe.body("HouseRules", host.version(), described())); }
                case "check" -> check(host, r.id(), rest);
                default -> Answer.error(r.id(), Errors.UNKNOWN_COMMAND, "There's no /houserules " + sub + ". Try /houserules describe.");
            };
        } catch (Request.Bad bad) {
            return Answer.error(r.id(), bad.code, bad.getMessage());
        } catch (Refused no) {
            return Answer.error(r.id(), no.code, no.getMessage());
        }
    }

    /** A refusal with a base error code, for this class's own checks. */
    private static final class Refused extends Exception {
        final String code;
        Refused(String code, String message) { super(message); this.code = code; }
    }

    private static void none(List<String> rest, String sub) throws Refused {
        if (!rest.isEmpty()) throw new Refused(Errors.BAD_ARGUMENT, "/houserules " + sub + " takes no arguments.");
    }

    private static String status(String id, Status s) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("paused", s.paused());
        m.put("circles", s.circles());
        m.put("filter", s.filter());
        m.put("problems", s.problems());
        return Answer.ok(id, m);
    }

    private static String reloaded(String id, Reloaded r) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("applied", r.applied());
        m.put("paused", r.paused());
        m.put("problems", r.problems());
        return Answer.ok(id, m);
    }

    private static String check(Host host, String id, List<String> rest) throws Request.Bad, Refused {
        if (rest.size() != 2) throw new Refused(Errors.BAD_ARGUMENT, "Usage: /houserules check <from> <to>");
        String from = Args.player(rest.get(0)), to = Args.player(rest.get(1));
        Status s = host.status();
        if (s.paused()) return Answer.error(id, "paused", "Chat is paused until the config is fixed, so nobody reaches anyone. See /houserules status.");
        Checked c = host.check(from, to);
        if (c == null) return Answer.error(id, Errors.NEEDS_ONLINE_PLAYER, "Both players need to be online.");
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("from", who(c.from()));
        m.put("to", who(c.to()));
        m.put("reaches", c.reaches() && c.maySpeak());
        m.put("why", c.why());
        m.put("maySpeak", c.maySpeak());
        m.put("filterBypass", c.filterBypass());
        m.put("reason", c.reason());
        return Answer.ok(id, m);
    }

    private static Map<String, Object> who(Who w) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("name", w.name());
        m.put("uuid", w.uuid());
        return m;
    }
}
