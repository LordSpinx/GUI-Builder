package net.spinx.GUIBuilder;

import org.bukkit.ChatColor;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.stream.Collectors;

public class GuiCommand implements CommandExecutor, TabCompleter {

    private final Main plugin;
    private final GuiManager manager;

    public GuiCommand(Main plugin, GuiManager manager) {
        this.plugin = plugin;
        this.manager = manager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player p)) { sender.sendMessage("Nur Spieler können das benutzen."); return true; }
        if (!sender.hasPermission("guibuilder.use")) { sender.sendMessage(ChatColor.RED + "Keine Berechtigung."); return true; }

        if (args.length == 0) { help(sender, label); return true; }

        String sub = args[0].toLowerCase(Locale.ROOT);

        switch (sub) {
            case "new" -> {
                if (args.length < 3) { sender.sendMessage(ChatColor.YELLOW + "Nutze: /" + label + " new <name> <1-6>"); return true; }
                String name = args[1];
                Integer rows = parseRows(args[2]);
                if (rows == null) { sender.sendMessage(ChatColor.RED + "Reihen müssen 1–6 sein."); return true; }
                if (manager.exists(name)) { sender.sendMessage(ChatColor.RED + "Diese GUI existiert bereits."); return true; }
                manager.createNew(p, name, rows);
            }
            case "open" -> {
                if (args.length < 2) { sender.sendMessage(ChatColor.YELLOW + "Nutze: /" + label + " open <name>"); return true; }
                manager.openView(p, args[1]);
            }
            case "edit" -> {
                if (args.length < 2) { sender.sendMessage(ChatColor.YELLOW + "Nutze: /" + label + " edit <name> [1-6]"); return true; }
                String name = args[1];
                Integer rows = (args.length >= 3) ? parseRows(args[2]) : null;
                if (rows != null && (rows < 1 || rows > 6)) { sender.sendMessage(ChatColor.RED + "Reihen müssen 1–6 sein."); return true; }
                manager.openEdit(p, name, Optional.ofNullable(rows));
                sender.sendMessage(ChatColor.GREEN + "GUI '" + name + "' im Bearbeiten-Modus geöffnet" + (rows != null ? (" (Größe: " + rows + ")") : "") + ".");
            }
            case "delete" -> {
                if (args.length < 2) { sender.sendMessage(ChatColor.YELLOW + "Nutze: /" + label + " delete <name>"); return true; }
                String name = args[1];
                if (!manager.exists(name)) { sender.sendMessage(ChatColor.RED + "Diese GUI gibt es nicht."); return true; }
                boolean ok = manager.delete(name);
                sender.sendMessage(ok ? ChatColor.GREEN + "GUI '" + name + "' wurde gelöscht."
                        : ChatColor.RED + "GUI '" + name + "' konnte nicht gelöscht werden.");
            }
            case "export" -> {
                if (args.length < 2) { sender.sendMessage(ChatColor.YELLOW + "Nutze: /" + label + " export <name>"); return true; }
                manager.export(p, args[1]);
            }
            default -> help(sender, label);
        }
        return true;
    }

    private Integer parseRows(String s) {
        try {
            int r = Integer.parseInt(s);
            return (r >= 1 && r <= 6) ? r : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private void help(CommandSender sender, String label) {
        sender.sendMessage(ChatColor.AQUA + "GUIBuilder – Befehle:");
        sender.sendMessage(ChatColor.GRAY + "/" + label + " new <name> <1-6> " + ChatColor.DARK_GRAY + "– neue GUI erstellen (Edit)");
        sender.sendMessage(ChatColor.GRAY + "/" + label + " open <name> " + ChatColor.DARK_GRAY + "– GUI ansehen (gesperrt)");
        sender.sendMessage(ChatColor.GRAY + "/" + label + " edit <name> [1-6] " + ChatColor.DARK_GRAY + "– GUI bearbeiten; Größe optional ändern");
        sender.sendMessage(ChatColor.GRAY + "/" + label + " delete <name> " + ChatColor.DARK_GRAY + "– GUI löschen");
        sender.sendMessage(ChatColor.GRAY + "/" + label + " export <name> " + ChatColor.DARK_GRAY + "– Beschreibung als Textdatei speichern");
        sender.sendMessage(ChatColor.DARK_GRAY + "Beim Schließen im Edit-Modus wird automatisch gespeichert.");
    }

    // Tab-Completion
    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        if (!sender.hasPermission("guibuilder.use")) return Collections.emptyList();

        if (args.length == 1) {
            return Arrays.asList("new", "open", "edit", "delete", "export").stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase(Locale.ROOT)))
                    .collect(Collectors.toList());
        }
        if (args.length == 2) {
            if (args[0].equalsIgnoreCase("open")
                    || args[0].equalsIgnoreCase("edit")
                    || args[0].equalsIgnoreCase("delete")
                    || args[0].equalsIgnoreCase("export")) {
                return manager.getAllNames().stream()
                        .filter(n -> n.toLowerCase(Locale.ROOT).startsWith(args[1].toLowerCase(Locale.ROOT)))
                        .sorted().collect(Collectors.toList());
            }
        }
        if (args.length == 3) {
            if (args[0].equalsIgnoreCase("new") || args[0].equalsIgnoreCase("edit")) {
                return Arrays.asList("1","2","3","4","5","6").stream()
                        .filter(s -> s.startsWith(args[2]))
                        .collect(Collectors.toList());
            }
        }
        return Collections.emptyList();
    }
}
