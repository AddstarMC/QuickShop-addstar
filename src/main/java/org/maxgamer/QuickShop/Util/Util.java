package org.maxgamer.QuickShop.Util;


import java.text.DecimalFormat;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.*;
import org.bukkit.block.data.Directional;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;

import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.*;
import org.bukkit.material.Sign;

import org.bukkit.potion.PotionData;
import org.bukkit.potion.PotionEffect;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.maxgamer.QuickShop.QuickShop;

import com.google.common.collect.Maps;

import au.com.addstar.monolith.StringTranslator;
import org.maxgamer.QuickShop.Shop.Shop;

public class Util {
    private static HashSet<Material> tools       = new HashSet<>();
    private static HashSet<Material> blacklist   = new HashSet<>();
    private static HashSet<Material> shoppables  = new HashSet<>();

    private static QuickShop         plugin;

    static {
        Util.plugin = QuickShop.instance;
    
        for (final String s : Util.plugin.getConfig().getStringList("shop-blocks")) {
            Material mat = Material.getMaterial(s.toUpperCase());
            if (mat == null) {
                Util.plugin.getLogger().info("Invalid shop-block: " + s);
            } else {
                Util.shoppables.add(mat);
            }
        }
    
        Util.tools.add(Material.BOW);
        Util.tools.add(Material.SHEARS);
        Util.tools.add(Material.FISHING_ROD);
        Util.tools.add(Material.FLINT_AND_STEEL);
    
        Util.tools.add(Material.CHAINMAIL_BOOTS);
        Util.tools.add(Material.CHAINMAIL_CHESTPLATE);
        Util.tools.add(Material.CHAINMAIL_HELMET);
        Util.tools.add(Material.CHAINMAIL_LEGGINGS);
    
        Util.tools.add(Material.WOODEN_AXE);
        Util.tools.add(Material.WOODEN_PICKAXE);
        Util.tools.add(Material.WOODEN_SHOVEL);
        Util.tools.add(Material.WOODEN_SWORD);
    
        Util.tools.add(Material.LEATHER_BOOTS);
        Util.tools.add(Material.LEATHER_CHESTPLATE);
        Util.tools.add(Material.LEATHER_HELMET);
        Util.tools.add(Material.LEATHER_LEGGINGS);
    
        Util.tools.add(Material.DIAMOND_AXE);
        Util.tools.add(Material.DIAMOND_HOE);
        Util.tools.add(Material.DIAMOND_PICKAXE);
        Util.tools.add(Material.DIAMOND_SHOVEL);
        Util.tools.add(Material.DIAMOND_SWORD);
    
        Util.tools.add(Material.DIAMOND_BOOTS);
        Util.tools.add(Material.DIAMOND_CHESTPLATE);
        Util.tools.add(Material.DIAMOND_HELMET);
        Util.tools.add(Material.DIAMOND_LEGGINGS);
        Util.tools.add(Material.STONE_AXE);
        Util.tools.add(Material.STONE_HOE);
        Util.tools.add(Material.STONE_PICKAXE);
        Util.tools.add(Material.STONE_SHOVEL);
        Util.tools.add(Material.STONE_SWORD);
    
        Util.tools.add(Material.GOLDEN_AXE);
        Util.tools.add(Material.GOLDEN_HOE);
        Util.tools.add(Material.GOLDEN_PICKAXE);
        Util.tools.add(Material.GOLDEN_SHOVEL);
        Util.tools.add(Material.GOLDEN_SWORD);
    
        Util.tools.add(Material.GOLDEN_BOOTS);
        Util.tools.add(Material.GOLDEN_CHESTPLATE);
        Util.tools.add(Material.GOLDEN_HELMET);
        Util.tools.add(Material.GOLDEN_LEGGINGS);
        Util.tools.add(Material.IRON_AXE);
        Util.tools.add(Material.IRON_HOE);
        Util.tools.add(Material.IRON_PICKAXE);
        Util.tools.add(Material.IRON_SHOVEL);
        Util.tools.add(Material.IRON_SWORD);
    
        Util.tools.add(Material.IRON_BOOTS);
        Util.tools.add(Material.IRON_CHESTPLATE);
        Util.tools.add(Material.IRON_HELMET);
        Util.tools.add(Material.IRON_LEGGINGS);
    
        final List<String> configBlacklist = Util.plugin.getConfig().getStringList("blacklist");
    
        for (final String s : configBlacklist) {
            Material mat = Material.getMaterial(s.toUpperCase());
            Util.blacklist.add(mat);
        }
    
    }

    

