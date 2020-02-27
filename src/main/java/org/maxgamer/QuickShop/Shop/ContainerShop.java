package org.maxgamer.QuickShop.Shop;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.maxgamer.QuickShop.QuickShop;
import org.maxgamer.QuickShop.Util.MsgUtil;
import org.maxgamer.QuickShop.Util.Util;
import org.maxgamer.QuickShop.exceptions.InvalidShopException;

public class ContainerShop implements Shop {
    private final Location  loc;
    private double          price;
    private UUID            ownerId;
    private final ItemStack item;
    private DisplayItem     displayItem;
    private boolean         unlimited;
    private ShopType        shopType;


    private Boolean         closed;

    private final QuickShop plugin;

    /**
     * Returns a clone of this shop.
     * References to the same display item,
     * itemstack, location and owner as
     * this shop does. Do not modify them or
     * you will modify this shop.
     * 
     * **NOT A DEEP CLONE**
     */
    @Override
    public ContainerShop clone() {
        return new ContainerShop(this);
    }

    private ContainerShop(ContainerShop s) {
        displayItem = s.displayItem;
        shopType = s.shopType;
        item = s.item;
        loc = s.loc;
        plugin = s.plugin;
        unlimited = s.unlimited;
        ownerId = s.ownerId;
        price = s.price;
        closed = s.closed;
    }

    /**
     * Adds a new shop.
     * 
     * @param loc
     *            The location of the chest block
     * @param price
     *            The cost per item
     * @param item
     *            The itemstack with the properties we want. This is .cloned, no
     *            need to worry about references
     * @param owner
     *            The player who owns this shop.
     */
    public ContainerShop(Location loc, double price, ItemStack item, UUID owner) {
        this.loc = loc;
        this.price = price;
        this.ownerId = owner;
        this.item = item.clone();
        plugin = (QuickShop) Bukkit.getPluginManager().getPlugin("QuickShop");
        this.item.setAmount(1);
        if (plugin.display) {
            displayItem = new DisplayItem(this, this.item);
        }
        shopType = ShopType.SELLING;
        this.closed = false;
    }

    /**
     * Returns the number of items this shop has in stock.
     * 
     * @return The number of items available for purchase.
     */
    @Override
    public int getRemainingStock() throws InvalidShopException{
        if (unlimited) {
            return 10000;
        }
        return Util.countItems(getInventory(), getItem());
    }

    /**
     * Returns the number of free spots in the chest for the particular item.
     * 
     * @return and integer showing the remaining spaces
     */
    @Override
    public int getRemainingSpace() throws InvalidShopException {
        if (unlimited) {
            return 10000;
        }
        return Util.countSpace(getInventory(), item);
    }

    /**
     * Returns true if the ItemStack matches what this shop is selling/buying
     * 
     * @param item
     *            The ItemStack
     * @return True if the ItemStack is the same (Excludes amounts)
     */
    @Override
    public boolean matches(ItemStack item) {
        return Util.matches(this.item, item);
    }

    /**
     * Returns the shop that shares it's inventory with this one.
     * 
     * @return the shop that shares it's inventory with this one.
     *         Will return null if this shop is not attached to another.
     */
    public ContainerShop getAttachedShop() {
        final Block c = Util.getSecondHalf(getLocation().getBlock());
        if (c == null) {
            return null;
        }
        final Shop shop = plugin.getShopManager().getShop(c.getLocation());
        return shop == null ? null : (ContainerShop) shop;
    }

    /**
     * Returns true if this shop is a double chest, and the other half is
     * selling/buying the same as this is buying/selling.
     * 
     * @return true if this shop is a double chest, and the other half is
     *         selling/buying the same as this is buying/selling.
     */
    public boolean isDoubleShop() {
        final ContainerShop nextTo = getAttachedShop();
        if (nextTo == null) {
            return false;
        }

        // They're both trading the same item
        try {
            return nextTo.matches(getItem()) && getShopType() != nextTo.getShopType();
        } catch (InvalidShopException e){
            return false;
        }
    }

    /**
     * @return The location of the shops chest
     */
    @Override
    public Location getLocation() {
        return loc;
    }

    /**
     * @return The price per item this shop is selling
     */
    @Override
    public double getPrice() {
        return price;
    }

