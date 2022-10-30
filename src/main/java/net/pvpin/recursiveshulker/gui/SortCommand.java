package net.pvpin.recursiveshulker.gui;

import com.cryptomorin.xseries.XMaterial;
import net.pvpin.recursiveshulker.Main;
import net.pvpin.recursiveshulker.util.PlayerInvIO;
import net.pvpin.recursiveshulker.util.RecursionLimit;
import org.bukkit.Bukkit;
import org.bukkit.block.ShulkerBox;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @author William_Shi
 */
public class SortCommand implements TabExecutor {
    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length < 1) {
            return false;
        }
        if (!(sender instanceof Player)) {
            return false;
        }
        var option = args[0];
        var pl = (Player) sender;
        boolean isAscending = option.charAt(0) == 'a' || option.charAt(0) == 'A';
        boolean isDescending = option.charAt(0) == 'd' || option.charAt(0) == 'D';
        boolean isMainHand = RecursionLimit.isShulkerBox(pl.getInventory().getItemInMainHand());
        boolean isOffHand = RecursionLimit.isShulkerBox(pl.getInventory().getItemInOffHand());
        if (((!isMainHand) && (!isOffHand)) || ((!isAscending) && (!isDescending))) {
            return false;
        }
        var meta = isMainHand ?
                (BlockStateMeta) pl.getInventory().getItemInMainHand().getItemMeta() :
                (BlockStateMeta) pl.getInventory().getItemInOffHand().getItemMeta();
        var shulker = (ShulkerBox) meta.getBlockState();
        var material = XMaterial.matchXMaterial(
                isMainHand ?
                        pl.getInventory().getItemInMainHand() :
                        pl.getInventory().getItemInOffHand()
        );
        var playerUUID = pl.getUniqueId();
        if (isMainHand) {
            pl.getInventory().setItemInMainHand(null);
        } else {
            pl.getInventory().setItemInOffHand(null);
        }
        Bukkit.getScheduler().runTaskLaterAsynchronously(Main.getPlugin(Main.class), () -> {
            var inv = shulker.getInventory();
            var invCopy = Bukkit.createInventory(null, 27);
            var map = new HashMap<Integer, Long>(32);
            for (int index = 0; index < 27; index++) {
                var content = inv.getItem(index);
                if (content == null) {
                    continue;
                }
                invCopy.setItem(index, content);
                var length = RecursionLimit.getLength(PlayerInvIO.readItemStackCompound(content));
                map.put(index, length);
            }
            System.out.println(map);
            var list = new ArrayList<>(map.entrySet());
            if (isAscending) {
                list.sort(Comparator.comparingLong(Map.Entry::getValue));
            } else {
                list.sort(Collections.reverseOrder(Comparator.comparingLong(Map.Entry::getValue)));
            }
            System.out.println(list);
            inv.clear();
            for (int index = 0; index < list.size(); index++) {
                var content = invCopy.getItem(list.get(index).getKey());
                inv.setItem(index, content);
            }
            meta.setBlockState(shulker);
            var tempStack = new ItemStack(material.parseMaterial());
            tempStack.setItemMeta(meta);
            addToInv(playerUUID, tempStack);
        }, 1L);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String label, String[] args) {
        return List.of("ascending", "descending");
    }

    private static void addToInv(UUID player, ItemStack stack) {
        Bukkit.getScheduler().runTaskLater(Main.getPlugin(Main.class), () -> {
            if (Bukkit.getOfflinePlayer(player).isOnline()) {
                var map = Bukkit.getPlayer(player).getInventory().addItem(stack);
                if (!map.isEmpty()) {
                    var loc = Bukkit.getPlayer(player).getLocation();
                    loc.getWorld().dropItemNaturally(loc, stack);
                }
            } else {
                Bukkit.getScheduler().runTaskLaterAsynchronously(Main.getPlugin(Main.class), () -> {
                    if (!PlayerInvIO.addStackToInv(player, stack)) {
                        var loc = PlayerInvIO.getLocation(player);
                        Bukkit.getScheduler().runTaskLater(Main.getPlugin(Main.class), () -> {
                            loc.getWorld().dropItemNaturally(loc, stack);
                        }, 1L);
                    }
                }, 200L);
            }
        }, 2L);
    }
}