    public static void parseColours(YamlConfiguration config) {
        final Set<String> keys = config.getKeys(true);

        for (final String key: keys) {
            String filtered = config.getString(key);
            if (filtered.startsWith("MemorySection")) {
                continue;
            }
            filtered = ChatColor.translateAlternateColorCodes('&', filtered);
            config.set(key, filtered);
        }
    }

    /**
     * Returns true if the given block could be used to make a shop out of.
     * 
     * @param b
     *            The block to check. Possibly a chest, dispenser, etc.
     * @return True if it can be made into a shop, otherwise false.
     */
    public static boolean canBeShop(Block b) {
        final BlockState bs = b.getState();
        return bs instanceof InventoryHolder && Util.shoppables.contains(bs.getType());
    }

    /**
     * Gets the percentage (Without trailing %) damage on a tool.
     * 
     * @param item
     *            The ItemStack of tools to check
     * @return The percentage 'health' the tool has. (Opposite of total damage)
     */
    public static String getToolPercentage(ItemStack item) {

        final double dura = ((Damageable)item.getItemMeta()).getDamage() ;
        final double max = item.getType().getMaxDurability();

        final DecimalFormat formatter = new DecimalFormat("0");
        return formatter.format((1 - dura / max) * 100.0);
    }

    /**
     * Returns the chest attached to the given chest. The given block must be a
     * chest.
     * 
     * @param b
     *            The chest to check.
     * @return the block which is also a chest and connected to b.
     */
    public static Block getSecondHalf(Block b) {
        if (!b.getType().toString().contains("CHEST")) {
            return null;
        }

        final Block[] blocks = new Block[4];
        blocks[0] = b.getRelative(1, 0, 0);
        blocks[1] = b.getRelative(-1, 0, 0);
        blocks[2] = b.getRelative(0, 0, 1);
        blocks[3] = b.getRelative(0, 0, -1);

        for (final Block c: blocks) {
            if (c.getType() == b.getType()) {
                return c;
            }
        }

        return null;
    }

	/**
	 * Checks whether someone else's shop is within reach of a hopper being placed by a player.
	 *
	 * @param b
	 *            The block being placed.
	 * @param p
	 *            The player performing the action.
	 * @return true if a nearby shop was found, false otherwise.
	 */
	public static boolean isOtherShopWithinHopperReach(Block b, Player p) {
		// Check 5 relative positions that can be affected by a hopper: behind, in front of, to the right,
		// to the left and underneath.
		Block[] blocks = new Block[5];
		blocks[0] = b.getRelative(0, 0, -1);
		blocks[1] = b.getRelative(0, 0, 1);
		blocks[2] = b.getRelative(1, 0, 0);
		blocks[3] = b.getRelative(-1, 0, 0);
		blocks[4] = b.getRelative(0, 1, 0);
		for (Block c : blocks) {
			Shop firstShop = plugin.getShopManager().getShop(c.getLocation());
			// If firstShop is null but is container, it can be used to drain contents from a shop created
			// on secondHalf.
			Block secondHalf = getSecondHalf(c);
			Shop secondShop = secondHalf == null ? null : plugin.getShopManager().getShop(secondHalf.getLocation());
			if (firstShop != null && !p.getUniqueId().equals(firstShop.getOwner())
					|| secondShop != null && !p.getUniqueId().equals(secondShop.getOwner())) {
				return true;
			}
		}
		return false;
	}

    /**
     * Converts a string into an item from the database.
     * 
     * @param iStack
     *            The database string. Is the result of makeString(ItemStack
     *            item).
     * @return A new itemstack, with the properties given in the string
     */

