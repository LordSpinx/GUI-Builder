package net.spinx.GUIBuilder;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.Map;
import java.util.Objects;

public class Main extends JavaPlugin {

    private GuiManager guiManager;

    @Override
    public void onEnable() {
        if (!isItemsAdderAvailable()) {
            getLogger().severe("ItemsAdder ist nicht installiert oder aktiviert. GUIBuilder wird deaktiviert.");
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }

        if (!isGuiBuilderPackPresent()) {
            getLogger().severe("Das GUIBuilder-Pack wurde nicht in plugins/ItemsAdder/contents gefunden. GUIBuilder wird deaktiviert.");
            Bukkit.getPluginManager().disablePlugin(this);
            return;
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

        getLogger().info("[GUIBuilder] aktiviert.");
    }

    @Override
    public void onDisable() {
        // Nichts nötig: jede GUI speichert in ihrer eigenen Datei on-the-fly.
        getLogger().info("[GUIBuilder] deaktiviert.");
    }

    private boolean isItemsAdderAvailable() {
        Plugin plugin = Bukkit.getPluginManager().getPlugin("ItemsAdder");
        return plugin != null && plugin.isEnabled();
    }

    private boolean isGuiBuilderPackPresent() {
        File pluginsDir = getDataFolder().getParentFile();
        if (pluginsDir == null) return false;

        File packDir = new File(pluginsDir, "ItemsAdder/contents/guibuilder");
        if (packDir.isDirectory()) return true;

        File packDirUppercase = new File(pluginsDir, "ItemsAdder/contents/GUIBuilder");
        return packDirUppercase.isDirectory();
    }
}