    /**
     * Sets the price of the shop. Does not update it in the database. Use
     * shop.update() for that.
     * 
     * @param price
     *            The new price of the shop.
     */
    @Override
    public void setPrice(double price) {
        this.price = price;
    }

    /**
     * @return The ItemStack type of this shop
     */
    public Material getMaterial() {
        return item.getType();
    }

    /**
     * Updates the shop into the database.
     */
    @Override
    public void update() {
        final int x = getLocation().getBlockX();
        final int y = getLocation().getBlockY();
        final int z = getLocation().getBlockZ();

        final String world = getLocation().getWorld().getName();
        final int unlimited = isUnlimited() ? 1 : 0;

        final String q = "UPDATE shops SET ownerId = ?, itemConfig = ?, unlimited = ?, type = ?, price = ? WHERE x = ? AND y = ? and z = ? and world = ?";
        try {
            plugin.getDB().execute(q, getOwner().getUniqueId(), Util.serialize(getItem()), unlimited, shopType.toID(), getPrice(), x,
                    y, z, world);
        } catch (final Exception e) {
            e.printStackTrace();
            System.out.println("Could not update shop in database! Changes will revert after a reboot!");
        }
    }

    /**
     * @return The durability of the item
     */
    @Override
    public short getDurability() {
        return item.getDurability();
    }

    /**
     * @return The chest this shop is based on.
     */
    public Inventory getInventory() throws InvalidShopException {
        try {
            final InventoryHolder container = (InventoryHolder) loc.getBlock().getState();
            return container.getInventory();
        } catch (ClassCastException e) {
            InvalidShopException inv
                  =  new InvalidShopException("Shop at :"+ loc.getBlock().getLocation().toString()
                  + " is not an Inventory Holder");
            inv.addSuppressed(e);
            throw inv;
        }
    }

    /**
     * @return The name of the player who owns the shop.
     */
    @Override
    public OfflinePlayer getOwner() {
        return Bukkit.getOfflinePlayer(ownerId);
    }
    
    public UUID getOwnerId() {
        return ownerId;
    }

    /**
     * @return The enchantments the shop has on its items.
     */
    public Map<Enchantment, Integer> getEnchants() {
        return item.getItemMeta().getEnchants();
    }

    /**
     * @return Returns a dummy itemstack of the item this shop is selling.
     */
    @Override
    @NotNull
    public ItemStack  getItem() throws InvalidShopException{
        if(item == null)
            throw new InvalidShopException("Shop Item was null.");
        return item;
    }

    /**
     * Removes an item from the shop.
     * 
     * @param item
     *            The itemstack. The amount does not matter, just everything
     *            else
     * @param amount
     *            The amount to remove from the shop.
     */
    @Override
    public void remove(ItemStack item, int amount) throws InvalidShopException {
        if (unlimited) {
            return;
        }
        final Inventory inv = getInventory();
        if(inv == null)
            throw new InvalidShopException(this.toString());
        int remains = amount;

        while (remains > 0) {
            final int stackSize = Math.min(remains, item.getMaxStackSize());
            item.setAmount(stackSize);
            inv.removeItem(item);
            remains = remains - stackSize;
        }
    }

    /**
     * Add an item to shops chest.
     * 
     * @param item
     *            The itemstack. The amount does not matter, just everything
     *            else
     * @param amount
     *            The amount to add to the shop.
     */
    @Override
    public void add(ItemStack item, int amount) throws InvalidShopException{
        if (unlimited) {
            return;
        }

        final Inventory inv = getInventory();

        int remains = amount;

        while (remains > 0) {
            final int stackSize = Math.min(remains, item.getMaxStackSize());
            item.setAmount(stackSize);
            inv.addItem(item);
            remains = remains - stackSize;
        }
    }

