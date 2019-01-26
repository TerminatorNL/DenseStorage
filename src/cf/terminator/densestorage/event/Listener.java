package cf.terminator.densestorage.event;

import cf.terminator.densestorage.DenseStorage;
import cf.terminator.densestorage.block.densechest.DenseChest;
import cf.terminator.densestorage.inventory.BaseInventory;
import cf.terminator.densestorage.inventory.DenseChestInventory;
import net.minecraft.server.v1_13_R2.EntityPlayer;
import net.minecraft.server.v1_13_R2.IInventory;
import net.minecraft.server.v1_13_R2.TileEntityDropper;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.Directional;
import org.bukkit.craftbukkit.v1_13_R2.inventory.CraftInventory;
import org.bukkit.craftbukkit.v1_13_R2.inventory.InventoryWrapper;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.HumanEntity;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDispenseEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.inventory.*;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;

import java.lang.reflect.Field;

public class Listener implements org.bukkit.event.Listener {

    public static final Listener INSTANCE = new Listener();

    @EventHandler
    public void onInteract(PlayerInteractEvent event){
        if(event.getAction() == Action.RIGHT_CLICK_BLOCK && event.getHand() == EquipmentSlot.HAND && event.getPlayer().isSneaking() == false) {
            Block clickedBlock = event.getClickedBlock();
            Block coreBlock;
            if(clickedBlock.getType() == Material.DROPPER){
                Directional data = (Directional) clickedBlock.getBlockData();
                coreBlock = clickedBlock.getRelative(data.getFacing());
            }else{
                coreBlock = clickedBlock;
            }
            if(DenseChest.isDenseChestHead(coreBlock)){
                event.setCancelled(true);
                if (DenseChest.isValidChest(coreBlock)) {
                    Inventory inventory = DenseChest.getInventory(coreBlock, event.getPlayer());
                    if(inventory != null) {
                        event.getPlayer().openInventory(inventory);
                    }
                } else {
                    event.getPlayer().sendMessage(ChatColor.RED + "Structure is invalid.");
                }
            }
        }
        if(event.getAction() == Action.LEFT_CLICK_BLOCK && event.getHand() == EquipmentSlot.HAND && event.getPlayer().isSneaking() == false) {
            Block clickedBlock = event.getClickedBlock();
            Block coreBlock;
            if (clickedBlock.getType() == Material.DROPPER) {
                Directional data = (Directional) clickedBlock.getBlockData();
                coreBlock = clickedBlock.getRelative(data.getFacing());
            } else {
                coreBlock = clickedBlock;
            }
            if(DenseChest.isDenseChestHead(coreBlock)){
                event.setCancelled(true);
                if (DenseChest.isValidChest(coreBlock)) {
                    DenseChestInventory inventory = DenseChest.getInventory(coreBlock, event.getPlayer());
                    if(inventory != null){
                        inventory.quickPlayerDump(event.getPlayer());
                    }
                } else {
                    event.getPlayer().sendMessage(ChatColor.RED + "Structure is invalid.");
                }
            }
        }
    }


    public boolean denseChestBreakCheck(Block block){
        Block coreBlock = DenseChest.getCoreBlock(block);
        if(coreBlock == null){
            return false;
        }
        DenseChestInventory inventory = DenseChest.getInventory(coreBlock, null);
        if(inventory != null && inventory.getStoredItemCount() > 0){
            return true;
        }else{
            DenseChest.breakChest(coreBlock);
            return false;
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent e){
        if(denseChestBreakCheck(e.getBlock())){
            e.setCancelled(true);
            e.getPlayer().sendMessage("§cBefore breaking the dense chest, please remove all stored items, rebuild the structure if you have to.");
            e.getPlayer().sendMessage("§aTip: §7Sneak and rightclick the top hopper with an empty hand to take out the disk!");
        }
    }

    @EventHandler
    public void onEntityExplode(EntityExplodeEvent e){
        for(Block block : e.blockList()){
            if(denseChestBreakCheck(block)){
                e.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler
    public void onBlockExplode(BlockExplodeEvent e){
        for(Block block : e.blockList()){
            if(denseChestBreakCheck(block)){
                e.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler
    public void onDispense(BlockDispenseEvent event) {
        Block dispenser = event.getBlock();
        if (dispenser.getType() == Material.DROPPER) {
            Directional data = (Directional) dispenser.getBlockData();
            Block coreBlock = dispenser.getRelative(data.getFacing());
            if (DenseChest.isDenseChestHead(coreBlock)) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onDispense(InventoryMoveItemEvent event) {
        Block dispenser = event.getDestination().getLocation().getBlock();
        if (dispenser.getType() == Material.DROPPER) {
            Directional data = (Directional) dispenser.getBlockData();
            Block coreBlock = dispenser.getRelative(data.getFacing());
            if (DenseChest.isDenseChestHead(coreBlock)) {
                event.setCancelled(true);
                DenseChest.absorbItemMoveEvent(coreBlock, event);
            }
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent e){
        Inventory inventory = e.getInventory();
        if(inventory instanceof BaseInventory){
            ((BaseInventory) inventory).notifyPlayerInventoryClose(e.getPlayer());
        }
    }

    @EventHandler
    public void onInventoryOpen(InventoryOpenEvent e){

        BaseInventory inventory = getBaseInventory(e.getInventory());
        if(inventory != null){
           inventory.notifyPlayerInventoryOpen(e.getPlayer());
        }
    }

    private static BaseInventory getBaseInventory(Inventory topInventory){
        if(topInventory instanceof CraftInventory){
            IInventory secondInventory = ((CraftInventory) topInventory).getInventory();
            if(secondInventory instanceof InventoryWrapper){
                /*
                Why the fuck do I need to do this? Cmon' bukkit...
                 */
                Inventory thirdInventory = null;
                try {
                    Field trueInventory = InventoryWrapper.class.getDeclaredField("inventory");
                    trueInventory.setAccessible(true);
                    thirdInventory = (Inventory) trueInventory.get(secondInventory);
                } catch (Throwable e1) {
                    e1.printStackTrace();
                }
                if(thirdInventory instanceof BaseInventory){
                    return ((BaseInventory) thirdInventory);
                }
            }
        }
        return null;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent e){
        Inventory topInventory = e.getInventory();
        BaseInventory inventory = getBaseInventory(topInventory);
        if(inventory != null){
            inventory.onInventoryClick(e);
        }
        Location location = topInventory.getLocation();
        if(location != null){
            Block coreBlock = DenseChest.getCoreBlock(location.getBlock());
            if(coreBlock != null) {
                if(e.getRawSlot() < e.getView().getTopInventory().getSize()) {
                    DenseChestInventory denseChestInventory = DenseChest.getInventory(coreBlock, e.getWhoClicked());
                    if (denseChestInventory != null && e.getCurrentItem().getType() == Material.MUSIC_DISC_13 && e.getCurrentItem().getEnchantments().containsKey(Enchantment.LOOT_BONUS_BLOCKS)) {
                        denseChestInventory.unLinkInventory(e.getWhoClicked());
                    }
                }
            }
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent e){
        BaseInventory inventory = getBaseInventory(e.getInventory());
        if(inventory != null){
            inventory.onInventoryDrag(e);
        }
    }
}
