package cf.terminator.densestorage.inventory;

import cf.terminator.densestorage.throwables.InvalidMinecraftVersionException;
import cf.terminator.densestorage.util.NBTUtils;
import net.minecraft.server.v1_14_R1.NBTTagCompound;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Utility;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.craftbukkit.v1_14_R1.inventory.CraftItemStack;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.material.MaterialData;

import javax.annotation.Nonnull;
import java.lang.reflect.Field;
import java.util.Map;

/**
 * Based off ItemStack, but does not hold or compare the item amount.
 */
public class ItemDescriptor implements Comparable<ItemDescriptor>{

    private Material type;
    private MaterialData data;
    private ItemMeta meta;

    /* Provides ways to differentiate metadata from eachother.
    *  Gets more specific the further in the array.
    * */
    private static final Class CRAFT_META_ITEM_CLASS;
    private static final Field NBT_EXTRACT_METHOD;
    static {
        try {
            CRAFT_META_ITEM_CLASS = Class.forName("org.bukkit.craftbukkit.v1_14_R1.inventory.CraftMetaItem");
            NBT_EXTRACT_METHOD = CRAFT_META_ITEM_CLASS.getDeclaredField("internalTag");
            NBT_EXTRACT_METHOD.setAccessible(true);
        } catch (Exception e) {
            throw new InvalidMinecraftVersionException(e);
        }
    }


    private static final MetaIntExtractor[] META_INT_EXTRACTORS = new MetaIntExtractor[]{
        new MetaIntExtractor() {
            @Override
            public int call(ItemMeta meta) {
                return meta.getItemFlags().size();
            }
        }, new MetaIntExtractor() {
            @Override
            public int call(ItemMeta meta) {
                if(meta.hasLore()){
                    return meta.getLore().size();
                }
                return 0;
            }
        }, new MetaIntExtractor() {
            @Override
            public int call(ItemMeta meta) {
                return meta.getEnchants().size();
            }
        },new MetaIntExtractor() {
            @Override
            public int call(ItemMeta meta) {
                int hash = 0;
                for(ItemFlag flags : meta.getItemFlags()){
                    hash = hash + flags.hashCode();
                }
                return hash;
            }
        }, new MetaIntExtractor() {
            @Override
            public int call(ItemMeta meta) {
                if(meta.hasLore()){
                    int hash = 0;
                    for(String lines : meta.getLore()){
                        hash = hash + lines.hashCode();
                    }
                    return hash;
                }
                return 0;
            }
        }, new MetaIntExtractor() {
            @Override
            public int call(ItemMeta meta) {
                int hash = 0;
                for(Map.Entry<Enchantment, Integer> e : meta.getEnchants().entrySet()){
                    hash = hash + (e.getKey().hashCode() * e.getValue());
                }
                return hash;
            }
        }, new MetaIntExtractor() {
            @Override
            public int call(ItemMeta meta) {
                if (meta.hasAttributeModifiers() == false) {
                    return 0;
                }
                int hash = 0;
                for (Map.Entry<Attribute, AttributeModifier> e : meta.getAttributeModifiers().entries()) {
                    hash = hash + (e.getKey().ordinal() * e.getValue().hashCode());
                }
                return hash;
            }
        }
    };

    public ItemDescriptor(ItemStack stack){
        if(stack.getType() == Material.AIR){
            throw new NullPointerException("Air item detected.");
        }
        type = stack.getType();
        data = stack.getData();
        meta = stack.getItemMeta();
    }

    public ItemStack getItemStack(int count){
        ItemStack stack = new ItemStack(type);
        if(stack.getType() == Material.AIR){
            throw new NullPointerException("Air item detected.");
        }
        stack.setAmount(count);
        stack.setData(data);
        stack.setItemMeta(meta);
        return stack;
    }

    /**
     * This method is the same as equals, but does not consider stack size
     * (amount).
     *
     * @param stack the item stack to compare to
     * @return true if the two stacks are equal, ignoring the amount
     */
    @Utility
    public boolean isSimilar(ItemStack stack) {
        return getItemStack(1).isSimilar(stack);
    }

    public Material getType(){
        return type;
    }

    @Utility
    public int getMaxStackSize() {
        Material material = type;
        if (material != null) {
            return Math.max(material.getMaxStackSize(), 1);
        }
        return 1;
    }

    @Override
    @Utility
    public int hashCode() {
        return type.hashCode();
    }

    /**
     * Tries its best to differentiate the items properly.
     * @param o the other descriptor
     * @return compared
     */
    @Override
    public int compareTo(@Nonnull ItemDescriptor o) {
        if(equals(o)){
            return 0;
        }
        int c = Integer.compare(o.type.ordinal(), this.type.ordinal());
        if (c != 0) {
            return c;
        }

        for(MetaIntExtractor extractor : META_INT_EXTRACTORS){
            int compared = Integer.compare(extractor.call(this.meta), extractor.call(o.meta));
            if(compared != 0){
                return compared;
            }
        }

        try {

            net.minecraft.server.v1_14_R1.ItemStack stackOne = CraftItemStack.asNMSCopy(this.getItemStack(1));
            net.minecraft.server.v1_14_R1.ItemStack stackTwo = CraftItemStack.asNMSCopy(o.getItemStack(1));

            NBTTagCompound thisTag = stackOne.save(new NBTTagCompound());
            NBTTagCompound thatTag = stackTwo.save(new NBTTagCompound());
            return NBTUtils.compare(thisTag, thatTag);
        } catch (Exception e) {
            throw new InvalidMinecraftVersionException(e);
        }
    }




    @Override
    public boolean equals(Object descriptor){
        if(descriptor instanceof ItemDescriptor == false){
            return false;
        }
        return ((ItemDescriptor) descriptor).getItemStack(1).equals(getItemStack(1));
    }

    /**
     * Gets the durability of this item
     *
     * @return Durability of this item
     */
    public short getDurability() {
        ItemMeta meta = getItemMeta();
        return (meta == null) ? 0 : (short) ((Damageable) meta).getDamage();
    }

    /**
     * Get a copy of this ItemStack's {@link ItemMeta}.
     *
     * @return a copy of the current ItemStack's ItemData
     */
    public ItemMeta getItemMeta() {
        return this.meta == null ? Bukkit.getItemFactory().getItemMeta(this.type) : this.meta.clone();
    }

    /**
     * Gets the MaterialData for this stack of items
     *
     * @return MaterialData for this item
     */
    public MaterialData getData() {
        Material mat = Bukkit.getUnsafe().toLegacy(type);
        if (data == null && mat != null && mat.getData() != null) {
            data = mat.getNewData((byte) this.getDurability());
        }

        return data;
    }

    /**
     * Checks to see if any meta data has been defined.
     *
     * @return Returns true if some meta data has been set for this item
     */
    public boolean hasItemMeta() {
        return !Bukkit.getItemFactory().equals(meta, null);
    }


    private interface MetaIntExtractor {
        int call(ItemMeta meta);
    }
}
