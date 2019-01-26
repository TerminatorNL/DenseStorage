package cf.terminator.densestorage.inventory;

import net.minecraft.server.v1_13_R2.EntityPlayer;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;

/**
 * Handles all basic operations needed for a GUI-only inventory.
 * All but a few methods throw UnsupportedOperationException.
 */
public abstract class BaseInventory implements Inventory {

    public static final ItemStack AIR = new ItemStack(Material.AIR);

    /**
     * Returns the size of the inventory
     *
     * @return The size of the inventory
     */
    @Override
    public int getSize() {
        /* Hide/Workaround stupid bug. */
        StackTraceElement[] trace = Thread.currentThread().getStackTrace();
        if(trace[2].getClassName().equals("org.bukkit.craftbukkit.v1_13_R2.inventory.InventoryWrapper") &&
                trace[3].getClassName().equals("org.bukkit.craftbukkit.v1_13_R2.inventory.InventoryWrapper")){
            return 0;
        }
        return 54;
    }

    /**
     * Returns the maximum stack size for an ItemStack in this inventory.
     *
     * @return The maximum size for an ItemStack in this inventory.
     */
    @Override
    public int getMaxStackSize() {
        return 64;
    }

    protected List<HumanEntity> viewerList = new ArrayList<>();

    /**
     * Gets a list of players viewing the inventory. Note that a player is
     * considered to be viewing their own inventory and internal crafting
     * screen even when said inventory is not open. They will normally be
     * considered to be viewing their inventory even when they have a
     * different inventory screen open, but it's possible for customized
     * inventory screens to exclude the viewer's inventory, so this should
     * never be assumed to be non-empty.
     *
     * @return A list of HumanEntities who are viewing this Inventory.
     */
    @Override
    public List<HumanEntity> getViewers() {
        return new ArrayList<>(viewerList);
    }

    public final void notifyPlayerInventoryClose(HumanEntity player){
        viewerList.remove(player);
        onInventoryClose(player);
    }
    public final void notifyPlayerInventoryOpen(HumanEntity player){
        viewerList.add(player);
        onInventoryOpen(player);
    }

    public abstract void onInventoryOpen(HumanEntity player);
    public abstract void onInventoryClose(HumanEntity player);
    public abstract void onInventoryClick(InventoryClickEvent event);
    public abstract void onInventoryDrag(InventoryDragEvent event);

    public boolean isThisInventoryClicked(InventoryClickEvent e){
        int slot =  e.getRawSlot();
        return slot < getSize();
    }

    public boolean isThisInventoryDragged(InventoryDragEvent e){
        for(Integer slot : e.getRawSlots()){
            if(slot < getSize()){
                return true;
            }
        }
        return false;
    }

    /**
     * Returns the title of this inventory.
     *
     * @return A String with the title.
     */
    @Override
    public String getTitle(){
        return getName();
    }

    /**
     * Returns what type of inventory this is.
     *
     * @return The InventoryType representing the type of inventory.
     */
    @Override
    public InventoryType getType() {
        return InventoryType.CHEST;
    }

    /**
     * Gets the block or entity belonging to the open inventory
     *
     * @return The holder of the inventory; null if it has no holder.
     */
    @Override
    public InventoryHolder getHolder() {
        return null;
    }






    /**
     * This method allows you to change the maximum stack size for an
     * inventory.
     * <p>
     * <b>Caveats:</b>
     * <ul>
     * <li>Not all inventories respect this value.
     * <li>Stacks larger than 127 may be clipped when the world is saved.
     * <li>This value is not guaranteed to be preserved; be sure to set it
     * before every time you want to set a slot over the max stack size.
     * <li>Stacks larger than the default max size for this type of inventory
     * may not display correctly in the client.
     * </ul>
     *
     * @param size The new maximum stack size for items in this inventory.
     */
    @Override
    public void setMaxStackSize(int size) {

    }

    /**
     * Returns the name of the inventory
     *
     * @return The String with the name of the inventory
     */
    @Override
    public abstract String getName();



    /**
     * Returns the ItemStack found in the slot at the given index
     *
     * @param index The index of the Slot's ItemStack to return
     * @return The ItemStack in the slot
     */
    @Override
    public abstract ItemStack getItem(int index);

    /**
     * Stores the ItemStack at the given index of the inventory.
     *
     * @param index The index where to put the ItemStack
     * @param item  The ItemStack to set
     */
    @Override
    public void setItem(int index, ItemStack item){
        throw new UnsupportedOperationException();
    }