    /**
     * Sells amount of item to Player p. Does NOT check our inventory, or
     * balances
     * 
     * @param p
     *            The player to sell to
     * @param amount
     *            The amount to sell
     */
    @Override
    public void sell(Player p, int amount) throws InvalidShopException {
        if (amount < 0) {
            buy(p, -amount);
        }
        // Items to drop on floor
        final ArrayList<ItemStack> floor = new ArrayList<>(5);
        final Inventory pInv = p.getInventory();
        if (isUnlimited()) {
            final ItemStack item = this.item.clone();

            while (amount > 0) {
                final int stackSize = Math.min(amount, this.item.getMaxStackSize());
                item.setAmount(stackSize);
                pInv.addItem(item);

                amount -= stackSize;
            }
        } else {
            final ItemStack[] chestContents = getInventory().getContents();
            for (int i = 0; amount > 0 && i < chestContents.length; i++) {
                // Can't clone it here, it could be null
                ItemStack item = chestContents[i];

                if (item != null && matches(item)) {
                    // Copy it, we don't want to interfere
                    item = item.clone();

                    // Amount = total, item.getAmount() = how many items in the
                    // stack
                    final int stackSize = Math.min(amount, item.getAmount());

                    // If Amount is item.getAmount(), then this sets the amount
                    // to 0
                    // Else it sets it to the remainder
                    chestContents[i].setAmount(chestContents[i].getAmount() - stackSize);

                    // We can modify this, it is a copy.
                    item.setAmount(stackSize);

                    // Add the items to the players inventory
                    floor.addAll(pInv.addItem(item).values());

                    amount -= stackSize;
                }
            }

            // We now have to update the chests inventory manually.
            getInventory().setContents(chestContents);
        }

        for (ItemStack aFloor : floor) {
            p.getWorld().dropItem(p.getLocation(), aFloor);
        }
    }

    /**
     * Buys amount of item from Player p. Does NOT check our inventory, or
     * balances
     * 
     * @param p
     *            The player to buy from
     * @param amount
     *            The amount to buy
     */
    @Override
    public void buy(Player p, int amount) throws InvalidShopException {
        if (amount < 0) {
            sell(p, -amount);
        }

        if (isUnlimited()) {
            final ItemStack[] contents = p.getInventory().getContents();

            for (int i = 0; amount > 0 && i < contents.length; i++) {
                final ItemStack stack = contents[i];
                if (stack == null) {
                    continue; // No item
                }
                if (matches(stack)) {
                    final int stackSize = Math.min(amount, stack.getAmount());

                    stack.setAmount(stack.getAmount() - stackSize);
                    amount -= stackSize;
                }
            }
            // Send the players new inventory to them
            p.getInventory().setContents(contents);

            // This should not happen.
            if (amount > 0) {
                plugin.getLogger().log(
                        Level.WARNING,
                        "Could not take all items from a players inventory on purchase! " + p.getName() + ", missing: "
                                + amount + ", item: " + getDataName() + "!");
            }
        } else {
            final ItemStack[] playerContents = p.getInventory().getContents();
            final Inventory chestInv = getInventory();

            for (int i = 0; amount > 0 && i < playerContents.length; i++) {
                ItemStack item = playerContents[i];

                if (item != null && matches(item)) {
                    // Copy it, we don't want to interfere
                    item = item.clone();
                    // Amount = total, item.getAmount() = how many items in the
                    // stack
                    final int stackSize = Math.min(amount, item.getAmount());

                    // If Amount is item.getAmount(), then this sets the amount
                    // to 0
                    // Else it sets it to the remainder
                    playerContents[i].setAmount(playerContents[i].getAmount() - stackSize);

                    // We can modify this, it is a copy.
                    item.setAmount(stackSize);

                    // Add the items to the players inventory
                    chestInv.addItem(item);

                    amount -= stackSize;
                }
            }

            // Now update the players inventory.
            p.getInventory().setContents(playerContents);
        }
    }

    /**
     * Changes the owner of this shop to the given player.
     * 
     * @param owner
     *            The name of the owner.
     *            You must do shop.update() after to save it after a reboot.
     */
    @Override
    public void setOwner(OfflinePlayer owner) {
        this.ownerId = owner.getUniqueId();
    }

    /**
     * Returns the display item associated with this shop.
     * 
     * @return The display item associated with this shop.
     */
    public DisplayItem getDisplayItem() {
        return displayItem;
    }

    @Override
    public void setUnlimited(boolean unlimited) {
        this.unlimited = unlimited;
    }

    @Override
    public boolean isUnlimited() {
        return unlimited;
    }

    @Override
    public ShopType getShopType() {
        return shopType;
    }

    @Override
    public boolean isBuying() {
        return shopType == ShopType.BUYING;
    }

    @Override
    public boolean isSelling() {
        return shopType == ShopType.SELLING;
    }

