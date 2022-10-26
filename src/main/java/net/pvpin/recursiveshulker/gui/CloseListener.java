package net.pvpin.recursiveshulker.gui;

import net.pvpin.recursiveshulker.Main;
import net.pvpin.recursiveshulker.util.PlayerInvIO;
import org.bukkit.Bukkit;
import org.bukkit.block.ShulkerBox;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.ItemStack;

/**
 * @author William_Shi
 */
public class CloseListener implements Listener {

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onClose(InventoryCloseEvent event) {
        if (event.getInventory().getHolder() == null) {
            return;
        }
        if (!(event.getInventory().getHolder() instanceof ShulkerInvHolder)) {
            return;
        }
        if (((ShulkerInvHolder) event.getInventory().getHolder()).isBeingVerified.get()) {
            return;
        }
        var playerUUID = event.getPlayer().getUniqueId();
        Bukkit.getScheduler().runTaskLaterAsynchronously(Main.getPlugin(Main.class), () -> {
            var holder = (ShulkerInvHolder) event.getInventory().getHolder();
            var meta = holder.meta;
            var shulker = (ShulkerBox) meta.getBlockState();
            shulker.getInventory().clear();
            for (int index = 0; index < 27; index++) {
                if (event.getInventory().getItem(index) != null) {
                    shulker.getInventory().setItem(index, event.getInventory().getItem(index));
                }
            }
            meta.setBlockState(shulker);
            var stack = new ItemStack(holder.material.parseMaterial());
            stack.setItemMeta(meta);
            Bukkit.getScheduler().runTaskLater(Main.getPlugin(Main.class), () -> {
                if (Bukkit.getOfflinePlayer(playerUUID).isOnline()) {
                    var map = Bukkit.getPlayer(playerUUID).getInventory().addItem(stack);
                    if (!map.isEmpty()) {
                        var loc = Bukkit.getPlayer(playerUUID).getLocation();
                        loc.getWorld().dropItemNaturally(loc, stack);
                    }
                } else {
                    Bukkit.getScheduler().runTaskLaterAsynchronously(Main.getPlugin(Main.class), () -> {
                        if (!PlayerInvIO.addStackToInv(playerUUID, stack)) {
                            var loc = PlayerInvIO.getLocation(playerUUID);
                            Bukkit.getScheduler().runTaskLater(Main.getPlugin(Main.class), () -> {
                                loc.getWorld().dropItemNaturally(loc, stack);
                            }, 1L);
                        }
                    }, 200L);
                }
            }, 2L);
        }, 1L);
    }
}
