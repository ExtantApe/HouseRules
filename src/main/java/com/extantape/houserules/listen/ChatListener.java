package com.extantape.houserules.listen;

import com.extantape.houserules.HouseRulesPlugin;
import com.extantape.houserules.config.Settings;
import com.extantape.houserules.rules.Reach;
import com.extantape.houserules.rules.Subject;
import com.extantape.houserules.rules.WordFilter;
import com.extantape.houserules.text.Messages;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import java.util.Map;

/** Public chat goes only to the players the speaker can reach. Runs on the async chat thread and never blocks it. */
public final class ChatListener implements Listener {
    private final HouseRulesPlugin plugin;

    public ChatListener(HouseRulesPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onChat(AsyncChatEvent event) {
        Player from = event.getPlayer();
        HouseRulesPlugin.State state = plugin.state();
        if (state == null) {
            event.setCancelled(true);
            plugin.tell(from, "chat-paused");
            return;
        }
        Settings settings = state.settings();
        Reach reach = settings.reach();
        Subject me = Subject.of(from);
        if (!reach.maySpeak(me)) {
            event.setCancelled(true);
            plugin.tell(from, "chat-closed-here");
            return;
        }
        if (settings.filtered().chat() && !me.has(WordFilter.BYPASS) && settings.filter().flags(PlainTextComponentSerializer.plainText().serialize(event.message()))) {
            event.setCancelled(true);
            plugin.tell(from, "filtered-chat");
            return;
        }
        if (!reach.active()) return;
        event.viewers().removeIf(viewer -> viewer instanceof Player to && !reach.mayReach(me, Subject.of(to)));

        if (!settings.nobodyHeard().enabled()) return;
        boolean heard = event.viewers().stream().anyMatch(v -> v instanceof Player p && !p.equals(from));
        if (heard || Bukkit.getOnlinePlayers().size() < 2) return;
        String note = state.quips().next(from.getUniqueId(), System.currentTimeMillis());
        // A tick later, so the note shows under the player's own line.
        if (note != null) Bukkit.getScheduler().runTask(plugin, () -> from.sendMessage(Messages.parse(note, Map.of())));
    }
}
