package cf.terminator.densestorage.util;

import org.bukkit.Material;
import org.bukkit.entity.HumanEntity;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;

public class PlayerUtils {
    public static void giveItemStackToPlayer(HumanEntity player, ItemStack ... itemStacks){
        HashMap<Integer, ItemStack> itemsToDrop = player.getInventory().addItem(itemStacks);
        for(ItemStack stack : itemsToDrop.values()){
            player.getWorld().dropItem(player.getLocation(), stack);
        }
    }

    public static void giveItemStackToPlayerReverse(HumanEntity player, ItemStack ... itemStacks){
        ItemStack[] storageContents = player.getInventory().getStorageContents();
        itemscan:
        for(ItemStack stackToGive : itemStacks) {
            for (int slot = storageContents.length - 1; slot > 0; slot--) {

                int translatedSlot;
                if(slot > 26){
                    /* MAIN TO HOTBAR */
                    translatedSlot = slot - 27;
                } else {
                    translatedSlot = slot + 9;
                }

                if(storageContents[translatedSlot] == null || storageContents[translatedSlot].getType() == Material.AIR){
                    player.getInventory().setItem(translatedSlot, stackToGive);
                    continue itemscan;
                }
                if (storageContents[translatedSlot].isSimilar(stackToGive)){
                    int maxSize = storageContents[translatedSlot].getMaxStackSize();
                    if(storageContents[translatedSlot].getAmount() + stackToGive.getAmount() <= maxSize){
                        storageContents[translatedSlot].setAmount(storageContents[translatedSlot].getAmount() + stackToGive.getAmount());
                        continue itemscan;
                    }
                }
            }
            giveItemStackToPlayer(player, stackToGive);
        }
    }
}
