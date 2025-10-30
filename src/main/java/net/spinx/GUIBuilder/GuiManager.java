package net.spinx.GUIBuilder;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.*;

public class GuiManager {

    public enum Mode { VIEW, EDIT }

    private final Map<String, GuiData> guis = new HashMap<>();
    private final Map<UUID, String> editingByPlayer = new HashMap<>();
    private final Main plugin;
    private final GuiStorage storage;

    public GuiManager(Main plugin, GuiStorage storage) {
        this.plugin = plugin;
        this.storage = storage;
    }

    public record GuiData(String name, int rows, ItemStack[] contents) {}

    public Set<String> getAllNames() { return new TreeSet<>(guis.keySet()); }
    public GuiData get(String name) { return guis.get(name.toLowerCase(Locale.ROOT)); }
    public boolean exists(String name) { return guis.containsKey(name.toLowerCase(Locale.ROOT)); }

    public void put(String name, int rows, ItemStack[] contents) {
        guis.put(name.toLowerCase(Locale.ROOT), new GuiData(name, rows, normalize(contents, rows)));
    }

    private ItemStack[] normalize(ItemStack[] input, int rows) {
        ItemStack[] arr = new ItemStack[rows * 9];
        if (input != null) System.arraycopy(input, 0, arr, 0, Math.min(input.length, arr.length));
        return arr;
    }

    // ------ Public Aktionen ------

    public void createNew(Player p, String name, int rows) {
        if (exists(name)) { p.sendMessage(ChatColor.RED + "Diese GUI existiert bereits."); return; }
        rows = clampRows(rows);
        put(name, rows, new ItemStack[rows * 9]);
        storage.save(get(name)); // sofort anlegen
        openEdit(p, name, Optional.empty());
        p.sendMessage(ChatColor.GREEN + "Neue GUI '" + name + "' mit " + rows + " Reihen erstellt (Bearbeiten).");
    }

    public void openView(Player p, String name) {
        GuiData data = get(name);
        if (data == null) { p.sendMessage(ChatColor.RED + "Diese GUI gibt es nicht."); return; }
        Inventory inv = Bukkit.createInventory(new NamedHolder(data.name(), Mode.VIEW), data.rows() * 9,
                ChatColor.DARK_AQUA + "GUI: " + data.name());
        inv.setContents(cloneItems(data.contents()));
        p.openInventory(inv);
    }

    public void openEdit(Player p, String name, Optional<Integer> maybeNewRows) {
        GuiData data = get(name);
        if (data == null) { p.sendMessage(ChatColor.RED + "Diese GUI gibt es nicht."); return; }

        int rows = data.rows();
        ItemStack[] base = data.contents();

        if (maybeNewRows.isPresent()) {
            rows = clampRows(maybeNewRows.get());
            ItemStack[] resized = new ItemStack[rows * 9];
            System.arraycopy(base, 0, resized, 0, Math.min(base.length, resized.length));
            base = resized;
            put(data.name(), rows, base);
            storage.save(get(name)); // Größe sofort persistieren
        }

        Inventory inv = Bukkit.createInventory(new NamedHolder(data.name(), Mode.EDIT), rows * 9,
                ChatColor.DARK_GREEN + "EDIT: " + data.name());
        inv.setContents(cloneItems(base));
        p.openInventory(inv);
        editingByPlayer.put(p.getUniqueId(), data.name().toLowerCase(Locale.ROOT));
    }

    public boolean delete(String name) {
        String key = name.toLowerCase(Locale.ROOT);
        GuiData removed = guis.remove(key);
        if (removed == null) return false;

        // Edit-Sessions bereinigen
        editingByPlayer.entrySet().removeIf(e -> e.getValue().equalsIgnoreCase(key));

        // Spieler, die diese GUI offen haben, schließen
        for (Player p : Bukkit.getOnlinePlayers()) {
            Inventory top = p.getOpenInventory() != null ? p.getOpenInventory().getTopInventory() : null;
            if (top != null && top.getHolder() instanceof NamedHolder holder) {
                if (holder.name().equalsIgnoreCase(removed.name())) {
                    p.closeInventory();
                    p.sendMessage(ChatColor.RED + "GUI '" + removed.name() + "' wurde gelöscht.");
                }
            }
        }

        // Datei löschen
        boolean deleted = storage.delete(removed.name());
        if (!deleted) plugin.getLogger().warning("Konnte Datei für GUI '" + removed.name() + "' nicht löschen.");
        return true;
    }

    // ------ Listener-Hooks ------

    public void handleClose(Player p, NamedHolder holder, ItemStack[] contents) {
        if (holder.mode() == Mode.EDIT) {
            GuiData old = guis.get(holder.name().toLowerCase(Locale.ROOT));
            if (old != null) {
                put(old.name(), old.rows(), contents);
                storage.save(get(old.name())); // Änderungen sichern
            }
            editingByPlayer.remove(p.getUniqueId());
            p.sendMessage(ChatColor.YELLOW + "GUI '" + holder.name() + "' gespeichert.");
        }
    }

    public boolean isPlayerEditing(Player p, NamedHolder holder) {
        return holder.mode() == Mode.EDIT &&
                editingByPlayer.getOrDefault(p.getUniqueId(), "").equalsIgnoreCase(holder.name());
    }

    // ------ Utils ------

    private int clampRows(int r) { return Math.max(1, Math.min(6, r)); }

    private ItemStack[] cloneItems(ItemStack[] src) {
        ItemStack[] out = new ItemStack[src.length];
        for (int i = 0; i < src.length; i++) out[i] = (src[i] == null) ? null : src[i].clone();
        return out;
    }

    public record NamedHolder(String name, Mode mode) implements org.bukkit.inventory.InventoryHolder {
        @Override public Inventory getInventory() { return null; }
    }
}
