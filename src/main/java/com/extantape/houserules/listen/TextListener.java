package com.extantape.houserules.listen;

import com.extantape.houserules.HouseRulesPlugin;
import com.extantape.houserules.config.Settings;
import com.extantape.houserules.rules.Subject;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.event.player.PlayerEditBookEvent;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.inventory.view.AnvilView;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Signs, books and item names follow the same rule as chat: where a player can't chat, they can't write either.
 * The word filter checks them too. Blocked text is never stored or logged: the sign stays as it was, the book keeps
 * its old pages, and the anvil offers no result.
 */
public final class TextListener implements Listener {
    private static final PlainTextComponentSerializer PLAIN = PlainTextComponentSerializer.plainText();
    /** The anvil updates on every key press, so its note is shown at most this often. */
    private static final long ANVIL_NOTE_MILLIS = 5000;
    private final HouseRulesPlugin plugin;
    private final Map<UUID, Long> anvilNotes = new ConcurrentHashMap<>();

    public TextListener(HouseRulesPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onSign(SignChangeEvent event) {
        List<String> lines = new ArrayList<>();
        for (Component line : event.lines()) lines.add(line == null ? "" : PLAIN.serialize(line));
        String refusal = refusal(event.getPlayer(), lines, Settings.Filtered::signs, "filtered-sign");
        if (refusal != null) {
            event.setCancelled(true);
            plugin.tell(event.getPlayer(), refusal);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBook(PlayerEditBookEvent event) {
        BookMeta book = event.getNewBookMeta();
        List<String> text = new ArrayList<>();
        for (Component page : book.pages()) text.add(PLAIN.serialize(page));
        if (event.isSigning() && book.hasTitle()) text.add(PLAIN.serialize(book.title()));
        String refusal = refusal(event.getPlayer(), text, Settings.Filtered::books, "filtered-book");
        if (refusal != null) {
            event.setCancelled(true);
            plugin.tell(event.getPlayer(), refusal);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onAnvil(PrepareAnvilEvent event) {
        AnvilView view = event.getView();
        String name = view.getRenameText();
        if (event.getResult() == null || name == null || name.isBlank() || !(view.getPlayer() instanceof Player player)) return;
        String refusal = refusal(player, List.of(name), Settings.Filtered::itemNames, "filtered-item-name");
        if (refusal == null) return;
        event.setResult(null);
        long now = System.currentTimeMillis();
        Long last = anvilNotes.get(player.getUniqueId());
        if (last == null || now - last > ANVIL_NOTE_MILLIS) {
            anvilNotes.put(player.getUniqueId(), now);
            plugin.tell(player, refusal);
        }
    }

    /** The message key explaining why this text can't be written, or null if it's fine. */
    private String refusal(Player player, List<String> text, java.util.function.Predicate<Settings.Filtered> check, String filteredKey) {
        HouseRulesPlugin.State state = plugin.state();
        if (state == null) return "writing-paused";
        Settings settings = state.settings();
        if (!settings.reach().maySpeak(Subject.of(player))) return "writing-closed-here";
        if (check.test(settings.filtered())) for (String part : text) if (settings.filter().flags(part)) return filteredKey;
        return null;
    }
}