    /**
     * Changes a shop type to Buying or Selling. Also updates the signs nearby.
     * 
     * @param shopType
     *            The new type (ShopType.BUYING or ShopType.SELLING)
     */
    @Override
    public void setShopType(ShopType shopType) {
        this.shopType = shopType;
        this.setSignText();
    }

    /**
     * Updates signs attached to the shop
     */
    @Override
    public void setSignText() {
        if (!Util.isLoaded(getLocation())) {
            return;
        }
        String owner = getOwnerLine();
        final String[] lines = new String[4];
        try {
            if (!closed) {
                lines[0] = owner;
                if (isBuying()) {
                    lines[1] = MsgUtil.getMessage("signs.buying", "" + getRemainingSpace());
                }
                if (isSelling()) {
                    lines[1] = MsgUtil.getMessage("signs.selling", "" + getRemainingStock());
                }
                lines[2] = Util.getName(item);
                lines[3] = MsgUtil.getMessage("signs.price", "" + getPrice());
            } else {
                lines[0] = "Shop is Closed";
                this.setSignText(lines);
            }
        }catch (InvalidShopException e){
            lines[0] = "Invalid Shop";
        }
        this.setSignText(lines);
    }

    /**
     * Returns the line for the owner text
     * 
     * @return the line for the owner text
     */
    private String getOwnerLine() {
        return ChatColor.DARK_RED + getOwnerName();
    }

    /**
     * Returns the shop owner
     *
     * @return the name of the shop owner or "Uknown" if the player doesnt exist
     */
    public String getOwnerName() {
        String shopOwner = getOwner().getName();
        if (shopOwner == null || shopOwner.isEmpty()) {
            // we cannot retrieve the shop owner which means they have never visited this server
            //no Player.dat exists. Disable the shop (but dont delete it)
            setClosed(true);
            shopOwner = "Unknown";
        }else{
            setClosed(false);
        }
        return shopOwner;
    }

    /**
     * Changes all lines of text on a sign near the shop
     * 
     * @param lines
     *            The array of lines to change. Index is line number.
     */
    @Override
    public void setSignText(String[] lines) {
        if (!Util.isLoaded(getLocation())) {
            return;
        }

        for (final Sign sign: getSigns()) {
            for (int i = 0; i < lines.length; i++) {
                sign.setLine(i, lines[i]);
            }

            sign.update();
        }
    }

    /**
     * Returns a list of signs that are attached to this shop (QuickShop and
     * blank signs only)
     * 
     * @return a list of signs that are attached to this shop (QuickShop and
     *         blank signs only)
     */
    @Override
    public List<Sign> getSigns() {
        final ArrayList<Sign> signs = new ArrayList<>(1);

        if (getLocation().getWorld() == null) {
            return signs;
        }

        final Block[] blocks = new Block[4];
        blocks[0] = loc.getBlock().getRelative(1, 0, 0);
        blocks[1] = loc.getBlock().getRelative(-1, 0, 0);
        blocks[2] = loc.getBlock().getRelative(0, 0, 1);
        blocks[3] = loc.getBlock().getRelative(0, 0, -1);

        for (final Block b: blocks) {
            if (b.getType() != Material.OAK_WALL_SIGN) {
                continue;
            }
            if (!isAttached(b)) {
                continue;
            }
            final Sign sign = (Sign) b.getState();

            if (sign.getLine(0).contains("[QuickShop") || sign.getLine(0).startsWith(ChatColor.DARK_RED + "")) {
                signs.add(sign);
            } else {
                boolean text = false;
                for (final String s: sign.getLines()) {
                    if (!s.isEmpty()) {
                        text = true;
                        break;
                    }
                }

                if (!text) {
                    signs.add(sign);
                }
            }
        }
        return signs;
    }

    @Override
    public boolean isAttached(Block b) {
        if (b.getType() != Material.OAK_WALL_SIGN) {
            new IllegalArgumentException(b + " Is not a sign!").printStackTrace();
        }
        return getLocation().getBlock().equals(Util.getAttached(b));
    }

    /**
     * Convenience method. Equivilant to
     * org.maxgamer.QuickShop.Util.getName(shop.getItem()).
     * 
     * @return The name of this shops item
     */
    @Override
    public String getDataName() {
        try {
            return Util.getName(getItem());
        } catch (InvalidShopException e) {
            QuickShop.instance.log(e.getMessage());
            return "ERROR ITEM INVALID";
        }
    }

