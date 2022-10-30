package net.pvpin.recursiveshulker;

import com.cryptomorin.xseries.XMaterial;
import net.pvpin.recursiveshulker.gui.CloseListener;
import net.pvpin.recursiveshulker.gui.OpenListener;
import net.pvpin.recursiveshulker.gui.SortCommand;
import net.pvpin.recursiveshulker.gui.TransactionListener;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author William_Shi
 */
public class Main extends JavaPlugin {
    public static final List<XMaterial> SUPPORTED;

    static {
        SUPPORTED = Arrays.stream(XMaterial.values())
                .filter(material -> material.name().contains("SHULKER_BOX"))
                .collect(Collectors.toList());
    }

    @Override
    public void onEnable() {
        Bukkit.getPluginManager().registerEvents(new OpenListener(), this);
        Bukkit.getPluginManager().registerEvents(new CloseListener(), this);
        Bukkit.getPluginManager().registerEvents(new TransactionListener(), this);
        var cmd = new SortCommand();
        Bukkit.getPluginCommand("sortshulker").setExecutor(cmd);
        Bukkit.getPluginCommand("sortshulker").setTabCompleter(cmd);
    }
}