    /**
     * Stores the given ItemStacks in the inventory. This will try to fill
     * existing stacks and empty slots as well as it can.
     * <p>
     * The returned HashMap contains what it couldn't store, where the key is
     * the index of the parameter, and the value is the ItemStack at that
     * index of the varargs parameter. If all items are stored, it will return
     * an empty HashMap.
     * <p>
     * If you pass in ItemStacks which exceed the maximum stack size for the
     * Material, first they will be added to partial stacks where
     * Material.getMaxStackSize() is not exceeded, up to
     * Material.getMaxStackSize(). When there are no partial stacks left
     * stacks will be split on Inventory.getMaxStackSize() allowing you to
     * exceed the maximum stack size for that material.
     * <p>
     * It is known that in some implementations this method will also set
     * the inputted argument amount to the number of that item not placed in
     * slots.
     *
     * @param items The ItemStacks to add
     * @return A HashMap containing items that didn't fit.
     * @throws IllegalArgumentException if items or any element in it is null
     */
    @Override
    public HashMap<Integer, ItemStack> addItem(ItemStack... items) throws IllegalArgumentException{
        throw new UnsupportedOperationException();
    }

    /**
     * Removes the given ItemStacks from the inventory.
     * <p>
     * It will try to remove 'as much as possible' from the types and amounts
     * you give as arguments.
     * <p>
     * The returned HashMap contains what it couldn't remove, where the key is
     * the index of the parameter, and the value is the ItemStack at that
     * index of the varargs parameter. If all the given ItemStacks are
     * removed, it will return an empty HashMap.
     * <p>
     * It is known that in some implementations this method will also set the
     * inputted argument amount to the number of that item not removed from
     * slots.
     *
     * @param items The ItemStacks to remove
     * @return A HashMap containing items that couldn't be removed.
     * @throws IllegalArgumentException if items is null
     */
    @Override
    public HashMap<Integer, ItemStack> removeItem(ItemStack... items) throws IllegalArgumentException{
        throw new UnsupportedOperationException();
    }

    /**
     * Returns all ItemStacks from the inventory
     *
     * @return An array of ItemStacks from the inventory.
     */
    @Override
    public abstract ItemStack[] getContents();

    /**
     * Completely replaces the inventory's contents. Removes all existing
     * contents and replaces it with the ItemStacks given in the array.
     *
     * @param items A complete replacement for the contents; the length must
     *              be less than or equal to {@link #getSize()}.
     * @throws IllegalArgumentException If the array has more items than the
     *                                  inventory.
     */
    @Override
    public void setContents(ItemStack[] items) throws IllegalArgumentException{
        throw new UnsupportedOperationException();
    }

    /**
     * Return the contents from the section of the inventory where items can
     * reasonably be expected to be stored. In most cases this will represent
     * the entire inventory, but in some cases it may exclude armor or result
     * slots.
     * <br>
     * It is these contents which will be used for add / contains / remove
     * methods which look for a specific stack.
     *
     * @return inventory storage contents
     */
    @Override
    public ItemStack[] getStorageContents(){
        return getContents();
    }


    /**
     * Put the given ItemStacks into the storage slots
     *
     * @param items The ItemStacks to use as storage contents
     * @throws IllegalArgumentException If the array has more items than the
     *                                  inventory.
     */
    @Override
    public void setStorageContents(ItemStack[] items) throws IllegalArgumentException{
        throw new UnsupportedOperationException();
    }

    /**
     * Checks if the inventory contains any ItemStacks with the given
     * material.
     *
     * @param material The material to check for
     * @return true if an ItemStack is found with the given Material
     * @throws IllegalArgumentException if material is null
     */
    @Override
    public boolean contains(Material material) throws IllegalArgumentException{
        throw new UnsupportedOperationException();
    }

    /**
     * Checks if the inventory contains any ItemStacks matching the given
     * ItemStack.
     * <p>
     * This will only return true if both the type and the amount of the stack
     * match.
     *
     * @param item The ItemStack to match against
     * @return false if item is null, true if any exactly matching ItemStacks
     * were found
     */
    @Override
    public boolean contains(ItemStack item){
        throw new UnsupportedOperationException();
    }

    /**
     * Checks if the inventory contains any ItemStacks with the given
     * material, adding to at least the minimum amount specified.
     *
     * @param material The material to check for
     * @param amount   The minimum amount
     * @return true if amount is less than 1, true if enough ItemStacks were
     * found to add to the given amount
     * @throws IllegalArgumentException if material is null
     */
    @Override
    public boolean contains(Material material, int amount) throws IllegalArgumentException{
        throw new UnsupportedOperationException();
    }

