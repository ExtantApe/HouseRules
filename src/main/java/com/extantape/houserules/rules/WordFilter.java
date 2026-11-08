package com.extantape.houserules.rules;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Finds blocked words and phrases in what players write. It matches whole words only, so "class" and "Scunthorpe"
 * pass. It sees through letter case, accents, common number swaps (sh1t) and stretched letters (shiiit). A trailing
 * {@code *} matches word endings too (swear* matches swearing). Words on the allow list are never flagged.
 */
public final class WordFilter {
    /** Players with this permission aren't filtered anywhere. Off for everyone by default, ops included. */
    public static final String BYPASS = "houserules.filter.bypass";
    public static final WordFilter NONE = new WordFilter(List.of(), Set.of());
    private static final Pattern SPLIT = Pattern.compile("[^\\p{L}\\p{N}@$]+");
    private static final Pattern MARKS = Pattern.compile("\\p{M}+");

    private final List<List<Pattern>> phrases;
    private final Set<String> allow;

    private WordFilter(List<List<Pattern>> phrases, Set<String> allow) {
        this.phrases = phrases;
        this.allow = allow;
    }

    public static WordFilter of(List<String> words, List<String> allowed) {
        List<List<Pattern>> phrases = new ArrayList<>();
        for (String entry : words) {
            List<Pattern> tokens = new ArrayList<>();
            String[] parts = entry.trim().split("\\s+");
            for (int i = 0; i < parts.length; i++) {
                boolean prefix = i == parts.length - 1 && parts[i].endsWith("*");
                String word = normalize(prefix ? parts[i].substring(0, parts[i].length() - 1) : parts[i]);
                if (!word.isEmpty()) tokens.add(pattern(word, prefix));
            }
            if (!tokens.isEmpty()) phrases.add(List.copyOf(tokens));
        }
        Set<String> allow = new HashSet<>();
        for (String word : allowed) allow.add(normalize(word));
        return new WordFilter(List.copyOf(phrases), Set.copyOf(allow));
    }

    public boolean isEmpty() {
        return phrases.isEmpty();
    }

    /** True if the text contains a blocked word or phrase. The text itself is never stored or logged. */
    public boolean flags(String text) {
        if (phrases.isEmpty() || text == null || text.isBlank()) return false;
        List<String> tokens = tokens(text);
        for (int start = 0; start < tokens.size(); start++) {
            for (List<Pattern> phrase : phrases) {
                if (start + phrase.size() > tokens.size()) continue;
                boolean hit = true;
                for (int i = 0; i < phrase.size() && hit; i++) {
                    String token = tokens.get(start + i);
                    hit = !allow.contains(token) && phrase.get(i).matcher(token).matches();
                }
                if (hit) return true;
            }
        }
        return false;
    }

    static List<String> tokens(String text) {
        List<String> out = new ArrayList<>();
        for (String raw : SPLIT.split(text)) {
            String token = normalize(raw);
            if (!token.isEmpty()) out.add(token);
        }
        return out;
    }

    /** Lower case, no accents, and the usual number and symbol swaps turned back into letters. */
    static String normalize(String word) {
        String plain = MARKS.matcher(Normalizer.normalize(word, Normalizer.Form.NFKD)).replaceAll("").toLowerCase(Locale.ROOT);
        StringBuilder out = new StringBuilder(plain.length());
        for (int i = 0; i < plain.length(); i++) {
            char c = plain.charAt(i);
            out.append(switch (c) {
                case '0' -> 'o';
                case '1' -> 'i';
                case '3' -> 'e';
                case '4', '@' -> 'a';
                case '5', '$' -> 's';
                case '7' -> 't';
                case '8' -> 'b';
                default -> c;
            });
        }
        return out.toString();
    }

    /** Each run of a letter may be stretched but not shortened: "ass" matches "asss" but never "as". */
    private static Pattern pattern(String word, boolean prefix) {
        StringBuilder regex = new StringBuilder();
        int i = 0;
        while (i < word.length()) {
            int j = i;
            while (j < word.length() && word.charAt(j) == word.charAt(i)) j++;
            regex.append(Pattern.quote(String.valueOf(word.charAt(i)))).append('{').append(j - i).append(",}");
            i = j;
        }
        if (prefix) regex.append(".*");
        return Pattern.compile(regex.toString());
    }
}
