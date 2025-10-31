package net.spinx.GUIBuilder;

import org.bukkit.ChatColor;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.stream.Collectors;

public class GuiCommand implements CommandExecutor, TabCompleter {

    private final Main plugin;
    private final GuiManager manager;
    private final Messages messages;

    public GuiCommand(Main plugin, GuiManager manager) {
        this.plugin = plugin;
        this.manager = manager;
        this.messages = plugin.messages();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player p)) { sender.sendMessage(messages.get("command.player_only")); return true; }
        if (!sender.hasPermission("guibuilder.use")) { sender.sendMessage(ChatColor.RED + messages.get("command.no_permission")); return true; }

        if (args.length == 0) { help(sender, label); return true; }

        String sub = args[0].toLowerCase(Locale.ROOT);

        switch (sub) {
            case "new" -> {
                if (args.length < 3) { sender.sendMessage(ChatColor.YELLOW + messages.format("command.usage.new", label)); return true; }
                String name = args[1];
                Integer rows = parseRows(args[2]);
                if (rows == null) { sender.sendMessage(ChatColor.RED + messages.get("command.rows_invalid")); return true; }
                if (manager.exists(name)) { sender.sendMessage(ChatColor.RED + messages.get("command.gui_exists")); return true; }
                manager.createNew(p, name, rows);
            }
            case "open" -> {
                if (args.length < 2) { sender.sendMessage(ChatColor.YELLOW + messages.format("command.usage.open", label)); return true; }
                manager.openView(p, args[1]);
            }
            case "edit" -> {
                if (args.length < 2) { sender.sendMessage(ChatColor.YELLOW + messages.format("command.usage.edit", label)); return true; }
                String name = args[1];
                Integer rows = (args.length >= 3) ? parseRows(args[2]) : null;
                if (rows != null && (rows < 1 || rows > 6)) { sender.sendMessage(ChatColor.RED + messages.get("command.rows_invalid")); return true; }
                manager.openEdit(p, name, Optional.ofNullable(rows));
                String message = (rows != null)
                        ? messages.format("command.edit_opened_resized", name, rows)
                        : messages.format("command.edit_opened", name);
                sender.sendMessage(ChatColor.GREEN + message);
            }
            case "delete" -> {
                if (args.length < 2) { sender.sendMessage(ChatColor.YELLOW + messages.format("command.usage.delete", label)); return true; }
                String name = args[1];
                if (!manager.exists(name)) { sender.sendMessage(ChatColor.RED + messages.get("command.gui_missing")); return true; }
                boolean ok = manager.delete(name);
                sender.sendMessage(ok ? ChatColor.GREEN + messages.format("command.delete_success", name)
                        : ChatColor.RED + messages.format("command.delete_failed", name));
            }
            case "export" -> {
                if (args.length < 2) { sender.sendMessage(ChatColor.YELLOW + messages.format("command.usage.export", label)); return true; }
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
        sender.sendMessage(ChatColor.AQUA + messages.get("command.help.header"));
        sender.sendMessage(ChatColor.GRAY + messages.format("command.help.new", label));
        sender.sendMessage(ChatColor.GRAY + messages.format("command.help.open", label));
        sender.sendMessage(ChatColor.GRAY + messages.format("command.help.edit", label));
        sender.sendMessage(ChatColor.GRAY + messages.format("command.help.delete", label));
        sender.sendMessage(ChatColor.GRAY + messages.format("command.help.export", label));
        sender.sendMessage(ChatColor.DARK_GRAY + messages.get("command.help.footer"));
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
