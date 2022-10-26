package net.pvpin.recursiveshulker.util;

import io.github.karlatemp.mxlib.nbt.*;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.inventory.ItemStack;
import sun.misc.Unsafe;

import java.io.*;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * @author William_Shi
 */
public class PlayerInvIO {
    private static final String VERSION = Bukkit.getServer().getClass().getPackage().getName()
            .replace(".", ",").split(",")[3];
    private static Class<?> nmsItemStack;
    private static Class<?> nmsNBTCompressedStreamTools;
    private static Class<?> nmsNBTTagCompound;
    private static Class<?> obcCraftItemStack;
    private static Method asNMSCopy;
    private static Method nmsItemStack_saveNBTTagCompound;
    private static Method nmsNBTCompressedStreamTools_read;
    private static Method nmsNBTCompressedStreamTools_write;

    static {
        try {
            Field theUnsafe = Unsafe.class.getDeclaredField("theUnsafe");
            theUnsafe.setAccessible(true);
            Unsafe unsafe = (Unsafe) theUnsafe.get(null);
            Field module = Class.class.getDeclaredField("module");
            long offset = unsafe.objectFieldOffset(module);
            unsafe.putObject(PlayerInvIO.class, offset, Object.class.getModule());

            nmsItemStack = Class.forName("net.minecraft.world.item.ItemStack");
            nmsNBTCompressedStreamTools = Class.forName("net.minecraft.nbt.NBTCompressedStreamTools");
            nmsNBTTagCompound = Class.forName("net.minecraft.nbt.NBTTagCompound");
            obcCraftItemStack = Class.forName("org.bukkit.craftbukkit." + VERSION + ".inventory.CraftItemStack");
            asNMSCopy = obcCraftItemStack.getMethod("asNMSCopy", ItemStack.class);
            nmsItemStack_saveNBTTagCompound = Arrays.stream(nmsItemStack.getMethods())
                    .filter(method -> method.getParameterCount() == 1)
                    .filter(method -> method.getParameterTypes()[0] == nmsNBTTagCompound)
                    .filter(method -> method.getReturnType() == nmsNBTTagCompound)
                    .collect(Collectors.toList()).get(0);
            nmsNBTCompressedStreamTools_read = nmsNBTCompressedStreamTools.getMethod("a", InputStream.class);
            nmsNBTCompressedStreamTools_write = nmsNBTCompressedStreamTools.getMethod("a", nmsNBTTagCompound, OutputStream.class);
        } catch (ClassNotFoundException ignore) {
            try {
                nmsItemStack = Class.forName("net.minecraft.server." + VERSION + ".ItemStack");
                nmsNBTCompressedStreamTools = Class.forName("net.minecraft.server." + VERSION + ".NBTCompressedStreamTools");
                nmsNBTTagCompound = Class.forName("net.minecraft.server." + VERSION + ".NBTTagCompound");
                obcCraftItemStack = Class.forName("org.bukkit.craftbukkit." + VERSION + ".inventory.CraftItemStack");
                asNMSCopy = obcCraftItemStack.getMethod("asNMSCopy", ItemStack.class);
                nmsItemStack_saveNBTTagCompound = nmsItemStack.getMethod("save", nmsNBTTagCompound);
                nmsNBTCompressedStreamTools_read = nmsNBTCompressedStreamTools.getMethod("a", InputStream.class);
                nmsNBTCompressedStreamTools_write = nmsNBTCompressedStreamTools.getMethod("a", nmsNBTTagCompound, OutputStream.class);
            } catch (ClassNotFoundException | NoSuchMethodException exception) {
                exception.printStackTrace();
            }
        } catch (NoSuchMethodException | NoSuchFieldException | IllegalAccessException exception) {
            exception.printStackTrace();
        }
    }

