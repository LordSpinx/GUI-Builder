package net.spinx.GUIBuilder;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class GuiStorage {

    private final JavaPlugin plugin;
    private final File folder;

    public GuiStorage(JavaPlugin plugin) {
        this.plugin = plugin;
        this.folder = new File(plugin.getDataFolder(), "guis");
        if (!folder.exists()) folder.mkdirs();
    }

    public Map<String, GuiManager.GuiData> loadAll() {
        Map<String, GuiManager.GuiData> out = new HashMap<>();
        File[] files = folder.listFiles((dir, name) -> name.endsWith(".yml"));
        if (files == null) return out;

        for (File f : files) {
            try {
                YamlConfiguration yml = YamlConfiguration.loadConfiguration(f);
                String name = yml.getString("name");
                int rows = Math.max(1, Math.min(6, yml.getInt("rows", 1)));
                List<?> raw = yml.getList("contents", Collections.emptyList());

                if (name == null || name.isBlank()) {
                    // Falls "name" fehlt, aus Dateiname ableiten
                    name = f.getName().substring(0, f.getName().length() - 4);
                }

                ItemStack[] contents = new ItemStack[rows * 9];
                for (int i = 0; i < contents.length && i < raw.size(); i++) {
                    Object o = raw.get(i);
                    if (o instanceof ItemStack it) contents[i] = it;
                }

                GuiManager.GuiData data = new GuiManager.GuiData(name, rows, contents);
                out.put(name.toLowerCase(Locale.ROOT), data);
            } catch (Exception ex) {
                plugin.getLogger().warning("Konnte GUI-Datei nicht laden: " + f.getName() + " -> " + ex.getMessage());
            }
        }
        return out;
    }

    public void save(GuiManager.GuiData data) {
        File f = fileFor(data.name());
        YamlConfiguration yml = new YamlConfiguration();
        yml.set("name", data.name());
        yml.set("rows", data.rows());

        List<ItemStack> list = new ArrayList<>(data.contents().length);
        Collections.addAll(list, data.contents());
        yml.set("contents", list);

        try {
            yml.save(f);
        } catch (IOException e) {
            plugin.getLogger().severe("Speichern fehlgeschlagen für GUI '" + data.name() + "': " + e.getMessage());
        }
    }

    public boolean delete(String name) {
        File f = fileFor(name);
        return !f.exists() || f.delete();
    }

    private File fileFor(String name) {
        String key = sanitize(name);
        return new File(folder, key + ".yml");
    }

    /** Erlaubt nur a–z, A–Z, 0–9, -, _. Alles andere → '_' */
    private String sanitize(String s) {
        String key = s.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9-_]+", "_");
        key = key.replaceAll("^_+|_+$", "");
        if (key.isBlank()) key = "gui";
        if (key.length() > 64) key = key.substring(0, 64);
        return key;
    }
}
