package org.maxgamer.QuickShop.Shop;

import java.util.List;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.maxgamer.QuickShop.exceptions.InvalidShopException;

public interface Shop {
    /**
     * Returns a clone of this shop.
     * References to the same display item,
     * itemstack, location and owner as
     * this shop does. Do not modify them or
     * you will modify this shop.
     * 
     * **NOT A DEEP CLONE**
     * @return Shop
     */
    Shop clone();

    /**
     * Returns the number of items this shop has in stock.
     * 
     * @return The number of items available for purchase.
     */
    int getRemainingStock() throws InvalidShopException;

    /**
     * Returns the number of free spots in the chest for the particular item.
     * 
     * @return remaining spaces
     */
    int getRemainingSpace() throws InvalidShopException;

    /**
     * Returns true if the ItemStack matches what this shop is selling/buying
     * 
     * @param item
     *            The ItemStack
     * @return True if the ItemStack is the same (Excludes amounts)
     */
    boolean matches(ItemStack item);

    /**
     * @return The location of the shops chest
     */
    Location getLocation();

    /**
     * @return The price per item this shop is selling
     */
    double getPrice();

    /**
     * Sets the price of the shop. Does not update it in the database. Use
     * shop.update() for that.
     * 
     * @param price
     *            The new price of the shop.
     */
    void setPrice(double price);

    /**
     * Upates the shop into the database.
     */
    void update();

    /**
     * @return The durability of the item
     */
    short getDurability();

    /**
     * @return The player who owns the shop.
     */
    OfflinePlayer getOwner();
    
    /**
     * @return Returns a dummy itemstack of the item this shop is selling.
     */
    ItemStack getItem();

    /**
     * Removes an item from the shop.
     * 
     * @param item
     *            The itemstack. The amount does not matter, just everything
     *            else
     * @param amount
     *            The amount to remove from the shop.
     */
    void remove(ItemStack item, int amount) throws InvalidShopException;

    /**
     * Add an item to shops chest.
     * 
     * @param item
     *            The itemstack. The amount does not matter, just everything
     *            else
     * @param amount
     *            The amount to add to the shop.
     */
    void add(ItemStack item, int amount) throws InvalidShopException;

    /**
     * Sells amount of item to Player p. Does NOT check our inventory, or
     * balances
     * 
     * @param p
     *            The player to sell to
     * @param amount
     *            The amount to sell
     */
    void sell(Player p, int amount) throws InvalidShopException;

    /**
     * Buys amount of item from Player p. Does NOT check our inventory, or
     * balances
     * 
     * @param p
     *            The player to buy from
     * @param amount
     *            The amount to buy
     */
    void buy(Player p, int amount) throws InvalidShopException;

    /**
     * Changes the owner of this shop to the given player.
     * 
     * @param owner
     * 			  The new owner
     *            You must do shop.update() after to save it after a reboot.
     */
    void setOwner(OfflinePlayer owner);

    void setUnlimited(boolean unlimited);

    boolean isUnlimited();

    ShopType getShopType();

    boolean isBuying();

    boolean isSelling();

    /**
     * Changes a shop type to Buying or Selling. Also updates the signs nearby.
     * 
     * @param shopType
     *            The new type (ShopType.BUYING or ShopType.SELLING)
     */
    void setShopType(ShopType shopType);

    /**
     * Updates signs attached to the shop
     */
    void setSignText();

    /**
     * Changes all lines of text on a sign near the shop
     * 
     * @param lines
     *            The array of lines to change. Index is line number.
     */
    void setSignText(String[] lines);

    /**
     * Returns a list of signs that are attached to this shop (QuickShop and
     * blank signs only)
     * 
     * @return a list of signs that are attached to this shop (QuickShop and
     *         blank signs only)
     */
    List<Sign> getSigns();

    boolean isAttached(Block b);

    /**
     * Convenience method. Equivilant to
     * org.maxgamer.QuickShop.Util.getName(shop.getItem()).
     * 
     * @return The name of this shops item
     */
    String getDataName();

    /**
     * Deletes the shop from the list of shops
     * and queues it for database deletion
     * *DOES* delete it from memory
     */
    void delete();

    /**
     * Deletes the shop from the list of shops
     * and queues it for database deletion
     * 
     * @param fromMemory
     *            True if you are *NOT* iterating over this currently, *false if
     *            you are iterating*
     */
    void delete(boolean fromMemory);

    /**
     * Should return true if this shop is valid.
     * Should return false if it is not - Such as, a ChestShop should be
     * situated on a chest.
     * 
     * This method is called periodically. Here, you should check:
     * * The block this is on has not changed (E.g. WorldEdit does not throw
     * block events)
     * * The display item (if any) is still valid etc
     * * And anything else that has to be brute force checked periodically.
     *
     * You can safely assume that this shop's world is loaded during this
     * method.
     * 
     * @return true if shop is valid
     */
    boolean isValid();

    /**
     * If a shop is closed then this will return true.
     *
     * @return boolean @{code true} if closed
     */

    boolean isClosed();

    /**
     * This method is called whenever the shop should be unloaded.
     * E.g. for chest shops, they should clean up their own display items.
     * This method is called when the chunk the shop is stored in is unloaded.
     *
     * This should not remove the shop from memory (That is done by the caller,
     * if at all).
     */

    void onUnload();

    /**
     * This method is called whenever the shop is loaded.
     * Such as when it is first created, or when the chunk
     * it is in is loaded from disk.
     */
    void onLoad();

    /**
     * This method is called whenever a player clicks
     * on the shop. For example, a player goes to purchase
     * from the shop. Only called when a player has permission
     * to open the shop. Does not get called for right click.
     */
    void onClick();
}