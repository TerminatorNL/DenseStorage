package cf.terminator.densestorage.block.densechest;

import cf.terminator.densestorage.DenseStorage;
import cf.terminator.densestorage.inventory.DenseChestInventory;
import cf.terminator.densestorage.throwables.InvalidMinecraftVersionException;
import cf.terminator.densestorage.util.BlockFaceUtils;
import net.minecraft.server.v1_13_R2.NBTTagCompound;
import net.minecraft.server.v1_13_R2.NBTTagList;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Directional;
import org.bukkit.craftbukkit.v1_13_R2.block.CraftBlockEntityState;
import org.bukkit.craftbukkit.v1_13_R2.block.CraftDropper;
import org.bukkit.craftbukkit.v1_13_R2.block.CraftSkull;
import org.bukkit.craftbukkit.v1_13_R2.inventory.CraftItemStack;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.HumanEntity;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.metadata.MetadataValue;

import java.util.*;

public class DenseChest {

    public static NBTTagCompound getDataFromHopper(CraftDropper dropper){
        ItemStack head = dropper.getInventory().getItem(0);
        NBTTagCompound tag = CraftItemStack.asNMSCopy(head).getOrCreateTag();
        if(tag.hasKey("DenseStorageData")){
            return tag;
        }
        return null;
    }

    public static boolean storeDataInHopper(CraftDropper dropper, NBTTagCompound denseStorageData, long amountStored){
        ItemStack disk = dropper.getInventory().getItem(0);

        if(disk != null){
            if(disk.getType() == Material.MUSIC_DISC_13 && disk.getEnchantments().containsKey(Enchantment.LOOT_BONUS_BLOCKS)) {
                ItemMeta meta = disk.getItemMeta();
                meta.setLore(MemoryDisk.getLore(amountStored));
                disk.setItemMeta(meta);
                net.minecraft.server.v1_13_R2.ItemStack diskCopy = CraftItemStack.asNMSCopy(disk);
                NBTTagCompound root = diskCopy.getOrCreateTag();
                root.set("DenseStorageData", denseStorageData);
                diskCopy.setTag(root);
                dropper.getInventory().setItem(0, CraftItemStack.asBukkitCopy(diskCopy));
                return true;
            }else{
                dropper.getInventory().setItem(0, new ItemStack(Material.AIR));
                dropper.getLocation().getWorld().dropItem(dropper.getLocation(), disk);
            }
        }
        return false;
    }

    public static DenseChestInventory getInventory(Block coreBlock, HumanEntity player){
        Block upperDropper = coreBlock.getRelative(BlockFace.UP);
        for(MetadataValue value :upperDropper.getMetadata("DenseChest")){
            if(value.getOwningPlugin() == DenseStorage.INSTANCE){
                return (DenseChestInventory) value.value();
            }
        }
        if(upperDropper.getType() != Material.DROPPER){
            return null;
        }
        CraftDropper dropper = (CraftDropper) upperDropper.getState();
        NBTTagCompound tag = getDataFromHopper(dropper);

        if(tag == null){
            if(player == null){
                return null;
            }
            ItemStack heldItem = player.getInventory().getItemInMainHand();
            if(heldItem.getType() == Material.MUSIC_DISC_13 && heldItem.getEnchantments().containsKey(Enchantment.LOOT_BONUS_BLOCKS)){
                player.getInventory().setItemInMainHand(new ItemStack(Material.AIR));
                player.sendMessage("§aDisk inserted.");
                dropper.getInventory().setItem(0, heldItem);
            }else{
                player.sendMessage("§cStorage disk required.");
                player.sendMessage("§7Hold the disk in your main hand, and try again.");
            }
            return null;
        }
        DenseChestInventory inventory = new DenseChestInventory(coreBlock, tag);
        dropper.setMetadata("DenseChest", new FixedMetadataValue(DenseStorage.INSTANCE, inventory));
        return inventory;
    }

