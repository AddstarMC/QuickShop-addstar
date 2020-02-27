package org.maxgamer.QuickShop.Shop;

import java.util.HashMap;
import java.util.Iterator;
import java.util.NoSuchElementException;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockDamageEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.material.Attachable;
import org.bukkit.material.Directional;
import org.bukkit.block.data.type.WallSign;
import org.bukkit.metadata.MetadataValue;
import org.maxgamer.QuickShop.QuickShop;
import org.maxgamer.QuickShop.Database.Database;
import org.maxgamer.QuickShop.Util.MsgUtil;
import org.maxgamer.QuickShop.Util.Util;

import org.jetbrains.annotations.Nullable;
import org.maxgamer.QuickShop.exceptions.InvalidShopException;

public class ShopManager {
    private final QuickShop                                                    plugin;
    private final HashMap<String, Info>                                        actions        = new HashMap<>(
            30);

    private final HashMap<String, HashMap<ShopChunk, HashMap<Location, Shop>>> shops          = new HashMap<>(
            3);

    public ShopManager(QuickShop plugin) {
        this.plugin = plugin;
    }

    public Database getDatabase() {
        return plugin.getDB();
    }

    /**
     * @return Returns the {@code HashMap<Player name, shopInfo>}. Info contains what
     *         their last question etc was.
     */
    public HashMap<String, Info> getActions() {
        return actions;
    }

    public void createShop(Shop shop) {
        final Location loc = shop.getLocation();
        final ItemStack item;
        try {
            item = shop.getItem();
        } catch (InvalidShopException e) {
            QuickShop.instance.log(e.getMessage());
            return;
        }
        try {
            // Write it to the database
            final String q = "INSERT INTO shops (ownerId, price, itemConfig, x, y, z, world, unlimited, type) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
            plugin.getDB().execute(q, shop.getOwner().getUniqueId().toString(), shop.getPrice(), Util.serialize(item), loc.getBlockX(),
                    loc.getBlockY(), loc.getBlockZ(), loc.getWorld().getName(), (shop.isUnlimited() ? 1 : 0),
                    shop.getShopType().toID());

            // Add it to the world
            addShop(loc.getWorld().getName(), shop);
        } catch (final Exception e) {
            e.printStackTrace();
            System.out.println("Could not create shop! Changes will revert after a reboot!");
        }
    }

    /**
     * Loads the given shop into storage. This method is used for loading data
     * from the database.
     * Do not use this method to create a shop.
     * 
     * @param world
     *            The world the shop is in
     * @param shop
     *            The shop to load
     */
    public void loadShop(String world, Shop shop) {
        addShop(world, shop);
    }

    /**
     * Returns a hashmap of World and  Chunk  and Shop
     * 
     * @return a hashmap of World and Chunk and Shop can be @{code Null}
     */
    public HashMap<String, HashMap<ShopChunk, HashMap<Location, Shop>>> getShops() {
        return shops;
    }

    /**
     * Returns a hashmap of Chunk and Shop
     * 
     * @param world
     *            The name of the world (case sensitive) to get the list of
     *            shops from
     * @return a hashmap of Chunk  and  Shop can be @{code Null}
     */
    @Nullable
    public HashMap<ShopChunk, HashMap<Location, Shop>> getShops(String world) {
        return shops.get(world);
    }

    /**
     * Returns a hashmap of Shops
     * 
     * @param c
     *            The chunk to search. Referencing doesn't matter, only
     *            coordinates and world are used.
     * @return a Hashmap of Location and Shops can be @{code NULL}
     */
    public HashMap<Location, Shop> getShops(Chunk c) {
        // long start = System.nanoTime();
        final HashMap<Location, Shop> shops = getShops(c.getWorld().getName(), c.getX(), c.getZ());
        // long end = System.nanoTime();
        // System.out.println("Chunk lookup in " + ((end - start)/1000000.0) +
        // "ms.");
        return shops;
    }

    /**
     *
     * @param world World
     * @param chunkX x coord
     * @param chunkZ y coord
     * @return a Hashmap of Location and Shops can be @{code Null}
     */
    public HashMap<Location, Shop> getShops(String world, int chunkX, int chunkZ) {
        final HashMap<ShopChunk, HashMap<Location, Shop>> inWorld = this.getShops(world);

        if (inWorld == null) {
            return null;
        }

        final ShopChunk shopChunk = new ShopChunk(world, chunkX, chunkZ);
        return inWorld.get(shopChunk);
    }

