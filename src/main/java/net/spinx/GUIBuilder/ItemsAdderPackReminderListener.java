package net.spinx.GUIBuilder;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class ItemsAdderPackReminderListener implements Listener {

    private final Main plugin;

    public ItemsAdderPackReminderListener(Main plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        if (!plugin.shouldRemindPackInstallation()) return;

        Player player = event.getPlayer();
        if (!player.isOp()) return;

        player.sendMessage(ChatColor.GOLD + "[GUIBuilder] "
                + ChatColor.YELLOW + plugin.messages().get("reminder.itemsadder_missing"));
    }
}