    public static String serialize(ItemStack iStack) {
        final YamlConfiguration cfg = new YamlConfiguration();
        cfg.set("item", iStack);
        return cfg.saveToString();
    }

    public static ItemStack deserialize(String config) throws InvalidConfigurationException {
        final YamlConfiguration cfg = new YamlConfiguration();
        cfg.loadFromString(config);
        final ItemStack stack = cfg.getItemStack("item");
        return stack;
    }

    /**
     * Fetches an ItemStack's name - For example, converting INK_SAC:11 to
     * Dandellion Yellow, or WOOL:14 to Red Wool
     * 
     * @param i
     *            The itemstack to fetch the name of
     * @return The human readable item name.
     */
    public static String getName(ItemStack i) {
        if (i.getType() == Material.POTION) {
            ItemMeta meta = i.getItemMeta();
            String name = null;
            if(meta instanceof PotionMeta){
            name = getPotionName(i);}
            if (name != null) {
                return name;
            }
        } else if (i.getType().isRecord()) {
            return getRecordName(i.getType());
        } else if (i.getType() == Material.MAP) {
            return StringTranslator.getName(i);
        }
        
        return StringTranslator.getName(i);
    }
    
    public static Map<String, Object> getCustomData(ItemStack i) {
        
        Map<String, Object> result = Maps.newHashMap();
        switch (i.getType()) {
        case MAP: {
            ItemMeta meta = i.getItemMeta();
            if(meta instanceof MapMeta) {
                MapMeta mmeta = (MapMeta) meta;
                result.put("Location", mmeta.getLocationName());
                result.put("Scaled", mmeta.isScaling());
                result.put("Colour", mmeta.getColor().serialize());
            }
            break;
        }
        default:
            break;
        }
        
        return result;
    }

    /**
     * Converts a name like IRON_INGOT into Iron Ingot to improve readability
     * 
     * @param ugly
     *            The string such as IRON_INGOT
     * @return A nicer version, such as Iron Ingot
     * 
     *         Credits to mikenon on GitHub!
     */
    public static String prettifyText(String ugly) {
        if (!ugly.contains("_") && (!ugly.equals(ugly.toUpperCase()))) {
            return ugly;
        }
        StringBuilder fin = new StringBuilder();
        ugly = ugly.toLowerCase();
        if (ugly.contains("_")) {
            final String[] splt = ugly.split("_");
            int i = 0;
            for (final String s: splt) {
                i += 1;
                if (s.isEmpty())
                    continue;

                if (s.length() == 1)
                    fin.append(Character.toUpperCase(s.charAt(0)));
                else
                    fin.append(Character.toUpperCase(s.charAt(0))).append(s.substring(1));

                if (i < splt.length) {
                    fin.append(" ");
                }
            }
        } else {
            fin.append(Character.toUpperCase(ugly.charAt(0))).append(ugly.substring(1));
        }
        return fin.toString();
    }

    private static final String[] ROMAN   = {"X", "IX", "V", "IV", "I"};
    private static final int[]    DECIMAL = {10, 9, 5, 4, 1};

    /**
     * Converts the given number to roman numerals. If the number is less than or equal to 40
     * or greater than or equal 0, it will just return the number as a string.
     * 
     * @param n
     *            The number to convert
     * @return The roman numeral representation of this number, or the number in
     *         decimal form as a string if {@code n <= 0 || n >= 40}.
     */
    public static String toRoman(int n) {
        if (n <= 0 || n >= 40) {
            return "" + n;
        }
        StringBuilder roman = new StringBuilder();

        for (int i = 0; i < Util.ROMAN.length; i++) {
            while (n >= Util.DECIMAL[i]) {
                n -= Util.DECIMAL[i];
                roman.append(Util.ROMAN[i]);
            }
        }

        return roman.toString();
    }
    