    /**
     * Deletes the shop from the list of shops
     * and queues it for database deletion
     * *DOES* delete it from memory
     */
    @Override
    public void delete() {
        delete(true);
    }

    /**
     * Deletes the shop from the list of shops
     * and queues it for database deletion
     * 
     * @param fromMemory
     *            True if you are *NOT* iterating over this currently, *false if
     *            you are iterating*
     */
    @Override
    public void delete(boolean fromMemory) {
        // Delete the display item

        if (getDisplayItem() != null) {
            getDisplayItem().remove();
        }

        // Delete the signs around it
        for (final Sign s: getSigns()) {
            s.getBlock().setType(Material.AIR);
        }

        // Delete it from the database
        final int x = getLocation().getBlockX();
        final int y = getLocation().getBlockY();
        final int z = getLocation().getBlockZ();
        final String world = getLocation().getWorld().getName();
        plugin.getDB().execute(
                "DELETE FROM shops WHERE x = '" + x + "' AND y = '" + y + "' AND z = '" + z + "' AND world = '" + world
                        + "'");

        // Refund if necessary
        if (plugin.getConfig().getBoolean("shop.refund")) {
            plugin.getEcon().deposit(getOwner(), plugin.getConfig().getDouble("shop.cost"));
        }

        if (fromMemory) {
            // Delete it from memory
            plugin.getShopManager().removeShop(this);
        }
    }

    @Override
    public boolean isValid() {
        checkDisplay();

        return Util.canBeShop(getLocation().getBlock());
    }

    @Override
    public boolean isClosed() {

        return closed;
    }

    public void setClosed(Boolean closed) {
        this.closed = closed;
    }

    private void checkDisplay() {
        if (plugin.display == false) {
            return;
        }
        if (getLocation().getWorld() == null) {
            return; // not loaded
        }
        final boolean trans = getLocation().clone().add(0.5, 1.2, 0.5).getBlock().getType().isTransparent();
        if (trans && getDisplayItem() == null) {
            try {
                displayItem = new DisplayItem(this, getItem());
                getDisplayItem().spawn();
            }catch (InvalidShopException e){
                QuickShop.instance.log(e.getMessage());
            }
        }

        if (getDisplayItem() != null) {
            if (!trans) { // We have a display item in a block... delete it
                getDisplayItem().remove();
                displayItem = null;
                return;
            }

            final DisplayItem disItem = getDisplayItem();
            final Location dispLoc = disItem.getDisplayLocation();

            if (dispLoc.getBlock() != null && dispLoc.getBlock().getType() == Material.WATER) { // Flowing
                                                                                                // water.
                                                                                                // Stationery
                                                                                                // water
                                                                                                // does
                                                                                                // not
                                                                                                // move
                                                                                                // items.
                disItem.remove();
                return;
            }
            if (disItem.getItem() == null) {
                disItem.spawn();
                disItem.removeDupe();
                return;
            }

            final Item item = disItem.getItem();

            if (item.getTicksLived() > 5000 || !item.isValid() || item.isDead()) {
                disItem.respawn();
                disItem.removeDupe();
            } else if (item.getLocation().distanceSquared(dispLoc) > 1) {
                item.teleport(dispLoc, TeleportCause.PLUGIN);
            }
            if(isClosed()){
                disItem.remove();
            }
        }
    }

    @Override
    public void onUnload() {
        if (getDisplayItem() != null) {
            getDisplayItem().remove();
            displayItem = null;
        }
    }

    @Override
    public void onLoad() {
        checkDisplay();
    }

    @Override
    public void onClick() {
        this.setSignText();
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("Shop "
                + (loc.getWorld() == null ? "unloaded world" : loc.getWorld().getName()) + "(" + loc.getBlockX() + ", "
                + loc.getBlockY() + ", " + loc.getBlockZ() + ")");
        sb.append(" Owner: ").append(getOwner());
        if (isUnlimited()) {
            sb.append(" Unlimited: true");
        }
        sb.append(" Price: ").append(getPrice());
        sb.append("Item: ");
        try {
            sb.append(getItem().toString());
        }catch (InvalidShopException e) {
            QuickShop.instance.log(e.getMessage());
            sb.append("ERROR");
        }
        return sb.toString();
    }
}