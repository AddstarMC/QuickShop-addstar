package org.maxgamer.QuickShop.Shop;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.inventory.ItemStack;
import org.maxgamer.QuickShop.QuickShop;
import org.maxgamer.QuickShop.exceptions.InvalidShopException;

public class Info {
    private final Location loc;
    private ShopAction     action;
    private ItemStack      item;
    private final Block    last;
    private Shop           shop;

    /**
     * Stores info for the players last shop interact.
     * 
     * @param loc
     *            The location they clicked (Block.getLocation())
     * @param action
     *            The action (ShopAction.*)
     * @param item the itemstack
     * @param last the block
     *            The block of the material
     */
    public Info(Location loc, ShopAction action, ItemStack item, Block last) {
        this.loc = loc;
        this.action = action;
        this.last = last;
        if (item != null) {
            this.item = item.clone();
        }
    }

    /**
     * Stores info for the players last shop interact.
     * 
     * @param loc
     *            The location they clicked (Block.getLocation())
     * @param action
     *            The action (ShopAction.*)
     * @param item
     *            The item they were holding
     * @param last
     *            The block of material
     * @param shop
     *            The shop they interacted with, or null if none
     */
    public Info(Location loc, ShopAction action, ItemStack item, Block last, Shop shop) {
        this.loc = loc;
        this.action = action;
        this.last = last;
        if (item != null) {
            this.item = item.clone();
        }

        if (shop != null) {
            this.shop = shop.clone();
        }
    }

    public boolean hasChanged(Shop shop) {
        if (this.shop.isUnlimited() != shop.isUnlimited()) {
            return true;
        }
        if (this.shop.getShopType() != shop.getShopType()) {
            return true;
        }
        if (!this.shop.getOwner().equals(shop.getOwner())) {
            return true;
        }
        try {
            return this.shop.getPrice() != shop.getPrice() || !this.shop.getLocation().equals(shop.getLocation()) || !this.shop.matches(shop.getItem());
        }catch (InvalidShopException e){
            QuickShop.instance.log(e.getMessage());
            return true;
        }

    }

    public ShopAction getAction() {
        return action;
    }

    public Location getLocation() {
        return loc;
    }

    /*
     * public Material getMaterial(){
     * return this.item.getType();
     * }
     * public byte getData(){
     * return this.getData();
     * }
     */
    public ItemStack getItem() {
        return item;
    }

    public void setAction(ShopAction action) {
        this.action = action;
    }

    public Block getSignBlock() {
        return last;
    }
}