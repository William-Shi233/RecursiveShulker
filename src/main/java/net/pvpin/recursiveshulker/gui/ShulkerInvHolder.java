package net.pvpin.recursiveshulker.gui;

import com.cryptomorin.xseries.XMaterial;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.meta.BlockStateMeta;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author William_Shi
 */
public class ShulkerInvHolder implements InventoryHolder {
    protected Inventory inventory;
    protected BlockStateMeta meta;
    protected XMaterial material;
    protected EquipmentSlot slot;
    protected AtomicBoolean isBeingVerified = new AtomicBoolean(false);

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
