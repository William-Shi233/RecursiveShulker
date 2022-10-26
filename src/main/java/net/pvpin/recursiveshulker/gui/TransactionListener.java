package net.pvpin.recursiveshulker.gui;

import net.pvpin.recursiveshulker.Main;
import net.pvpin.recursiveshulker.util.PlayerInvIO;
import net.pvpin.recursiveshulker.util.RecursionLimit;
import org.bukkit.Bukkit;
import org.bukkit.block.ShulkerBox;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryInteractEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Arrays;

/**
 * @author William_Shi
 */
public class TransactionListener implements Listener {

    public static void verify(InventoryInteractEvent event) {
        var holder = (ShulkerInvHolder) event.getView().getTopInventory().getHolder();
        Bukkit.getScheduler().runTaskLaterAsynchronously(Main.getPlugin(Main.class), () -> {
            var meta = holder.meta;
            var shulker = (ShulkerBox) meta.getBlockState();
            shulker.getInventory().clear();
            for (int index = 0; index < 27; index++) {
                if (event.getView().getTopInventory().getItem(index) != null) {
                    shulker.getInventory().setItem(index, event.getView().getTopInventory().getItem(index));
                }
            }
            meta.setBlockState(shulker);
            var stack = new ItemStack(holder.material.parseMaterial());
            stack.setItemMeta(meta);
            var compound = PlayerInvIO.readItemStackCompound(stack);
            var valid = RecursionLimit.verifyCompound(compound);
            if (!valid) {
                Bukkit.getScheduler().runTaskLater(Main.getPlugin(Main.class), () -> {
                    var stackEmpty = new ItemStack(holder.material.parseMaterial());
                    var shulkerBlockState = (ShulkerBox) meta.getBlockState();
                    Arrays.stream(shulkerBlockState.getInventory().getContents())
                            .filter(element -> element != null)
                            .forEach(element -> event.getWhoClicked().getWorld().dropItemNaturally(
                                    event.getWhoClicked().getLocation(),
                                    element
                            ));
                    shulkerBlockState.getInventory().clear();
                    meta.setBlockState(shulkerBlockState);
                    stackEmpty.setItemMeta(meta);
                    event.getWhoClicked().getWorld().dropItemNaturally(
                            event.getWhoClicked().getLocation(),
                            stackEmpty
                    );
                    event.getWhoClicked().closeInventory();
                    Main.getPlugin(Main.class).getLogger().warning(
                            event.getWhoClicked().getName() +
                                    "'s filling a shulker box with invalid NBT tags, either too long or too deep !"
                    );
                }, 2L);
            } else {
                holder.isBeingVerified.set(false);
            }
        }, 1L);
    }

    @EventHandler
    public void onTransaction(InventoryClickEvent event) {
        if (!(event.getView().getTopInventory().getHolder() instanceof ShulkerInvHolder)) {
            return;
        }
        var holder = (ShulkerInvHolder) event.getView().getTopInventory().getHolder();
        if (holder.isBeingVerified.get()) {
            event.setCancelled(true);
            event.setResult(Event.Result.DENY);
            return;
        }
        holder.isBeingVerified.set(true);
        verify(event);
    }

    @EventHandler
    public void onTransaction(InventoryDragEvent event) {
        if (!(event.getView().getTopInventory().getHolder() instanceof ShulkerInvHolder)) {
            return;
        }
        var holder = (ShulkerInvHolder) event.getView().getTopInventory().getHolder();
        if (holder.isBeingVerified.get()) {
            event.setCancelled(true);
            event.setResult(Event.Result.DENY);
            return;
        }
        holder.isBeingVerified.set(true);
        verify(event);
    }
}
