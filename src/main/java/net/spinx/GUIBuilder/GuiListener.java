package net.spinx.GUIBuilder;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.*;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;

import static net.spinx.GUIBuilder.GuiManager.Mode;

public class GuiListener implements Listener {

    private final GuiManager manager;

    public GuiListener(GuiManager manager) {
        this.manager = manager;
    }

    private boolean isOurHolder(Inventory inv) {
        return inv != null && inv.getHolder() instanceof GuiManager.NamedHolder;
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        InventoryView view = e.getView();
        Inventory top = view.getTopInventory();
        if (!isOurHolder(top)) return;

        GuiManager.NamedHolder holder = (GuiManager.NamedHolder) top.getHolder();
        Player p = (Player) e.getWhoClicked();

        if (holder.mode() == Mode.VIEW) {
            if (e.getClickedInventory() == top) e.setCancelled(true);
            return;
        }
        e.setCancelled(false); // EDIT erlaubt
    }

    @EventHandler
    public void onDrag(InventoryDragEvent e) {
        Inventory top = e.getView().getTopInventory();
        if (!isOurHolder(top)) return;
        GuiManager.NamedHolder holder = (GuiManager.NamedHolder) top.getHolder();

        if (holder.mode() == Mode.VIEW) {
            for (int rawSlot : e.getRawSlots()) {
                if (rawSlot < top.getSize()) { e.setCancelled(true); return; }
            }
        }
    }

    @EventHandler
    public void onMoveItem(InventoryMoveItemEvent e) {
        Inventory inv = e.getDestination();
        if (isOurHolder(inv)) {
            GuiManager.NamedHolder holder = (GuiManager.NamedHolder) inv.getHolder();
            if (holder.mode() == Mode.VIEW) e.setCancelled(true);
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent e) {
        Inventory top = e.getView().getTopInventory();
        if (!isOurHolder(top)) return;
        GuiManager.NamedHolder holder = (GuiManager.NamedHolder) top.getHolder();
        Player p = (Player) e.getPlayer();

        if (holder.mode() == Mode.EDIT) {
            ItemStack[] contents = top.getContents();
            manager.handleClose(p, holder, contents);
        } else {
            p.sendMessage(ChatColor.GRAY + "GUI '" + holder.name() + "' geschlossen.");
        }
    }
}
