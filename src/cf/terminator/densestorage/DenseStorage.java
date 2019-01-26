package cf.terminator.densestorage;

import cf.terminator.densestorage.block.densechest.DenseChest;
import cf.terminator.densestorage.event.Listener;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Logger;

public class DenseStorage extends JavaPlugin {

    public static Plugin INSTANCE;
    public static Logger LOGGER;


    @Override
    public void onEnable(){
        INSTANCE = this;
        LOGGER = getLogger();
        Bukkit.getPluginManager().registerEvents(Listener.INSTANCE, this);

        ShapedRecipe denseCoreRecipe = new ShapedRecipe(new NamespacedKey(this, "DenseCore"), DenseChest.MemoryCore.getSkull())
                .shape(
                        "PPP",
                        "PHP",
                        "PPP"
                )
                .setIngredient('P', Material.ENDER_PEARL)
                .setIngredient('H', Material.HOPPER);


        ShapedRecipe diskRecipe = new ShapedRecipe(new NamespacedKey(this, "DenseDisk"), DenseChest.MemoryDisk.getDisk())
                .shape(
                        "NNN",
                        "NPN",
                        "NNN"
                )
                .setIngredient('N', Material.IRON_NUGGET)
                .setIngredient('P', Material.PAPER);




        Bukkit.getServer().addRecipe(diskRecipe);
        Bukkit.getServer().addRecipe(denseCoreRecipe);
    }


    @Override
    public void onDisable(){

    }

}