    /**
     * Gets a shop in a specific location
     * 
     * @param loc
     *            The location to get the shop from
     * @return The shop at that location
     */
    public Shop getShop(Location loc) {
        final HashMap<Location, Shop> inChunk = getShops(loc.getChunk());
        if (inChunk == null) {
            return null;
        }
        // We can do this because WorldListener updates the world reference so
        // the world in loc is the same as world in inChunk.get(loc)
        return inChunk.get(loc);
    }

    /**
     * Adds a shop to the world. Does NOT require the chunk or world to be
     * loaded
     * 
     * @param world
     *            The name of the world
     * @param shop
     *            The shop to add
     */
    private void addShop(String world, Shop shop) {
        HashMap<ShopChunk, HashMap<Location, Shop>> inWorld = this.getShops().computeIfAbsent(world, k -> new HashMap<>(3));

        // There's no world storage yet. We need to create that hashmap.
        // Put it in the data universe

        // Calculate the chunks coordinates. These are 1,2,3 for each chunk, NOT
        // location rounded to the nearest 16.
        final int x = (int) Math.floor((shop.getLocation().getBlockX()) / 16.0);
        final int z = (int) Math.floor((shop.getLocation().getBlockZ()) / 16.0);

        // Get the chunk set from the world info
        final ShopChunk shopChunk = new ShopChunk(world, x, z);
        HashMap<Location, Shop> inChunk = inWorld.computeIfAbsent(shopChunk, k -> new HashMap<>(1));

        // That chunk data hasn't been created yet - Create it!
        // Put it in the world

        // Put the shop in its location in the chunk list.
        inChunk.put(shop.getLocation(), shop);
    }

    /**
     * Removes a shop from the world. Does NOT remove it from the database.
     * * REQUIRES * the world to be loaded
     * 
     * @param shop
     *            The shop to remove
     */
    public void removeShop(Shop shop) {
        final Location loc = shop.getLocation();
        final String world = loc.getWorld().getName();
        final HashMap<ShopChunk, HashMap<Location, Shop>> inWorld = this.getShops().get(world);

        final int x = (int) Math.floor((shop.getLocation().getBlockX()) / 16.0);
        final int z = (int) Math.floor((shop.getLocation().getBlockZ()) / 16.0);

        final ShopChunk shopChunk = new ShopChunk(world, x, z);
        final HashMap<Location, Shop> inChunk = inWorld.get(shopChunk);

        inChunk.remove(loc);
    }

    /**
     * Removes all shops from memory and the world. Does not delete them from
     * the database.
     * Call this on plugin disable ONLY.
     */
    public void clear() {
        if (plugin.display) {
            for (final World world: Bukkit.getWorlds()) {
                for (final Chunk chunk: world.getLoadedChunks()) {
                    final HashMap<Location, Shop> inChunk = this.getShops(chunk);
                    if (inChunk == null) {
                        continue;
                    }
                    for (final Shop shop: inChunk.values()) {
                        shop.onUnload();
                    }
                }
            }
        }
        actions.clear();
        shops.clear();
    }

    /**
     * Checks other plugins to make sure they can use the chest they're making a
     * shop.
     * 
     * @param p
     *            The player to check
     * @param b
     *           The block to check
     * @param bf
     *           the blockface to check
     * @return True if they're allowed to place a shop there.
     */
    public boolean canBuildShop(Player p, Block b, BlockFace bf) {
        if (plugin.limit) {
            int owned = 0;
            final Iterator<Shop> it = getShopIterator();
            while (it.hasNext()) {
                if (p.equals(it.next().getOwner().getPlayer())) {
                    owned++;
                }
            }

            final int max = plugin.getShopLimit(p);
            if (owned + 1 > max) {
                p.sendMessage(ChatColor.RED + "You have already created a maximum of " + owned + "/" + max + " shops!");
                return false;
            }
        }

        final PlayerInteractEvent pie = new PlayerInteractEvent(p, Action.RIGHT_CLICK_BLOCK,
                new ItemStack(Material.AIR), b, bf); // PIE =
                                                     // PlayerInteractEvent -
                                                     // What else?
        Bukkit.getPluginManager().callEvent(pie);
        pie.getPlayer().closeInventory(); // If the player has chat open, this
                                          // will close their chat.

        if (pie.isCancelled()) {
            return false;
        }

        final ShopPreCreateEvent spce = new ShopPreCreateEvent(p, b.getLocation());
        Bukkit.getPluginManager().callEvent(spce);
        return !spce.isCancelled();

    }