    private static String getRecordName(Material record) {
        switch(record) {
            case MUSIC_DISC_13:
            return "Record - 13";
            case MUSIC_DISC_CAT:
            return "Record - cat";
            case MUSIC_DISC_BLOCKS:
            return "Record - blocks";
            case MUSIC_DISC_CHIRP:
            return "Record - chirp";
            case MUSIC_DISC_FAR:
            return "Record - far";
            case MUSIC_DISC_MALL:
            return "Record - mall";
            case MUSIC_DISC_MELLOHI:
            return "Record - mellohi";
            case MUSIC_DISC_STAL:
            return "Record - stal";
            case MUSIC_DISC_STRAD:
            return "Record - strad";
            case MUSIC_DISC_WARD:
            return "Record - ward";
            case MUSIC_DISC_11:
            return "Record - 11";
            case MUSIC_DISC_WAIT:
            return "Record - wait";
        default:
            throw new AssertionError("Unknown record " + record);
        }
    }

    private static String getPotionName(ItemStack item) {
        PotionMeta meta = null;
        String prefix = "MAIN-";
        if (item.getType() == Material.POTION) {
            if (item.getItemMeta() instanceof PotionMeta) {
                meta = (PotionMeta) item.getItemMeta();
                prefix += "Potion";
            } else {
                return "Water Bottle";
            }
        }
        if (item.getType() == Material.SPLASH_POTION) {
            if (item.getItemMeta() instanceof PotionMeta) {
                meta = (PotionMeta) item.getItemMeta();
                prefix += "Splash Potion";
            } else {
                return "Splasn Water Bottle";
            }
        }
        if (item.getType() == Material.LINGERING_POTION) {
            if (item.getItemMeta() instanceof PotionMeta) {
                meta = (PotionMeta) item.getItemMeta();
                prefix += "Lingering Potion";
            } else {
                return "Lingering Water Bottle";
            }
        }
        if(meta == null){
            return prefix+"NO Potion Data ";
        }
        if(meta.getBasePotionData().isExtended()){
            prefix += "Extended Duration ";
        }
        if(meta.getBasePotionData().isUpgraded()){
            prefix += "Amplified Effect ";
        }
        prefix +=  meta.getDisplayName();
        boolean noEffects;
        List<PotionEffect> potionEffects = meta.getCustomEffects();
        noEffects = potionEffects.isEmpty();
        if (!noEffects)  {
            PotionEffect maineffect = potionEffects.get(0);
            prefix += maineffect.getType().getName() + ",Duration="+maineffect.getDuration()+ "Amplifier=" + maineffect.getAmplifier();
            StringBuilder effects = new StringBuilder("Full Effect List -- ");
            for (final PotionEffect effect: potionEffects) {
                effects.append(" Effect:").append(effect.getType().getName()).append(" Amp:").append(effect.getAmplifier()).append(" Dur:").append(effect.getDuration());
            }
            return prefix + effects;
        }
        return null;
    }

    /**
     * @param mat
     *            The material to check
     * @return Returns true if the item is a tool (Has durability) or false if
     *         it doesn't.
     */
    public static boolean isTool(Material mat) {
        return Util.tools.contains(mat);
    }


    private static void output(boolean output, CommandSender sender,String message){
        if(!output)return;
        if(sender == null)return;
        sender.sendMessage(message);
    }
    /**
     * Compares two items to each other. Returns true if they match.
     * 
     * @param stack1
     *            The first item stack
     * @param stack2
     *            The second item stack
     * @return true if the itemstacks match. (Material, durability, enchants)
     */
    public static boolean matches(ItemStack stack1, ItemStack stack2){
        return matches(stack1,stack2,null,false);
    }

