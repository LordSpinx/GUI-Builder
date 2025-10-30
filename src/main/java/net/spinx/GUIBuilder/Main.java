package net.spinx.GUIBuilder;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Map;
import java.util.Objects;

public class Main extends JavaPlugin {

    private GuiManager guiManager;

    @Override
    public void onEnable() {
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
}
