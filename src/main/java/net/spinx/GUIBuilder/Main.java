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
    private Messages messages;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        this.messages = Messages.load(getConfig().getString("language", "en"), getLogger());
        this.itemsAdderAvailable = checkItemsAdderAvailable();
        this.guiBuilderPackPresent = itemsAdderAvailable && checkGuiBuilderPackPresent();

        if (!itemsAdderAvailable) {
            getLogger().info(messages.get("log.itemsadder.missing"));
        } else if (!guiBuilderPackPresent) {
            getLogger().warning(messages.get("log.itemsadder.no_pack"));
        } else {
            getLogger().info(messages.get("log.itemsadder.ready"));
        }

        // Ordner anlegen
        if (!getDataFolder().exists()) getDataFolder().mkdirs();

        // Storage + Manager
        GuiStorage storage = new GuiStorage(this);
        this.guiManager = new GuiManager(this, storage);

        // Vorhandene GUIs laden
        Map<String, GuiManager.GuiData> loaded = storage.loadAll();
        loaded.values().forEach(d -> guiManager.put(d.name(), d.rows(), d.contents()));
        getLogger().info(messages.format("log.loaded_guis", guiManager.getAllNames()));

        // Commands + Listener
        GuiCommand cmd = new GuiCommand(this, guiManager);
        Objects.requireNonNull(getCommand("guibuilder")).setExecutor(cmd);
        Objects.requireNonNull(getCommand("guibuilder")).setTabCompleter(cmd);

        Bukkit.getPluginManager().registerEvents(new GuiListener(guiManager, messages), this);
        Bukkit.getPluginManager().registerEvents(new ItemsAdderPackReminderListener(this), this);

        getLogger().info(messages.get("log.enabled"));
    }

    @Override
    public void onDisable() {
        // Nichts nötig: jede GUI speichert in ihrer eigenen Datei on-the-fly.
        getLogger().info(messages.get("log.disabled"));
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

    public Messages messages() {
        return messages;
    }
}
