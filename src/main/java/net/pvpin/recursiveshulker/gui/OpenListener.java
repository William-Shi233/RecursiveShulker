package net.pvpin.recursiveshulker.gui;

import com.cryptomorin.xseries.XMaterial;
import net.pvpin.recursiveshulker.Main;
import net.pvpin.recursiveshulker.util.PlayerInvIO;
import org.bukkit.Bukkit;
import org.bukkit.block.ShulkerBox;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.meta.BlockStateMeta;

/**
 * @author William_Shi
 */
public class OpenListener implements Listener {
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onRightClickAir(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR) {
            return;
        }
        if ((!event.hasItem()) || event.getItem() == null) {
            return;
        }
        var xMaterial = XMaterial.matchXMaterial(event.getItem());
        if (!Main.SUPPORTED.contains(xMaterial)) {
            return;
        }
        if (event.getHand() == EquipmentSlot.HAND) {
            event.getPlayer().getInventory().setItemInMainHand(null);
        } else {
            event.getPlayer().getInventory().setItemInOffHand(null);
        }
        var playerUUID = event.getPlayer().getUniqueId();
        Bukkit.getScheduler().runTaskLaterAsynchronously(Main.getPlugin(Main.class), () -> {
            var meta = (BlockStateMeta) event.getItem().getItemMeta();
            var shulker = (ShulkerBox) meta.getBlockState();
            var originInv = shulker.getInventory();
            var newHolder = new ShulkerInvHolder();
            var newInv = Bukkit.createInventory(newHolder, 27, "贆櫝");
            newHolder.inventory = newInv;
            newHolder.meta = meta;
            newHolder.material = xMaterial;
            newHolder.slot = event.getHand();
            for (int index = 0; index < 27; index++) {
                if (originInv.getItem(index) != null) {
                    newInv.setItem(index, originInv.getItem(index));
                }
            }
            if (Bukkit.getOfflinePlayer(playerUUID).isOnline()) {
                Bukkit.getScheduler().runTaskLater(Main.getPlugin(Main.class), () -> event.getPlayer().openInventory(newInv), 2L);
            } else {
                if (!PlayerInvIO.addStackToInv(playerUUID, event.getItem())) {
                    var loc = PlayerInvIO.getLocation(playerUUID);
                    loc.getWorld().dropItemNaturally(loc, event.getItem());
                }
            }
        }, 1L);
    }
}
