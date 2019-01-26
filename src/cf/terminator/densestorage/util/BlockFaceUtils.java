package cf.terminator.densestorage.util;

import org.bukkit.block.BlockFace;

public class BlockFaceUtils {

    public static BlockFace rotateRight(BlockFace in){
        switch (in){
            case NORTH:
                return BlockFace.EAST;
            case EAST:
                return BlockFace.SOUTH;
            case SOUTH:
                return BlockFace.WEST;
            case WEST:
                return BlockFace.NORTH;
            default:
                throw new IllegalArgumentException("Expected NORTH, EAST, SOUTH or WEST. Not: " + in);
        }
    }

    public static BlockFace rotateLeft(BlockFace in){
        switch (in){
            case NORTH:
                return BlockFace.WEST;
            case EAST:
                return BlockFace.NORTH;
            case SOUTH:
                return BlockFace.EAST;
            case WEST:
                return BlockFace.SOUTH;
            default:
                throw new IllegalArgumentException("Expected NORTH, EAST, SOUTH or WEST. Not: " + in);
        }
    }
}