    /**
     * Checks if the inventory contains at least the minimum amount specified
     * of exactly matching ItemStacks.
     * <p>
     * An ItemStack only counts if both the type and the amount of the stack
     * match.
     *
     * @param item   the ItemStack to match against
     * @param amount how many identical stacks to check for
     * @return false if item is null, true if amount less than 1, true if
     * amount of exactly matching ItemStacks were found
     * @see #containsAtLeast(ItemStack, int)
     */
    @Override
    public boolean contains(ItemStack item, int amount){
        throw new UnsupportedOperationException();
    }

    /**
     * Checks if the inventory contains ItemStacks matching the given
     * ItemStack whose amounts sum to at least the minimum amount specified.
     *
     * @param item   the ItemStack to match against
     * @param amount the minimum amount
     * @return false if item is null, true if amount less than 1, true if
     * enough ItemStacks were found to add to the given amount
     */
    @Override
    public boolean containsAtLeast(ItemStack item, int amount){
        throw new UnsupportedOperationException();
    }

    /**
     * Returns a HashMap with all slots and ItemStacks in the inventory with
     * the given Material.
     * <p>
     * The HashMap contains entries where, the key is the slot index, and the
     * value is the ItemStack in that slot. If no matching ItemStack with the
     * given Material is found, an empty map is returned.
     *
     * @param material The material to look for
     * @return A HashMap containing the slot index, ItemStack pairs
     * @throws IllegalArgumentException if material is null
     */
    @Override
    public HashMap<Integer, ? extends ItemStack> all(Material material) throws IllegalArgumentException{
        throw new UnsupportedOperationException();
    }

    /**
     * Finds all slots in the inventory containing any ItemStacks with the
     * given ItemStack. This will only match slots if both the type and the
     * amount of the stack match
     * <p>
     * The HashMap contains entries where, the key is the slot index, and the
     * value is the ItemStack in that slot. If no matching ItemStack with the
     * given Material is found, an empty map is returned.
     *
     * @param item The ItemStack to match against
     * @return A map from slot indexes to item at index
     */
    @Override
    public HashMap<Integer, ? extends ItemStack> all(ItemStack item){
        throw new UnsupportedOperationException();
    }

    /**
     * Finds the first slot in the inventory containing an ItemStack with the
     * given material
     *
     * @param material The material to look for
     * @return The slot index of the given Material or -1 if not found
     * @throws IllegalArgumentException if material is null
     */
    @Override
    public int first(Material material) throws IllegalArgumentException{
        throw new UnsupportedOperationException();
    }

    /**
     * Returns the first slot in the inventory containing an ItemStack with
     * the given stack. This will only match a slot if both the type and the
     * amount of the stack match
     *
     * @param item The ItemStack to match against
     * @return The slot index of the given ItemStack or -1 if not found
     */
    @Override
    public int first(ItemStack item){
        throw new UnsupportedOperationException();
    }
    /**
     * Returns the first empty Slot.
     *
     * @return The first empty Slot found, or -1 if no empty slots.
     */
    @Override
    public int firstEmpty(){
        throw new UnsupportedOperationException();
    }

    /**
     * Removes all stacks in the inventory matching the given material.
     *
     * @param material The material to remove
     * @throws IllegalArgumentException if material is null
     */
    @Override
    public void remove(Material material) throws IllegalArgumentException{
        throw new UnsupportedOperationException();
    }

    /**
     * Removes all stacks in the inventory matching the given stack.
     * <p>
     * This will only match a slot if both the type and the amount of the
     * stack match
     *
     * @param item The ItemStack to match against
     */
    @Override
    public void remove(ItemStack item){
        throw new UnsupportedOperationException();
    }

    /**
     * Clears out a particular slot in the index.
     *
     * @param index The index to empty.
     */
    @Override
    public void clear(int index){
        throw new UnsupportedOperationException();
    }

    /**
     * Clears out the whole Inventory.
     */
    @Override
    public void clear(){
        throw new UnsupportedOperationException();
    }


    @Override
    public @Nonnull ListIterator<ItemStack> iterator(){
        throw new UnsupportedOperationException();
    }


    /**
     * Returns an iterator starting at the given index. If the index is
     * positive, then the first call to next() will return the item at that
     * index; if it is negative, the first call to previous will return the
     * item at index (getSize() + index).
     *
     * @param index The index.
     * @return An iterator.
     */
    @Override
    public ListIterator<ItemStack> iterator(int index){
        throw new UnsupportedOperationException();
    }


    /**
     * Get the location of the block or entity which corresponds to this inventory. May return null if this container
     * was custom created or is a virtual / subcontainer.
     *
     * @return location or null if not applicable.
     */
    @Override
    public Location getLocation() {
        return null;
    }
}