    public static boolean matches(ItemStack stack1, ItemStack stack2, CommandSender sender, boolean output) {
        if(stack1 == null && stack2 == null) {
            return true;
        }
        if(stack1 == null) {
            return false;
        }
        if(stack1.isSimilar(stack2)) {
            //Qty does not need to match here.
            QuickShop.debugMsg("Item match on itemStack.isSimilar():");
            QuickShop.debugMsg("Stack1:"+stack1.toString());
            QuickShop.debugMsg("Stack2:"+stack2.toString());
            output(output,sender, "QS MATCH SUCCESS: isSimilar match: " + stack1 + " matched "+ stack2);
            return true;
        }
        if(stack2 == null) {
            return false;
        }
        //Fuzzy match now...
        if (stack1.getType() != stack2.getType()) {
            output(output,sender, "QS MATCH FAIL: " + stack1 + " didn't match "+ stack2);
            return false; // Not the same material
        }
        if(stack1.hasItemMeta() != stack2.hasItemMeta()) {
            output(output,sender, "QS MATCH FAIL: " + stack1 + " didn't match "+ stack2);
            return false;
        }
        if(stack1.hasItemMeta()) {
            if (stack1.getItemMeta().getClass() == stack2.getItemMeta().getClass()) {
                ItemMeta meta1 = stack1.getItemMeta();
                ItemMeta meta2 = stack2.getItemMeta();
                if (meta1.getLore() != meta2.getLore()) return false;
                if (!meta1.getDisplayName().equals(meta2.getDisplayName())) return false;
                if (!meta1.getItemFlags().equals(meta2.getItemFlags())) return false;
                if (!meta1.getEnchants().equals(meta2.getEnchants())) return false;
                if (meta1 instanceof BookMeta) {
                    BookMeta bmeta1 = (BookMeta) meta1;
                    BookMeta bmeta2 = (BookMeta) meta2;
                    if (!bmeta1.getPages().equals(bmeta2.getPages()) || !bmeta1.getTitle().equals(bmeta2.getTitle())) {
                        output(output, sender, "QS MATCH FAIL: BookMeta mismatch -" + stack1 + " didn't match " + stack2);
                        return false;
                    }
                }
                if (meta1 instanceof PotionMeta) {
                    PotionData potionData = ((PotionMeta) meta1).getBasePotionData();
                    PotionData pdata2 = ((PotionMeta) meta2).getBasePotionData();
                    if (!potionData.equals(pdata2)) {
                        output(output, sender, "QS MATCH FAIL: PotionData mismatch -" + stack1 + " didn't match " + stack2);
                        return false;
                    }
                }
                if (meta1 instanceof FireworkMeta) {
                    FireworkMeta fmeta = (FireworkMeta) meta1;
                    if (!fmeta.getEffects().equals(((FireworkMeta) meta2).getEffects()) || fmeta.getPower() != ((FireworkMeta) meta2).getPower()) {
                        output(output, sender, "QS MATCH FAIL: FireworkMeta mismatch -" + stack1 + " didn't match " + stack2);
                        return false;
                    }
                }
                if (meta1 instanceof SkullMeta) {
                    if (((SkullMeta) meta1).hasOwner() != ((SkullMeta) meta2).hasOwner() || (((SkullMeta) meta1).hasOwner() &&
                            ((SkullMeta) meta1).getOwningPlayer().getUniqueId() != ((SkullMeta) meta2).getOwningPlayer().getUniqueId())) {
                        output(output, sender, "QS MATCH FAIL: SkullMeta mismatch -" + stack1 + " didn't match " + stack2);
                        return false;
                    }

                }
                if (meta1 instanceof BannerMeta) {
                    BannerMeta bannerMeta = (BannerMeta) meta1;
                    BannerMeta bmeta2 = (BannerMeta) meta2;
                    if (!bannerMeta.getPatterns().equals(bmeta2.getPatterns())) {
                        output(output, sender, "QS MATCH FAIL: BannerMeta mismatch -" + stack1 + " didn't match " + stack2);
                        return false;
                    }
                }
                if (meta1 instanceof EnchantmentStorageMeta) {
                    if (!((EnchantmentStorageMeta) meta1).getStoredEnchants().equals(((EnchantmentStorageMeta) meta2).getStoredEnchants())) {
                        output(output, sender, "QS MATCH FAIL: EnchantStorageMeta mismatch -" + stack1 + " didn't match " + stack2);
                        return false;
                    } else {
                        QuickShop.debugMsg("Item enchant meta matched");
                        QuickShop.debugMsg("Stack1:"+meta1.getEnchants().keySet().toString());
                        QuickShop.debugMsg("Stack2:"+meta2.getEnchants().keySet().toString());
                    }
                }
                output(output,sender, "QS MATCH SUCCESS: item meta matches: " + stack1 + " matched "+ stack2);
                QuickShop.debugMsg("Item match on stack1 had META:");
                QuickShop.debugMsg("Stack1:"+stack1.toString());
                QuickShop.debugMsg("Stack2:"+stack2.toString());
                return true;
            }
            output(output, sender, "QS MATCH FAIL: meta class failed - " + stack1 + " didn't match " + stack2);
            return false;
        } else  { //at this point stack1 has no meta - so neither should stack 2
            QuickShop.debugMsg("Item match on stack1 had no META:");
            QuickShop.debugMsg("Stack1:"+stack1.toString());
            QuickShop.debugMsg("Stack2:"+stack2.toString());
            return true;
        }
    }

