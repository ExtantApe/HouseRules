package com.extantape.houserules.text;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Turns the MiniMessage text from config into chat components. Values filled in, such as player names, are always
 * inserted as plain text, so a name can never inject formatting or clickable text.
 */
public final class Messages {
    private static final MiniMessage MINI = MiniMessage.miniMessage();
    private final Map<String, String> templates;

    public Messages(Map<String, String> templates) {
        this.templates = templates;
    }

    public Component get(String key, Map<String, String> vars) {
        String template = templates.get(key);
        return template == null ? Component.text(key) : parse(template, vars);
    }

    public Component get(String key) {
        return get(key, Map.of());
    }

    /** True if the server owner left this message empty, which turns it off. */
    public boolean silenced(String key) {
        String template = templates.get(key);
        return template == null || template.isBlank();
    }

    public static Component parse(String template, Map<String, String> vars) {
        List<TagResolver> resolvers = new ArrayList<>();
        vars.forEach((k, v) -> resolvers.add(Placeholder.unparsed(k, v)));
        return MINI.deserialize(template, TagResolver.resolver(resolvers));
    }
}