    public void handleChat(final Player p, String msg) {
        final String message = ChatColor.stripColor(msg);

        // Use from the main thread, because Bukkit hates life
        Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, () -> {
            final HashMap<String, Info> actions = getActions();
            // They wanted to do something.
            final Info info = actions.remove(p.getName());
            if (info == null) {
                QuickShop.debugMsg("action removed info was null.....");
                return; // multithreaded means this can happen
            }
            if (info.getLocation().getWorld() != p.getLocation().getWorld()) {
                p.sendMessage(MsgUtil.getMessage("shop-creation-cancelled"));
                return;
            }
            if (info.getLocation().distanceSquared(p.getLocation()) > 25) {
                p.sendMessage(MsgUtil.getMessage("shop-creation-cancelled"));
                return;
            }

            /* Creation handling */
            if (info.getAction() == ShopAction.CREATE) {
                try {
                    // Checking the shop can be created
                    if (plugin.getShopManager().getShop(info.getLocation()) != null) {
                        p.sendMessage(MsgUtil.getMessage("shop-already-owned"));
                        return;
                    }

                    if (Util.getSecondHalf(info.getLocation().getBlock()) != null
                            && !p.hasPermission("quickshop.create.double")) {
                        p.sendMessage(MsgUtil.getMessage("no-double-chests"));
                        return;
                    }

                    if (!Util.canBeShop(info.getLocation().getBlock())) {
                        p.sendMessage(MsgUtil.getMessage("chest-was-removed"));
                        return;
                    }

                    // Price per item
                    double price;
                    if (plugin.getConfig().getBoolean("whole-number-prices-only")) {
                        price = Integer.parseInt(message);
                    } else {
                        price = Double.parseDouble(message);
                    }
                    if (price < 0.01) {
                        p.sendMessage(MsgUtil.getMessage("price-too-cheap"));
                        return;
                    }
                    final double tax = plugin.getConfig().getDouble("shop.cost");

                    // Tax refers to the cost to create a shop. Not actual
                    // tax, that would be silly
                    if (tax > 0 && plugin.getEcon().getBalance(p) < tax) {
                        p.sendMessage(MsgUtil.getMessage("you-cant-afford-a-new-shop", format(tax)));
                        return;
                    }

                    // Create the sample shop.
                    final Shop shop = new ContainerShop(info.getLocation(), price, info.getItem(), p.getUniqueId());
                    shop.onLoad();

                    final ShopCreateEvent e = new ShopCreateEvent(shop, p);
                    Bukkit.getPluginManager().callEvent(e);
                    if (e.isCancelled()) {
                        shop.onUnload();
                        return;
                    }

                    // This must be called after the event has been called.
                    // Else, if the event is cancelled, they won't get their
                    // money back.
                    if (tax > 0) {
                        if (!plugin.getEcon().withdraw(p, tax)) {
                            p.sendMessage(MsgUtil.getMessage("you-cant-afford-a-new-shop", format(tax)));
                            shop.onUnload();
                            return;
                        }

                        if (plugin.getTaxAccount().hasPlayedBefore()) {
                            plugin.getEcon().deposit(plugin.getTaxAccount(), tax);
                        }
                    }

                    /* The shop has hereforth been successfully created */
                    createShop(shop);

                    final Location loc = shop.getLocation();
                    plugin.log(p.getName() + " created a " + shop.getDataName() + " shop at ("
                            + loc.getWorld().getName() + " - " + loc.getX() + "," + loc.getY() + "," + loc.getZ()
                            + ")");

                    if (!plugin.getConfig().getBoolean("shop.lock")) {
                        // Warn them if they haven't been warned since
                        // reboot
                        if (!plugin.warnings.contains(p.getName())) {
                            p.sendMessage(MsgUtil.getMessage("shops-arent-locked"));
                            plugin.warnings.add(p.getName());
                        }
                    }

                    // Figures out which way we should put the sign on and
                    // sets its text.
                    Block signblock = info.getSignBlock();
                    Block shopBlock = info.getLocation().getBlock();
                    if (signblock != null && signblock.getType() == Material.AIR
                            && plugin.getConfig().getBoolean("shop.auto-sign")) {
                        BlockState signBlockState = signblock.getState();
                        BlockFace bf = shopBlock.getFace(info.getSignBlock());
                        if(bf == null){ //signBlock isn't connected???!!!
                            if(shopBlock.getBlockData() instanceof Directional){
                                Directional shopdata = (Directional) shopBlock.getBlockData();
                                bf = shopdata.getFacing();
                                signblock = shopBlock.getRelative(bf);
                                signBlockState = signblock.getState();
                            }
                        }
                        signBlockState.setType(Material.OAK_WALL_SIGN);
                        WallSign signBlockDataType = (WallSign) signBlockState.getBlockData();
                        signBlockDataType.setFacing(bf);
                        signBlockState.update(true);
                        shop.setSignText();
                    }
    
                    final ContainerShop cs = (ContainerShop) shop;
                    if (cs.isDoubleShop()) {
                        final Shop nextTo = cs.getAttachedShop();

                        if (nextTo.getPrice() > shop.getPrice()) {
                            // The one next to it must always be a
                            // buying shop.
                            p.sendMessage(MsgUtil.getMessage("buying-more-than-selling"));
                        }
                    }
                }
                /* They didn't enter a number. */
                catch (final NumberFormatException ex) {
                    p.sendMessage(MsgUtil.getMessage("shop-creation-cancelled"));
                }
            }
            /* Purchase Handling */
            else if (info.getAction() == ShopAction.BUY) {
                int amount;
                try {
                    amount = Integer.parseInt(message);
                } catch (final NumberFormatException e) {
                    p.sendMessage(MsgUtil.getMessage("shop-purchase-cancelled"));
                    return;
                }

                // Get the shop they interacted with
                final Shop shop = plugin.getShopManager().getShop(info.getLocation());

                // It's not valid anymore
                if (shop == null || !Util.canBeShop(info.getLocation().getBlock())) {
                    p.sendMessage(MsgUtil.getMessage("chest-was-removed"));
                    return;
                }
                if(shop.isClosed()){
                    p.sendMessage(MsgUtil.getMessage("shop-is-closed"));
                    return;
                }
                if (info.hasChanged(shop)) {
                    p.sendMessage(MsgUtil.getMessage("shop-has-changed"));
                    return;
                }

                if (shop.isSelling()) {
                    final int stock;
                    final ItemStack itemSold;
                    try {
                        stock = shop.getRemainingStock();
                        itemSold = shop.getItem();
                    } catch (InvalidShopException e){
                        p.sendMessage(MsgUtil.getMessage("shop-is-invalid"));
                        QuickShop.instance.log(e.getMessage());
                        return;
                    }
                    if (stock < amount) {
                            p.sendMessage(MsgUtil.getMessage("shop-stock-too-low", ""
                                        + stock, shop.getDataName()));
                        return;
                    }

                    if (validatePurchase(p, amount, shop)) return;
    
                    final int pSpace = Util.countSpace(p.getInventory(), itemSold);
                    if (amount > pSpace) {
                        p.sendMessage(MsgUtil.getMessage("not-enough-space", "" + pSpace));
                        return;
                    }

                    final ShopPurchaseEvent e = new ShopPurchaseEvent(shop, p, amount);
                    Bukkit.getPluginManager().callEvent(e);
                    if (e.isCancelled()) {
                        return; // Cancelled
                    }

                    // Money handling
                    if (true) {
                        // Check their balance. Works with *most* economy
                        // plugins*
                        if (plugin.getEcon().getBalance(p) < amount * shop.getPrice()) {
                            p.sendMessage(MsgUtil.getMessage("you-cant-afford-to-buy",
                                    format(amount * shop.getPrice()),
                                    format(plugin.getEcon().getBalance(p))));
                            return;
                        }

                        // Don't tax them if they're purchasing from
                        // themselves.
                        // Do charge an amount of tax though.
                        final double tax = plugin.getConfig().getDouble("tax");
                        final double total = amount * shop.getPrice();

                        // Check if player has enough to pay for it
                        if (plugin.getEcon().getBalance(p) <= total) {
                            p.sendMessage(MsgUtil.getMessage("you-cant-afford-to-buy",
                                    format(amount * shop.getPrice()),
                                    format(plugin.getEcon().getBalance(p))));
                            return;
                        }

                        // Attempt to give money to the shop owner
                        if (!shop.isUnlimited() || plugin.getConfig().getBoolean("shop.pay-unlimited-shop-owners")) {
                            try {
                                plugin.getEcon().deposit(shop.getOwner(), total * (1 - tax));
                            } catch (Exception ex) {
                                p.sendMessage(ChatColor.RED + "Error: Unable to purchase from this shop!");
                                plugin.getLogger().warning("Unable to complete purchase from QuickShop owned by " + shop.getOwner().getName());
                                ex.printStackTrace();
                                return;
                            }

                            if (tax != 0 && plugin.getTaxAccount().hasPlayedBefore()) {
                                plugin.getEcon().deposit(plugin.getTaxAccount(), total * tax);
                            }
                        }

                        // Withdraw the money from the purchaser
                        plugin.getEcon().withdraw(p, total);

                        // Notify the shop owner
                        if (plugin.getConfig().getBoolean("show-tax")) {
                            String msg1 = MsgUtil.getMessage("player-bought-from-your-store-tax", p.getName(), ""
                                    + amount, shop.getDataName(), Util.format((tax * total)));
                            if (stock == amount) {
                                msg1 += "\n"
                                        + MsgUtil.getMessage("shop-out-of-stock", ""
                                                + shop.getLocation().getBlockX(), ""
                                                + shop.getLocation().getBlockY(), ""
                                                + shop.getLocation().getBlockZ(), shop.getDataName());
                            }
                            MsgUtil.send(shop.getOwner(), msg1);
                        } else {
                            String msg1 = MsgUtil.getMessage("player-bought-from-your-store", p.getName(), ""
                                    + amount, shop.getDataName());
                            if (stock == amount) {
                                msg1 += "\n"
                                        + MsgUtil.getMessage("shop-out-of-stock", ""
                                                + shop.getLocation().getBlockX(), ""
                                                + shop.getLocation().getBlockY(), ""
                                                + shop.getLocation().getBlockZ(), shop.getDataName());
                            }
                            MsgUtil.send(shop.getOwner(), msg1);
                        }

                    }
                    // Transfers the item from A to B
                    try {
                        shop.sell(p, amount);
                    } catch (InvalidShopException inv) {
                        p.sendMessage(MsgUtil.getMessage("shop-is-invalid"));
                        QuickShop.instance.log(inv.getMessage());
                        return;
                    }
                    MsgUtil.sendPurchaseSuccess(p, shop, amount);
                    plugin.log(p.getName() + " bought " + amount + " for " + (shop.getPrice() * amount) + " from "
                            + shop.toString());
                } else if (shop.isBuying()) {
                    final int space;
                    final ItemStack itemSold;
                    try {
                         space = shop.getRemainingSpace();
                         itemSold = shop.getItem();
                    }catch (InvalidShopException e) {
                        p.sendMessage(MsgUtil.getMessage("shop-is-invalid"));
                        QuickShop.instance.log(e.getMessage());
                        return;
                    }
                    if (space < amount) {
                        p.sendMessage(MsgUtil.getMessage("shop-has-no-space", "" + space, shop.getDataName()));
                        return;
                    }

                    final int count = Util.countItems(p.getInventory(), itemSold);

                    // Not enough items
                    if (amount > count) {
                        p.sendMessage(MsgUtil.getMessage("you-dont-have-that-many-items", "" + count,
                                shop.getDataName()));
                        return;
                    }
    
                    if (validatePurchase(p, amount, shop)) return;
    
                    // Money handling
                    if (!p.equals(shop.getOwner().getPlayer())) {
                        // Don't tax them if they're purchasing from
                        // themselves.
                        // Do charge an amount of tax though.
                        final double tax = plugin.getConfig().getDouble("tax");
                        final double total = amount * shop.getPrice();

                        if (!shop.isUnlimited() || plugin.getConfig().getBoolean("shop.pay-unlimited-shop-owners")) {
                            // Tries to check their balance nicely to see if
                            // they can afford it.
                            if (plugin.getEcon().getBalance(shop.getOwner()) < amount * shop.getPrice()) {
                                p.sendMessage(MsgUtil.getMessage("the-owner-cant-afford-to-buy-from-you",
                                        format(amount * shop.getPrice()),
                                        format(plugin.getEcon().getBalance(shop.getOwner()))));
                                return;
                            }

                            // Check for plugins faking econ.has(amount)
                            if (!plugin.getEcon().withdraw(shop.getOwner(), total)) {
                                p.sendMessage(MsgUtil.getMessage("the-owner-cant-afford-to-buy-from-you",
                                        format(amount * shop.getPrice()),
                                        format(plugin.getEcon().getBalance(shop.getOwner()))));
                                return;
                            }

                            if (tax != 0 && plugin.getTaxAccount().hasPlayedBefore()) {
                                plugin.getEcon().deposit(plugin.getTaxAccount(), total * tax);
                            }
                        }
                        // Give them the money after we know we succeeded
                        plugin.getEcon().deposit(p, total * (1 - tax));

                        // Notify the owner of the purchase.
                        String msg1 = MsgUtil.getMessage("player-sold-to-your-store", p.getName(), "" + amount,
                                shop.getDataName());
                        if (space == amount) {
                            msg1 += "\n"
                                    + MsgUtil.getMessage("shop-out-of-space", "" + shop.getLocation().getBlockX(),
                                            "" + shop.getLocation().getBlockY(), ""
                                                    + shop.getLocation().getBlockZ());
                        }

                        MsgUtil.send(shop.getOwner(), msg1);
                    }
                    try {
                        shop.buy(p, amount);
                    } catch (InvalidShopException e) {
                        p.sendMessage(MsgUtil.getMessage("shop-is-invalid"));
                        QuickShop.instance.log(e.getMessage());
                        return;
                    }
                    MsgUtil.sendSellSuccess(p, shop, amount);
                    plugin.log(p.getName() + " sold " + amount + " for " + (shop.getPrice() * amount) + " to "
                            + shop.toString());
                }
                shop.setSignText(); // Update the signs count
            }
            /* If it was already cancelled (from destroyed) */
            else {
            }
        });
    }
    
    private boolean validatePurchase(Player p, int amount, Shop shop) {
        if (amount == 0) {
            // Dumb.
            MsgUtil.sendPurchaseSuccess(p, shop, amount);
            return true;
        } else if (amount < 0) {
            // & Dumber
            p.sendMessage(MsgUtil.getMessage("negative-amount"));
            return true;
        }
        return false;
    }
    
    /**
     * Returns a new shop iterator object, allowing iteration over shops
     * easily, instead of sorting through a 3D hashmap.
     * 
     * @return a new shop iterator object.
     */
    public Iterator<Shop> getShopIterator() {
        return new ShopIterator();
    }

    public String format(double d) {
        return plugin.getEcon().format(d);
    }

    public class ShopIterator implements Iterator<Shop> {
        private Iterator<Shop>                                              shops;
        private Iterator<HashMap<Location, Shop>>                           chunks;
        private final Iterator<HashMap<ShopChunk, HashMap<Location, Shop>>> worlds;

        private Shop                                                        current;

        public ShopIterator() {
            worlds = getShops().values().iterator();
        }

        /**
         * Returns true if there is still more shops to
         * iterate over.
         */
        @Override
        public boolean hasNext() {
            if (shops == null || !shops.hasNext()) {
                if (chunks == null || !chunks.hasNext()) {
                    if (!worlds.hasNext()) {
                        return false;
                    } else {
                        chunks = worlds.next().values().iterator();
                        return hasNext();
                    }
                } else {
                    shops = chunks.next().values().iterator();
                    return hasNext();
                }
            }
            return true;
        }

        /**
         * Fetches the next shop.
         * Throws NoSuchElementException if there are no more shops.
         */
        @Override
        public Shop next() {
            if (shops == null || !shops.hasNext()) {
                if (chunks == null || !chunks.hasNext()) {
                    if (!worlds.hasNext()) {
                        throw new NoSuchElementException("No more shops to iterate over!");
                    }
                    chunks = worlds.next().values().iterator();
                }
                shops = chunks.next().values().iterator();
            }
            if (!shops.hasNext()) {
                return next(); // Skip to the next one (Empty iterator?)
            }
            current = shops.next();
            return current;
        }

        /**
         * Removes the current shop.
         * This method will delete the shop from
         * memory and the database.
         */
        @Override
        public void remove() {
            current.delete(false);
            shops.remove();
        }
    }
}