    public static boolean addStackToInv(UUID player, ItemStack stack) {
        var compound = readPlayerCompound(player);
        var inv = (NBTList<NBTTagCompound>) compound.get("Inventory");
        // [
        // {Slot:0b,id:"minecraft:netherite_sword",tag:{Damage:0},Count:1b}
        // ]
        var allPossible = new ArrayList<Byte>(64);
        for (byte index = 0; index < 36; index++) {
            allPossible.add(index);
        }
        inv.stream()
                .map(element -> element.getByte("Slot"))
                .filter(allPossible::contains)
                .forEach(allPossible::remove);
        if (allPossible.isEmpty()) {
            return false;
        } else {
            var stackCompound = readItemStackCompound(stack);
            stackCompound.set("Slot", new NBTTagByte(allPossible.get(0)));
            inv.add(stackCompound);
        }
        return writePlayerCompound(player, compound);
    }

    public static Location getLocation(UUID player) {
        var compound = readPlayerCompound(player);
        var uuidLeast = ((NBTTagLong) compound.get("WorldUUIDLeast")).asLong();
        var xPos = ((NBTNumber) ((NBTTagList) compound.get("Pos")).get(0)).asDouble();
        var yPos = ((NBTNumber) ((NBTTagList) compound.get("Pos")).get(1)).asDouble();
        var zPos = ((NBTNumber) ((NBTTagList) compound.get("Pos")).get(2)).asDouble();
        var world = Bukkit.getWorlds().stream()
                .filter(element -> element.getUID().getLeastSignificantBits() == uuidLeast)
                .collect(Collectors.toList()).get(0);
        return new Location(world, xPos, yPos, zPos);
    }

    private static NBTTagCompound readPlayerCompound(UUID player) {
        var fileName =
                new StringBuilder("playerdata/").append(player).append(".dat").toString();
        AtomicReference<NBTTagCompound> result = new AtomicReference<>();
        var list = new ArrayList<>(Bukkit.getWorlds());
        for (World world : list) {
            var folder = world.getWorldFolder();
            var file = new File(folder, fileName);
            if (file.exists()) {
                try (DataInputStream input = new DataInputStream(
                        new GZIPInputStream(
                                new BufferedInputStream(new FileInputStream(file))
                        )
                )) {
                    result.set(NBTCompressedStreamTools.loadCompound(
                            input, NBTReadLimiter.UN_LIMITED
                    ));
                } catch (IOException ex) {
                    throw new RuntimeException(ex);
                }
            }
        }
        return result.get();
    }

    private static boolean writePlayerCompound(UUID player, NBTTagCompound compound) {
        var fileName =
                new StringBuilder("playerdata/").append(player).append(".dat").toString();
        var list = new ArrayList<>(Bukkit.getWorlds());
        for (World world : list) {
            var folder = world.getWorldFolder();
            var file = new File(folder, fileName);
            if (file.exists()) {
                try {
                    var outputStream = new DataOutputStream(
                            new BufferedOutputStream(
                                    new GZIPOutputStream(
                                            new FileOutputStream(file)
                                    )
                            )
                    );
                    NBTCompressedStreamTools.write(compound, outputStream);
                    outputStream.flush();
                    outputStream.close();
                } catch (Exception ex) {
                    ex.printStackTrace();
                    return false;
                }
            }
        }
        return true;
    }

    public static NBTTagCompound readItemStackCompound(ItemStack stack) {
        NBTTagCompound result = null;
        try {
            var nmsStack = asNMSCopy.invoke(null, stack);
            var emptyTag = nmsNBTTagCompound.newInstance();
            nmsItemStack_saveNBTTagCompound.invoke(nmsStack, emptyTag);
            var outputStream = new ByteArrayOutputStream();
            nmsNBTCompressedStreamTools_write.invoke(null, emptyTag, outputStream);
            try (DataInputStream input = new DataInputStream(
                    new GZIPInputStream(
                            new BufferedInputStream(
                                    new ByteArrayInputStream(outputStream.toByteArray())
                            )
                    )
            )) {
                result = NBTCompressedStreamTools.loadCompound(
                        input, NBTReadLimiter.UN_LIMITED
                );
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        } catch (InvocationTargetException | IllegalAccessException | InstantiationException exception) {
            exception.printStackTrace();
        }
        return result;
    }
}
