package cf.terminator.densestorage.inventory;

import cf.terminator.densestorage.DenseStorage;
import cf.terminator.densestorage.block.densechest.DenseChest;
import cf.terminator.densestorage.util.PlayerUtils;
import net.minecraft.server.v1_13_R2.NBTBase;
import net.minecraft.server.v1_13_R2.NBTTagCompound;
import net.minecraft.server.v1_13_R2.NBTTagList;
import net.minecraft.server.v1_13_R2.ParticleType;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.craftbukkit.v1_13_R2.block.CraftDropper;
import org.bukkit.craftbukkit.v1_13_R2.inventory.CraftItemStack;
import org.bukkit.entity.HumanEntity;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.reflect.Constructor;
import java.util.*;

public class DenseChestInventory extends BaseInventory {

    private static final int MAX_STACK_COUNT = 45;
    private static final int MAX_STACK_SIZE = Integer.MAX_VALUE;
    private static final Constructor<ItemMeta> CRAFT_META_ITEM_CONSTRUCTOR;

    static {
        try {
            //noinspection unchecked
            Class<ItemMeta> itemMetaClass = (Class<ItemMeta>) Class.forName("org.bukkit.craftbukkit.v1_13_R2.inventory.CraftMetaItem");
            CRAFT_META_ITEM_CONSTRUCTOR = itemMetaClass.getDeclaredConstructor(NBTTagCompound.class);
            CRAFT_META_ITEM_CONSTRUCTOR.setAccessible(true);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private final Block coreBlock;
    private TreeMap<ItemDescriptor, Integer> BACKING_DATA = new TreeMap<>();
    private ItemStack[] VISUAL_CACHE = null;
    private UUID lastHumanToTouch = null;

    public DenseChestInventory(Block coreBlock, ItemStack disk) {
        this.coreBlock = coreBlock;
        load(CraftItemStack.asNMSCopy(disk).getOrCreateTag().getCompound("DenseStorageData"));
        save();
    }

    public DenseChestInventory(Block coreBlock, NBTTagCompound tag) {
        this.coreBlock = coreBlock;
        load(tag.getCompound("DenseStorageData"));
    }

    private boolean canAddNewItem() {
        return BACKING_DATA.size() < MAX_STACK_COUNT;
    }

    private int getItemCount(ItemStack itemStack) {
        for (Map.Entry<ItemDescriptor, Integer> existing : BACKING_DATA.entrySet()) {
            if (existing.getKey().isSimilar(itemStack)) {
                return existing.getValue();
            }
        }
        return 0;
    }

    private boolean canItemStack(@Nullable ItemStack itemStack) {
        for (ItemDescriptor existing : BACKING_DATA.keySet()) {
            if (existing.isSimilar(itemStack)) {
                return true;
            }
        }
        return false;
    }

    public boolean addItemStack(@Nonnull ItemStack itemStack) {
        VISUAL_CACHE = null;
        if (itemStack.getType() == Material.AIR) {
            return true;
        }
        ItemDescriptor descriptor = new ItemDescriptor(itemStack);
        int storedCount = getItemCount(itemStack);
        if (canItemStack(itemStack) && storedCount + itemStack.getAmount() < MAX_STACK_SIZE && storedCount + itemStack.getAmount() > 0) {
            BACKING_DATA.put(descriptor, storedCount + itemStack.getAmount());
            save();
            return true;
        } else if (canAddNewItem() && storedCount == 0) {
            BACKING_DATA.put(descriptor, itemStack.getAmount());
            save();
            return true;
        }
        return false;
    }

    public boolean canAddItemStack(@Nonnull ItemStack itemStack){
        if (itemStack.getType() == Material.AIR) {
            return true;
        }
        int storedCount = getItemCount(itemStack);
        if (canItemStack(itemStack) && storedCount + itemStack.getAmount() < MAX_STACK_SIZE) {
            return true;
        } else {
            return canAddNewItem();
        }
    }

    private ItemStack takeItem(ItemDescriptor descriptor, int amount) {
        VISUAL_CACHE = null;
        Integer storedAmount = BACKING_DATA.get(descriptor);
        if (storedAmount == null) {
            return AIR;
        }
        if (storedAmount <= amount) {
            ItemStack stack = descriptor.getItemStack(BACKING_DATA.remove(descriptor));
            save();
            return stack;
        } else {
            BACKING_DATA.put(descriptor, storedAmount - amount);
            save();
            return descriptor.getItemStack(amount);
        }
    }

    private void load(NBTTagCompound tagCompound) {
        VISUAL_CACHE = null;
        NBTTagList list = (NBTTagList) tagCompound.get("Storage");
        if (list == null) {
            list = new NBTTagList();
        }
        for (NBTBase stackTag_ : list) {
            NBTTagCompound stackTag = (NBTTagCompound) stackTag_;
            net.minecraft.server.v1_13_R2.ItemStack itemStack = net.minecraft.server.v1_13_R2.ItemStack.a(stackTag);
            int amount = stackTag.getInt("Stored");
            ItemStack stack = CraftItemStack.asBukkitCopy(itemStack);
            BACKING_DATA.put(new ItemDescriptor(stack), amount);
        }
    }

    public NBTTagCompound getTag() {
        NBTTagCompound tag = new NBTTagCompound();
        NBTTagList list = new NBTTagList();

        for (Map.Entry<ItemDescriptor, Integer> e : BACKING_DATA.entrySet()) {
            NBTTagCompound stackTag = new NBTTagCompound();
            ItemStack rawStack = e.getKey().getItemStack(1);
            net.minecraft.server.v1_13_R2.ItemStack stack = CraftItemStack.asNMSCopy(rawStack);
            stackTag.setInt("Stored", e.getValue());
            list.add(stack.save(stackTag));
        }

        tag.set("Storage", list);
        return tag;
    }

    public void save() {
        CraftDropper dropper = (CraftDropper) coreBlock.getRelative(BlockFace.UP).getState();
        if (DenseChest.storeDataInHopper(dropper, getTag(), getStoredItemCount()) == false) {
            Iterator<Map.Entry<ItemDescriptor, Integer>> entrySet = BACKING_DATA.entrySet().iterator();
            while (entrySet.hasNext()) {
                Map.Entry<ItemDescriptor, Integer> entry = entrySet.next();
                int maxStackSize = entry.getKey().getMaxStackSize();
                int itemsToSpawn = entry.getValue();
                while (itemsToSpawn > 0) {
                    int spawning = Math.min(itemsToSpawn, maxStackSize);
                    coreBlock.getLocation().getWorld().dropItem(coreBlock.getLocation(), entry.getKey().getItemStack(spawning));
                    itemsToSpawn = itemsToSpawn - spawning;
                }
                entrySet.remove();
            }
        }
    }

    public void unLinkInventory(HumanEntity excludeCloseInventoryPlayer) {
        save();
        Block upperDropper = coreBlock.getRelative(BlockFace.UP);
        upperDropper.removeMetadata("DenseChest", DenseStorage.INSTANCE);
        for (HumanEntity player : getViewers()) {
            if (player.equals(excludeCloseInventoryPlayer) == false) {
                player.closeInventory();
            }
        }
        VISUAL_CACHE = null;
        BACKING_DATA.clear();
    }

    public long getStoredItemCount() {
        long total = 0L;
        for (Integer count : BACKING_DATA.values()) {
            total = total + count;
        }
        return total;
    }

    public void quickPlayerDump(HumanEntity player) {
        ListIterator<ItemStack> iterator = player.getInventory().iterator();
        int stacksMoved = 0;
        while (iterator.hasNext()) {
            ItemStack item = iterator.next();
            if (canItemStack(item) && addItemStack(item)) {
                iterator.set(AIR);
                stacksMoved++;
            }
        }
        if (stacksMoved == 0) {
            player.sendMessage("§c0§7 stacks moved.");
            if(player.getUniqueId().equals(lastHumanToTouch)){
                player.sendMessage("§aTip:§7 If you want to destroy this dense chest, sneak!");
            }
            lastHumanToTouch = player.getUniqueId();
        } else {
            if (stacksMoved == 1) {
                player.sendMessage("§a1§7 stack moved.");
            } else {
                player.sendMessage("§a" + stacksMoved + "§7 stacks moved");
            }
            Location coreBlockCenter = coreBlock.getLocation().add(0.5, 0.5, 0.5);


            coreBlock.getWorld().spawnParticle(Particle.PORTAL, coreBlockCenter, 100 * stacksMoved);
            coreBlock.getWorld().spawnParticle(Particle.DRAGON_BREATH, coreBlockCenter, 10 * stacksMoved);
            coreBlock.getWorld().playSound(coreBlockCenter, Sound.ENTITY_ENDERMAN_TELEPORT, 0.25f, 0.5f);
            lastHumanToTouch = null;
        }
    }

    @Override
    public void onInventoryOpen(HumanEntity player) {

    }

    @Override
    public void onInventoryClose(HumanEntity player) {

    }

    @Override
    public void onInventoryClick(InventoryClickEvent event) {
        event.setCancelled(true);
        switch (event.getAction()) {
            case NOTHING:
                return;
            case MOVE_TO_OTHER_INVENTORY:
                if (isThisInventoryClicked(event)) {
                    /* FROM THIS TO PLAYER */
                    int slot = event.getSlot();
                    ItemDescriptor clickedStack = getDescriptor(slot);
                    if (clickedStack == null) {
                        return;
                    }
                    ItemStack receivedStack = takeItem(clickedStack, clickedStack.getMaxStackSize());
                    PlayerUtils.giveItemStackToPlayerReverse(event.getWhoClicked(), receivedStack);
                } else {
                    /* FROM PLAYER TO THIS */
                    ItemStack stack = event.getCurrentItem();
                    if (addItemStack(stack) == true) {
                        event.setCurrentItem(AIR);
                    }
                }
                return;
            case PICKUP_ALL:
                if (isThisInventoryClicked(event)) {
                    int slot = event.getSlot();
                    ItemDescriptor clickedStack = getDescriptor(slot);
                    if (clickedStack == null) {
                        return;
                    }
                    ItemStack receivedStack = takeItem(clickedStack, clickedStack.getMaxStackSize());
                    event.getView().setCursor(receivedStack);
                } else {
                    event.setCancelled(false);
                }
                return;
            case PICKUP_HALF:
                if (isThisInventoryClicked(event)) {
                    int slot = event.getSlot();
                    ItemDescriptor clickedStack = getDescriptor(slot);
                    if (clickedStack == null) {
                        return;
                    }
                    int amountClicked = getItem(slot).getAmount();
                    int amountTaken;
                    if (amountClicked % 2 == 0) {
                        amountTaken = amountClicked / 2;
                    } else {
                        amountTaken = Math.floorDiv(amountClicked, 2) + 1;
                    }
                    if (amountTaken > amountClicked) {
                        amountTaken = amountClicked;
                    }
                    ItemStack receivedStack = takeItem(clickedStack, amountTaken);
                    event.getView().setCursor(receivedStack);
                } else {
                    event.setCancelled(false);
                }
                return;
            case PLACE_ALL:
                if (isThisInventoryClicked(event)) {
                    ItemStack cursorStack = event.getCursor();
                    if (addItemStack(cursorStack)) {
                        event.getView().setCursor(AIR);
                    }
                } else {
                    event.setCancelled(false);
                }
                return;
            case PLACE_ONE:
                if (isThisInventoryClicked(event)) {
                    ItemStack cursorStack = event.getCursor().clone();
                    cursorStack.setAmount(1);
                    if (addItemStack(cursorStack)) {
                        ItemStack newCursorStack = event.getCursor().clone();
                        newCursorStack.setAmount(newCursorStack.getAmount() - 1);
                        event.getView().setCursor(newCursorStack);
                    }
                } else {
                    event.setCancelled(false);
                }
                return;
            case SWAP_WITH_CURSOR:
                if (isThisInventoryClicked(event)) {
                    ItemStack cursorStack = event.getCursor();
                    ItemDescriptor clickedStack = getDescriptor(event.getSlot());
                    if (addItemStack(cursorStack)) {
                        if (clickedStack == null) {
                            event.getView().setCursor(AIR);
                            return;
                        } else {
                            event.getView().setCursor(takeItem(clickedStack, clickedStack.getMaxStackSize()));
                        }
                    }
                } else {
                    event.setCancelled(false);
                }
                return;
            default:
                //DenseStorage.LOGGER.info("UNKNOWN ACTION: " + event.getAction());
                return;
        }
    }

    @Override
    public void onInventoryDrag(InventoryDragEvent event) {
        if (isThisInventoryDragged(event)) {
            event.setCancelled(true);
        }
    }

    /**
     * Returns the name of the inventory
     *
     * @return The String with the name of the inventory
     */
    @Override
    public String getName() {
        return "Dense Chest";
    }

    public ItemDescriptor getDescriptor(int index) {
        if (index >= BACKING_DATA.size()) {
            return null;
        }
        return BACKING_DATA.keySet().toArray(new ItemDescriptor[0])[index];
    }

    /**
     * Returns the ItemStack found in the slot at the given index
     *
     * @param index The index of the Slot's ItemStack to return
     * @return The ItemStack in the slot or AIR if none was found.
     */
    @Override
    public ItemStack getItem(int index) {
        if (VISUAL_CACHE == null) {
            VISUAL_CACHE = getContents();
        }
        return VISUAL_CACHE[index];
    }

    /**
     * Returns all ItemStacks from the inventory
     *
     * @return An array of ItemStacks from the inventory.
     */
    @Override
    public ItemStack[] getContents() {
        ItemStack[] array = new ItemStack[54];
        int index = 0;
        for (Map.Entry<ItemDescriptor, Integer> e : BACKING_DATA.entrySet()) {
            int storedCount = e.getValue();
            ItemDescriptor descriptor = e.getKey();
            int storedStacks = Math.floorDiv(storedCount, descriptor.getMaxStackSize());
            int storedRemainder = storedCount - (storedStacks * descriptor.getMaxStackSize());
            array[index] = descriptor.getItemStack(Math.min(storedCount, descriptor.getMaxStackSize()));
            ItemMeta meta = array[index].getItemMeta();
            if (meta == null) {
                try {
                    meta = CRAFT_META_ITEM_CONSTRUCTOR.newInstance(new NBTTagCompound());
                } catch (Exception error) {
                    throw new RuntimeException(error);
                }
            }

            final LinkedList<String> lore;
            if (meta.hasLore()) {
                lore = new LinkedList<>(meta.getLore());
                lore.addFirst("");
            } else {
                lore = new LinkedList<>();
            }
            lore.addFirst("§r§7Stored: §e" + storedCount + "§r§7 (§f" + storedStacks + "§7x" + descriptor.getMaxStackSize() + " + §f" + storedRemainder + "§7)");
            meta.setLore(lore);
            array[index].setItemMeta(meta);
            index++;
        }
        while (index < 54) {
            array[index] = AIR;
            index++;
        }
        return array;
    }
}