package com.extantape.mayor.config;

import com.extantape.mayor.rules.CommandRules;
import com.extantape.mayor.rules.Reach;
import com.extantape.mayor.rules.WordFilter;

import java.util.List;
import java.util.Map;

/** Everything read from config.yml, filter.yml and quips.yml, ready to use. Replaced whole on reload. */
public record Settings(
        Reach reach,
        CommandRules commands,
        WordFilter filter,
        Filtered filtered,
        NobodyHeard nobodyHeard,
        List<String> quips,
        Map<String, String> messages) {

    /** A short sentence for the log about the word filter. */
    public String filterSummary() {
        return filter.isEmpty() ? "The word filter is off." : "The word filter is on.";
    }

    /** Which kinds of text the word filter checks. */
    public record Filtered(boolean chat, boolean messages, boolean signs, boolean books, boolean itemNames) {
    }

    /** The note players get when nobody could hear their chat. */
    public record NobodyHeard(boolean enabled, int cooldownSeconds) {
    }
}