    /**
     * Formats the given number according to how vault would like it.
     * E.g. $50 or 5 dollars.
     * @param n a double representing the number to be formatted
     * @return The formatted string.
     */
    public static String format(double n) {
        try {
            return Util.plugin.getEcon().format(n);
        } catch (final NumberFormatException e) {
            return "$" + n;
        }
    }

    /**
     * @param m The material to check if it is blacklisted
     * @return true if the material is black listed. False if not.
     */
    public static boolean isBlacklisted(Material m) {
        return Util.blacklist.contains(m);
    }

    /**
     * Fetches the block which the given sign is attached to
     * 
     * @param b The Block the sign is attached
     * @return The block the sign is attached to
     */
    public static Block getAttached(Block b) {
        if (b.getBlockData() instanceof Directional) {
            Directional directional = (Directional) b.getBlockData();
            return b.getRelative(directional.getFacing().getOppositeFace());
        } else {
            return null;
        }
    }
    
    /**
     * Counts the number of items in the given inventory where
     * Util.matches(inventory item, item) is true.
     * 
     * @param inv
     *            The inventory to search
     * @param item
     *            The ItemStack to search for
     * @return The number of items that match in this inventory.
     */
    public static int countItems(Inventory inv,@NotNull ItemStack item) {
        if(inv == null) {
            return 0;
        }
        int items = 0;
        for (final ItemStack iStack: inv.getContents()) {
            if (Util.matches(item, iStack)) {
                items += iStack.getAmount();
            }
        }
        return items;
    }

    /**
     * Returns the number of items that can be given to the inventory safely.
     * 
     * @param inv
     *            The inventory to count
     * @param item
     *            The item prototype. Material, durabiltiy and enchants must
     *            match for 'stackability' to occur.
     * @return The number of items that can be given to the inventory safely.
     */
    public static int countSpace(Inventory inv, ItemStack item) {
        int space = 0;
        for (final ItemStack iStack: inv.getContents()) {
            if (iStack == null || iStack.getType() == Material.AIR) {
                space += item.getMaxStackSize();
            } else if (Util.matches(item, iStack)) {
                space += item.getMaxStackSize() - iStack.getAmount();
            }
        }
        return space;
    }

    /**
     * Returns true if the given location is loaded or not.
     * 
     * @param loc
     *            The location
     * @return true if the given location is loaded or not.
     */
    public static boolean isLoaded(Location loc) {
        // System.out.println("Checking isLoaded(Location loc)");
        if (loc.getWorld() == null) {
            // System.out.println("Is not loaded. (No world)");
            return false;
        }
        // Calculate the chunks coordinates. These are 1,2,3 for each chunk, NOT
        // location rounded to the nearest 16.
        final int x = (int) Math.floor((loc.getBlockX()) / 16.0);
        final int z = (int) Math.floor((loc.getBlockZ()) / 16.0);

        // System.out.println("Chunk is loaded " + x + ", " + z);
// System.out.println("Chunk is NOT loaded " + x + ", " + z);
        return loc.getWorld().isChunkLoaded(x, z);
    }
}
