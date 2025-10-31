package net.spinx.GUIBuilder;

import dev.lone.itemsadder.api.CustomStack;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;
import java.util.logging.Level;

public class GuiStorage {

    private final Main plugin;
    private final File folder;

    public GuiStorage(Main plugin) {
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

    public Optional<File> exportHumanReadable(GuiManager.GuiData data) {
        File exportFolder = new File(plugin.getDataFolder(), "exports");
        if (!exportFolder.exists() && !exportFolder.mkdirs()) {
            plugin.getLogger().severe("Konnte Export-Ordner nicht erstellen: " + exportFolder.getAbsolutePath());
            return Optional.empty();
        }

        File outFile = new File(exportFolder, sanitize(data.name()) + ".txt");
        try (PrintWriter writer = new PrintWriter(outFile, StandardCharsets.UTF_8)) {
            ItemStack[] contents = data.contents();
            int nonEmpty = 0;
            for (ItemStack stack : contents) {
                if (stack != null && !stack.getType().isAir()) nonEmpty++;
            }

            writer.println("GUI: " + data.name());
            writer.println("Reihen: " + data.rows());
            writer.println("Slots insgesamt: " + contents.length);
            writer.println("Belegte Slots: " + nonEmpty);
            writer.println();

            if (nonEmpty == 0) {
                writer.println("Alle Slots sind leer.");
            } else {
                writer.println("Slot-Details:");
                for (int i = 0; i < contents.length; i++) {
                    ItemStack stack = contents[i];
                    if (stack == null || stack.getType().isAir()) continue;
                    writeItem(writer, stack, i);
                }
            }
        } catch (IOException ex) {
            plugin.getLogger().severe("Export fehlgeschlagen für GUI '" + data.name() + "': " + ex.getMessage());
            return Optional.empty();
        }

        return Optional.of(outFile);
    }

    private void writeItem(PrintWriter writer, ItemStack stack, int slot) {
        int row = slot / 9 + 1;
        int column = slot % 9 + 1;
        writer.println(String.format(Locale.ROOT, "- Slot %02d (Reihe %d, Spalte %d)", slot, row, column));
        writer.println("    Material: " + stack.getType());
        if (stack.getAmount() != 1) {
            writer.println("    Anzahl: " + stack.getAmount());
        }

        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            if (meta.hasDisplayName()) {
                writer.println("    Titel: " + strip(meta.getDisplayName()));
            }
            if (meta.hasLore()) {
                writer.println("    Beschreibung:");
                for (String line : Objects.requireNonNull(meta.getLore())) {
                    writer.println("      • " + strip(line));
                }
            }
            if (meta.hasEnchants()) {
                writer.println("    Verzauberungen:");
                for (Map.Entry<Enchantment, Integer> entry : meta.getEnchants().entrySet()) {
                    writer.println(String.format(Locale.ROOT, "      • %s Stufe %d", entry.getKey().getKey(), entry.getValue()));
                }
            }
            if (!meta.getItemFlags().isEmpty()) {
                writer.println("    Versteckte Flags: " + meta.getItemFlags().stream()
                        .map(ItemFlag::name)
                        .sorted()
                        .collect(Collectors.joining(", ")));
            }
            if (meta.hasCustomModelData()) {
                writer.println("    CustomModelData: " + meta.getCustomModelData());
            }
            if (meta instanceof Damageable damageable && damageable.hasDamage()) {
                writer.println("    Haltbarkeit: " + damageable.getDamage());
            }
            if (meta instanceof PotionMeta potionMeta) {
                writer.println("    Trankdaten: " + potionMeta.getBasePotionData());
            }
        }

        resolveItemsAdderInfo(stack).ifPresent(info -> {
            writer.println("    ItemsAdder: " + info.namespacedId());
            if (info.addon() != null && !info.addon().isBlank()) {
                writer.println("    ItemsAdder-Pack: " + info.addon());
            }
        });

        writer.println();
    }

    private String strip(String input) {
        return ChatColor.stripColor(input == null ? "" : input);
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

    private Optional<ItemsAdderInfo> resolveItemsAdderInfo(ItemStack stack) {
        if (!plugin.isItemsAdderAvailable()) {
            return Optional.empty();
        }

        try {
            CustomStack customStack = CustomStack.byItemStack(stack);
            if (customStack == null) {
                return Optional.empty();
            }

            String namespacedId = customStack.getNamespacedID();
            String addon = determineAddon(customStack);
            return Optional.of(new ItemsAdderInfo(namespacedId, addon));
        } catch (Throwable throwable) {
            plugin.getLogger().log(Level.FINE, "Konnte ItemsAdder-Informationen nicht ermitteln", throwable);
            return Optional.empty();
        }
    }

    private String determineAddon(CustomStack customStack) {
        String configPath = customStack.getConfigPath();
        if (configPath != null) {
            String normalized = configPath.replace('\\', '/');
            normalized = normalized.startsWith("/") ? normalized.substring(1) : normalized;

            int contentsIndex = normalized.indexOf("contents/");
            String remainder = contentsIndex >= 0
                    ? normalized.substring(contentsIndex + "contents/".length())
                    : normalized;

            remainder = remainder.strip();
            if (!remainder.isEmpty()) {
                if (remainder.startsWith("/")) {
                    remainder = remainder.substring(1);
                }
                int slashIndex = remainder.indexOf('/');
                if (slashIndex > 0) {
                    return remainder.substring(0, slashIndex);
                }
                if (slashIndex < 0 && !remainder.isBlank()) {
                    return remainder;
                }
            }
        }

        return customStack.getNamespace();
    }

    private record ItemsAdderInfo(String namespacedId, String addon) {
    }
}