    public static boolean isDenseChestHead(Block block){
        if(block.getType() == Material.PLAYER_WALL_HEAD || block.getType() == Material.PLAYER_HEAD){
            CraftBlockEntityState state = (CraftBlockEntityState) block.getState();
            NBTTagCompound tag = state.getSnapshotNBT();
            NBTTagCompound ownerTag = tag.getCompound("Owner");
            if(MemoryCore.OWNER.equals(ownerTag.getString("Id"))){
                NBTTagCompound propertiesTag = ownerTag.getCompound("Properties");
                NBTTagList texturesTag = (NBTTagList) propertiesTag.get("textures");
                if(MemoryCore.TEXTURE.equals(texturesTag.getCompound(0).getString("Value"))){
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Checks if there's a core block related to the structure, does not check validity
     * @param randomBlock any block
     * @return the core block, or null if there is none
     */
    public static Block getCoreBlock(Block randomBlock){
        if(isDenseChestHead(randomBlock)){
            return randomBlock;
        }
        Directional data;
        Block potential;
        switch (randomBlock.getType()){
            case DROPPER:
                data = (Directional) randomBlock.getBlockData();
                potential = randomBlock.getRelative(data.getFacing());
                if(isDenseChestHead(potential)){
                    return potential;
                }
                return null;
            case IRON_TRAPDOOR:
                data = (Directional) randomBlock.getBlockData();
                potential = randomBlock.getRelative(data.getFacing().getOppositeFace());
                if(isDenseChestHead(potential)){
                    return potential;
                }
                return null;
            case REDSTONE_BLOCK:
                for(BlockFace face : new BlockFace[]{BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST, BlockFace.UP, BlockFace.DOWN}){
                    potential = randomBlock.getRelative(face);
                    if(potential.getType() == Material.IRON_TRAPDOOR){
                        data = (Directional) potential.getBlockData();

                        if(potential.getRelative(data.getFacing()).getLocation().equals(randomBlock.getLocation())){
                            return null;
                        }


                        Block ultraPotential = potential.getRelative(data.getFacing().getOppositeFace());



                        if(isDenseChestHead(ultraPotential)){
                            return ultraPotential;
                        }
                    }
                }
                return null;
            default:
                return null;
        }
    }

    /**
     * Handles the event, the event is cancelled by default and has to be re-enabled
     * @param coreBlock core block
     * @param event the event
     */
    public static void absorbItemMoveEvent(Block coreBlock, InventoryMoveItemEvent event){
        if(isValidChest(coreBlock) == false){
            return;
        }
        DenseChestInventory inventory = getInventory(coreBlock, null);
        if(inventory != null && inventory.canAddItemStack(event.getItem())){
            event.setCancelled(false);
            Bukkit.getScheduler().scheduleSyncDelayedTask(DenseStorage.INSTANCE, new DenseChestImportTask(coreBlock, event.getDestination()));
        }

    }

    public static boolean isValidChest(Block coreBlock){
        if(coreBlock.getType() == Material.PLAYER_WALL_HEAD){

            /* Dropper above */
            Block dropperAbove = coreBlock.getRelative(BlockFace.UP);
            if(dropperAbove.getType() == Material.DROPPER){
                Directional data = (Directional) dropperAbove.getBlockData();
                if(data.getFacing() != BlockFace.DOWN){
                    return false;
                }
            }else{
                return false;
            }

            /* Dropper below */
            Block dropperBelow = coreBlock.getRelative(BlockFace.DOWN);
            if(dropperBelow.getType() == Material.DROPPER){
                Directional data = (Directional) dropperBelow.getBlockData();
                if(data.getFacing() != BlockFace.UP){
                    return false;
                }
            }else{
                return false;
            }

            BlockFace coreDirection = ((Directional) coreBlock.getBlockData()).getFacing();
            BlockFace coreDirectionOpposite = coreDirection.getOppositeFace();

            /* Trapdoor behind */
            Block trapDoorBehind = coreBlock.getRelative(coreDirectionOpposite);
            if(trapDoorBehind.getType() == Material.IRON_TRAPDOOR){
                Directional data = (Directional) trapDoorBehind.getBlockData();
                if(data.getFacing() != coreDirectionOpposite){
                    return false;
                }
            }else{
                return false;
            }

            /* Dropper left */
            Block dropperLeft = coreBlock.getRelative(BlockFaceUtils.rotateLeft(coreDirection));
            if(dropperLeft.getType() == Material.DROPPER){
                Directional data = (Directional) dropperLeft.getBlockData();
                if(data.getFacing() != BlockFaceUtils.rotateLeft(coreDirection).getOppositeFace()){
                    return false;
                }
            }else{
                return false;
            }

            /* Dropper right */
            Block dropperRight = coreBlock.getRelative(BlockFaceUtils.rotateRight(coreDirection));
            if(dropperRight.getType() == Material.DROPPER){
                Directional data = (Directional) dropperRight.getBlockData();
                if(data.getFacing() != BlockFaceUtils.rotateRight(coreDirection).getOppositeFace()){
                    return false;
                }
            }else{
                return false;
            }

            Block[] droppers = new Block[]{dropperAbove, dropperBelow, dropperLeft, dropperRight};
            for(Block dropper : droppers){
                if(dropper.getRelative(coreDirectionOpposite).getType() != Material.REDSTONE_BLOCK){
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    public static void breakChest(Block coreBlock){
        if(isValidChest(coreBlock) == false){
            return;
        }

        Block[] droppers = getDroppers(coreBlock);
        Block[] redstoneBlocks = getRedstoneBlocks(coreBlock);
        Block trapDoor = getTrapdoor(coreBlock);

        assert droppers != null;
        assert redstoneBlocks != null;
        assert trapDoor != null;

        coreBlock.setType(Material.AIR);
        coreBlock.getLocation().getWorld().dropItemNaturally(coreBlock.getLocation(), DenseChest.MemoryCore.getSkull());

        for(Block block : droppers){
            CraftDropper dropper = (CraftDropper) block.getState();
            dropper.getInventory().clear();
            block.breakNaturally();
        }
        for(Block block : redstoneBlocks){
            block.breakNaturally();
        }
        trapDoor.breakNaturally();

    }

    public static Block getTrapdoor(Block coreBlock){
        BlockFace coreDirection = ((Directional) coreBlock.getBlockData()).getFacing();
        BlockFace coreDirectionOpposite = coreDirection.getOppositeFace();

        /* Trapdoor behind */
        Block trapDoorBehind = coreBlock.getRelative(coreDirectionOpposite);
        if(trapDoorBehind.getType() == Material.IRON_TRAPDOOR){
            Directional data = (Directional) trapDoorBehind.getBlockData();
            if(data.getFacing() != coreDirectionOpposite){
                return null;
            }
        }else{
            return null;
        }
        return trapDoorBehind;
    }

    public static Block[] getRedstoneBlocks(Block coreBlock){
        Block[] droppers = getDroppers(coreBlock);
        if(droppers == null){
            return null;
        }

        BlockFace coreDirection = ((Directional) coreBlock.getBlockData()).getFacing();
        BlockFace coreDirectionOpposite = coreDirection.getOppositeFace();

        Block[] redstoneBlocks = new Block[droppers.length];

        int i = 0;
        for(Block dropper : droppers){
            Block redstoneBlock = dropper.getRelative(coreDirectionOpposite);
            if(redstoneBlock.getType() != Material.REDSTONE_BLOCK){
                return null;
            }else{
                redstoneBlocks[i] = redstoneBlock;
                i++;
            }
        }
        return redstoneBlocks;
    }

    public static Block[] getDroppers(Block coreBlock){
        if(isDenseChestHead(coreBlock) == false){
            return null;
        }

        /* Dropper above */
        Block dropperAbove = coreBlock.getRelative(BlockFace.UP);
        if(dropperAbove.getType() == Material.DROPPER){
            Directional data = (Directional) dropperAbove.getBlockData();
            if(data.getFacing() != BlockFace.DOWN){
                return null;
            }
        }else{
            return null;
        }

        /* Dropper below */
        Block dropperBelow = coreBlock.getRelative(BlockFace.DOWN);
        if(dropperBelow.getType() == Material.DROPPER){
            Directional data = (Directional) dropperBelow.getBlockData();
            if(data.getFacing() != BlockFace.UP){
                return null;
            }
        }else{
            return null;
        }

        BlockFace coreDirection = ((Directional) coreBlock.getBlockData()).getFacing();
        BlockFace coreDirectionOpposite = coreDirection.getOppositeFace();

        /* Dropper left */
        Block dropperLeft = coreBlock.getRelative(BlockFaceUtils.rotateLeft(coreDirection));
        if(dropperLeft.getType() == Material.DROPPER){
            Directional data = (Directional) dropperLeft.getBlockData();
            if(data.getFacing() != BlockFaceUtils.rotateLeft(coreDirection).getOppositeFace()){
                return null;
            }
        }else{
            return null;
        }

        /* Dropper right */
        Block dropperRight = coreBlock.getRelative(BlockFaceUtils.rotateRight(coreDirection));
        if(dropperRight.getType() == Material.DROPPER){
            Directional data = (Directional) dropperRight.getBlockData();
            if(data.getFacing() != BlockFaceUtils.rotateRight(coreDirection).getOppositeFace()){
                return null;
            }
        }else{
            return null;
        }

        Block[] droppers = new Block[]{dropperAbove, dropperBelow, dropperLeft, dropperRight};
        for(Block dropper : droppers){
            if(dropper.getRelative(coreDirectionOpposite).getType() != Material.REDSTONE_BLOCK){
                return null;
            }
        }
        return droppers;
    }


    public static class MemoryCore{

        private static final String TEXTURE = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZDc4ZjJiN2U1ZTc1NjM5ZWE3ZmI3OTZjMzVkMzY0YzRkZjI4YjQyNDNlNjZiNzYyNzdhYWRjZDYyNjEzMzcifX19";
        private static final String OWNER = "5e609e12-8eb1-4c5b-81c1-db73963f94fd";
        private static final String DISPLAY_NAME = "§r§l§eDense core";
        private static final List<String> LORE = Arrays.asList(
                "§r1: §7Place four droppers facing inwards",
                "§r2: §7At the side of every hopper, place",
                "§r   §7one redstone block on the same side.",
                "§r3: §7Between the redstone blocks, place an iron",
                "§r   §7trapdoor closest to the droppers",
                "§r4: §7Place me on the iron trapdoor",
                "§r5: §7Click me!"
        );

        private static ItemStack skull;

        public static ItemStack getSkull(){
            if(skull != null){
                return skull.clone();
            }
            try {

                NBTTagCompound root = new NBTTagCompound();
                NBTTagCompound skullOwnerTag = new NBTTagCompound();

                NBTTagCompound propertiesTag = new NBTTagCompound();
                NBTTagList texturesTag = new NBTTagList();
                NBTTagCompound textureTag = new NBTTagCompound();
                textureTag.setString("Value",TEXTURE);
                texturesTag.add(textureTag);
                propertiesTag.set("textures", texturesTag);
                skullOwnerTag.set("Properties", propertiesTag);
                skullOwnerTag.setString("Id",OWNER);

                root.set("SkullOwner", skullOwnerTag);
                net.minecraft.server.v1_13_R2.ItemStack skull = CraftItemStack.asNMSCopy(new ItemStack(Material.PLAYER_HEAD));

                skull.setTag(root);

                MemoryCore.skull = CraftItemStack.asBukkitCopy(skull);
                ItemMeta meta = MemoryCore.skull.getItemMeta();
                meta.setDisplayName(DISPLAY_NAME);

                meta.setLore(LORE);

                MemoryCore.skull.setItemMeta(meta);

                return MemoryCore.skull.clone();
            } catch (Throwable e) {
                throw new InvalidMinecraftVersionException(e);
            }
        }


    }

    public static class MemoryDisk{


        private static final String DISPLAY_NAME = "§r§l§eDense Storage Disk";
        private static final List<String> LORE = Arrays.asList(
                "§rStorage for your dense chest.",
                "§rIf you lose this item, there is",
                "§rno way to get your stuff back!",
                "",
                "§r§7Stored items: 0"
        );

        public static List<String> getLore(long itemCount){
            LinkedList<String> list = new LinkedList<>(LORE);
            list.removeLast();
            list.addLast("§r§7Stored items: " + String.valueOf(itemCount));
            return list;
        }

        private static ItemStack disk;

        public static ItemStack getDisk(){
            if(disk != null){
                return disk.clone();
            }
            ItemStack disk = new ItemStack(Material.MUSIC_DISC_13);
            ItemMeta meta = disk.getItemMeta();
            meta.setDisplayName(DISPLAY_NAME);
            meta.setLore(LORE);
            meta.addEnchant(Enchantment.LOOT_BONUS_BLOCKS, 1, true);
            meta.addItemFlags(ItemFlag.values());
            disk.setItemMeta(meta);

            net.minecraft.server.v1_13_R2.ItemStack stack = CraftItemStack.asNMSCopy(disk);
            NBTTagCompound root = stack.getOrCreateTag();
            root.set("DenseStorageData", new NBTTagCompound());
            stack.setTag(root);
            MemoryDisk.disk = CraftItemStack.asBukkitCopy(stack);
            return MemoryDisk.disk.clone();
        }
    }

    public static class DenseChestImportTask implements Runnable{

        private final Block coreBlock;
        private final Inventory hopperInventory;

        public DenseChestImportTask(Block coreBlock, Inventory hopper){
            this.coreBlock = coreBlock;
            this.hopperInventory = hopper;
        }

        @Override
        public void run() {
            HashMap<Integer, ItemStack> slots = new HashMap<>();
            int slot = 0;
            while(slot < hopperInventory.getSize()){
                ItemStack itemstack = hopperInventory.getItem(slot);
                if(itemstack != null && (itemstack.getType() != Material.MUSIC_DISC_13 || itemstack.getEnchantments().containsKey(Enchantment.LOOT_BONUS_BLOCKS) == false)){
                    slots.put(slot, itemstack);
                }
                slot++;
            }
            if(slots.size() == 0){
                return;
            }

            if(isValidChest(coreBlock)){
                DenseChestInventory inventory = getInventory(coreBlock, null);
                if(inventory != null){
                    for(Map.Entry<Integer, ItemStack> e : slots.entrySet()){
                        if(inventory.addItemStack(e.getValue())){
                            hopperInventory.clear(e.getKey());
                        }
                    }
                }
            }
        }
    }
}
