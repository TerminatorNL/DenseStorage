package cf.terminator.densestorage.util;

import net.minecraft.server.v1_13_R2.BlockPosition;
import org.bukkit.Location;

public class BlockLocationUtils {

    public static BlockPosition getBlockPosition(Location location){
        return new BlockPosition(location.getBlockX(), location.getBlockY(), location.getBlockZ());
    }
}
