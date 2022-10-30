package net.pvpin.recursiveshulker.util;

import com.cryptomorin.xseries.XMaterial;
import io.github.karlatemp.mxlib.nbt.*;
import net.pvpin.recursiveshulker.Main;
import org.bukkit.block.ShulkerBox;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * @author William_Shi
 */
public class RecursionLimit {
    public static int calculateMaxRecursion(ItemStack stack) {
        if (!isShulkerBox(stack)) {
            return 0;
        }
        var result = new ArrayList<Integer>(16);

        Arrays.stream(readInv(stack).getContents())
                .filter(RecursionLimit::isShulkerBox)
                .map(RecursionLimit::calculateMaxRecursion)
                .forEach(result::add);

        if (result.isEmpty()) {
            return 1;
        } else {
            result.sort((int1, int2) -> -1 * int1.compareTo(int2));
            return result.get(0) + 1;
        }
    }

    public static boolean isShulkerBox(ItemStack stack) {
        if (stack == null) {
            return false;
        }
        return Main.SUPPORTED.contains(XMaterial.matchXMaterial(stack));
    }

    private static Inventory readInv(ItemStack stack) {
        return ((ShulkerBox) ((BlockStateMeta) stack.getItemMeta()).getBlockState()).getInventory();
    }

    public static boolean verifyCompound(NBTTagCompound compound) {
        var depth = getDepth(compound);
        if (depth > 256) { // Max: 512
            return false;
        }
        var result = true;
        try {
            var bytes = new ByteArrayOutputStream();
            var outputStream = new DataOutputStream(
                    new BufferedOutputStream(
                            new GZIPOutputStream(bytes)
                    )
            );
            NBTCompressedStreamTools.write(compound, outputStream);
            outputStream.flush();
            outputStream.close();
            NBTCompressedStreamTools.loadCompound(new DataInputStream(
                    new GZIPInputStream(
                            new BufferedInputStream(
                                    new ByteArrayInputStream(bytes.toByteArray())
                            )
                    )
            ), new NBTReadLimiter(1048576)); // Max: 2097152
        } catch (RuntimeException exception) {
            result = false; // Too long
        } catch (IOException exception) {
            exception.printStackTrace();
        }
        return result;
    }

    public static int getDepth(NBTBase base) {
        AtomicInteger depth = new AtomicInteger(0);
        if (base instanceof NBTList) {
            ((NBTList<NBTBase>) base)
                    .forEach(element -> {
                        var tmpDepth = getDepth(element);
                        if (tmpDepth > depth.get()) {
                            depth.set(tmpDepth);
                        }
                    });
        } else if (base instanceof NBTTagCompound) {
            ((NBTTagCompound) base).getKeys().stream()
                    .map(((NBTTagCompound) base)::get)
                    .forEach(element -> {
                        var tmpDepth = getDepth(element);
                        if (tmpDepth > depth.get()) {
                            depth.set(tmpDepth);
                        }
                    });
        }
        return 1 + depth.get();
    }

    public static long getLength(NBTBase base) {
        try {
            var bytes = new ByteArrayOutputStream();
            var outputStream = new DataOutputStream(
                    new BufferedOutputStream(
                            new GZIPOutputStream(bytes)
                    )
            );
            NBTCompressedStreamTools.write(base, outputStream);
            outputStream.flush();
            outputStream.close();
            var limiter = new NBTReadLimiter(Long.MAX_VALUE);
            NBTCompressedStreamTools.loadCompound(new DataInputStream(
                    new GZIPInputStream(
                            new BufferedInputStream(
                                    new ByteArrayInputStream(bytes.toByteArray())
                            )
                    )
            ), limiter);
            var field = NBTReadLimiter.class.getDeclaredField("reade");
            field.setAccessible(true);
            var length = (long) field.get(limiter);
            return length;
        } catch (IOException | NoSuchFieldException | IllegalAccessException ex) {
            ex.printStackTrace();
        }
        return 0L;
    }
}
