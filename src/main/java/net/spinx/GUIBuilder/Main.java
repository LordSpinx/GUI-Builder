package net.spinx.GUIBuilder;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.Map;
import java.util.Objects;

public class Main extends JavaPlugin {

    private GuiManager guiManager;
    private boolean itemsAdderAvailable;
    private boolean guiBuilderPackPresent;

    @Override
    public void onEnable() {
        this.itemsAdderAvailable = checkItemsAdderAvailable();
        this.guiBuilderPackPresent = itemsAdderAvailable && checkGuiBuilderPackPresent();

        if (!itemsAdderAvailable) {
            getLogger().info("ItemsAdder wurde nicht gefunden. GUIBuilder läuft ohne ItemsAdder-Integration.");
        } else if (!guiBuilderPackPresent) {
            getLogger().warning("ItemsAdder erkannt, aber das GUIBuilder-Pack fehlt. Einige Komfortfunktionen sind deaktiviert.");
        } else {
            getLogger().info("ItemsAdder und GUIBuilder-Pack erkannt. Erweiterte GUI-Benennung aktiviert.");
        }

        // Ordner anlegen
        if (!getDataFolder().exists()) getDataFolder().mkdirs();

        // Storage + Manager
        GuiStorage storage = new GuiStorage(this);
        this.guiManager = new GuiManager(this, storage);

        // Vorhandene GUIs laden
        Map<String, GuiManager.GuiData> loaded = storage.loadAll();
        loaded.values().forEach(d -> guiManager.put(d.name(), d.rows(), d.contents()));
        getLogger().info("Geladene GUIs: " + guiManager.getAllNames());

        // Commands + Listener
        GuiCommand cmd = new GuiCommand(this, guiManager);
        Objects.requireNonNull(getCommand("guibuilder")).setExecutor(cmd);
        Objects.requireNonNull(getCommand("guibuilder")).setTabCompleter(cmd);

        Bukkit.getPluginManager().registerEvents(new GuiListener(guiManager), this);
        Bukkit.getPluginManager().registerEvents(new ItemsAdderPackReminderListener(this), this);

        getLogger().info("[GUIBuilder] aktiviert.");
    }

    @Override
    public void onDisable() {
        // Nichts nötig: jede GUI speichert in ihrer eigenen Datei on-the-fly.
        getLogger().info("[GUIBuilder] deaktiviert.");
    }

    private boolean checkItemsAdderAvailable() {
        Plugin plugin = Bukkit.getPluginManager().getPlugin("ItemsAdder");
        return plugin != null && plugin.isEnabled();
    }

    private boolean checkGuiBuilderPackPresent() {
        File pluginsDir = getDataFolder().getParentFile();
        if (pluginsDir == null) return false;

        File packDir = new File(pluginsDir, "ItemsAdder/contents/guibuilder");
        if (packDir.isDirectory()) return true;

        File packDirUppercase = new File(pluginsDir, "ItemsAdder/contents/GUIBuilder");
        return packDirUppercase.isDirectory();
    }

    public boolean shouldUseItemsAdderLayout() {
        return itemsAdderAvailable && guiBuilderPackPresent;
    }

    public boolean shouldRemindPackInstallation() {
        return itemsAdderAvailable && !guiBuilderPackPresent;
    }

    public boolean isItemsAdderAvailable() {
        return itemsAdderAvailable;
    }
}
