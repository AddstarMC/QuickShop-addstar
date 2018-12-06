package org.maxgamer.QuickShop.Listeners;

import java.util.HashMap;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.block.data.Directional;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.maxgamer.QuickShop.QuickShop;
import org.maxgamer.QuickShop.Shop.DisplayItem;
import org.maxgamer.QuickShop.Shop.Info;
import org.maxgamer.QuickShop.Shop.Shop;
import org.maxgamer.QuickShop.Shop.ShopAction;
import org.maxgamer.QuickShop.Util.MsgUtil;
import org.maxgamer.QuickShop.Util.Util;

public class PlayerListener implements Listener {
    private final QuickShop plugin;

    public PlayerListener(QuickShop plugin) {
        this.plugin = plugin;
    }

    /*
     * Could be useful one day
     * private LinkedList<String> getParents(Class<?> clazz){
     * LinkedList<String> classes = new LinkedList<String>();
     * while(clazz != null){
     * classes.add("Extends " + ChatColor.GREEN + clazz.getCanonicalName());
     * for(Class<?> iface : clazz.getInterfaces()){
     * classes.add("Implements " + ChatColor.RED + iface.getCanonicalName());
     * classes.addAll(getParents(iface));
     * }
     * clazz = clazz.getSuperclass();
     * }
     * return classes;
     * }
     */

    /**
     * Handles players left clicking a chest.
     * Left click a NORMAL chest with item : Send creation menu
     * Left click a SHOP chest : Send purchase menu
     * @param e the event
     */
    @EventHandler(ignoreCancelled = true)
    public void onClick(PlayerInteractEvent e) {
        if (e.getAction() != Action.LEFT_CLICK_BLOCK) {
            return;
        }

        final Block b = e.getClickedBlock();

        if (!Util.canBeShop(b) && b.getType() != Material.WALL_SIGN) {
            return;
        }
        final Player p = e.getPlayer();

        if (plugin.sneak && !p.isSneaking()) {
            // Sneak only
            return;
        }

        final Location loc = b.getLocation();
        final ItemStack item = e.getItem();

        // Get the shop
        Shop shop = plugin.getShopManager().getShop(loc);
        // If that wasn't a shop, search nearby shops
        if (shop == null && b.getType() == Material.WALL_SIGN) {
            final Block attached = Util.getAttached(b);
            if (attached != null) {
                shop = plugin.getShopManager().getShop(attached.getLocation());
            }
        }
        // Purchase handling
        if (shop != null && p.hasPermission("quickshop.use") && (!plugin.sneakTrade || p.isSneaking())) {
            shop.onClick();
            // Text menu
            if(shop.isClosed()){
                p.sendMessage(MsgUtil.getMessage("shop-is-closed"));
                return;
            }
            MsgUtil.sendShopInfo(p, shop);
            if (shop.isSelling()) {
                p.sendMessage(MsgUtil.getMessage("how-many-buy"));
            } else {
                final int items = Util.countItems(p.getInventory(), shop.getItem());

                p.sendMessage(MsgUtil.getMessage("how-many-sell", "" + items));
            }

            // Add the new action
            final HashMap<String, Info> actions = plugin.getShopManager().getActions();
            final Info info = new Info(shop.getLocation(), ShopAction.BUY, null, null, shop);
            actions.put(p.getName(), info);
    
        }
        // Handles creating shops
        else if (shop == null && item != null && item.getType() != Material.AIR
                && p.hasPermission("quickshop.create.sell") && Util.canBeShop(b)
                && p.getGameMode() != GameMode.CREATIVE && (!plugin.sneakCreate || p.isSneaking())) {
            if (!plugin.getShopManager().canBuildShop(p, b, e.getBlockFace())) {
                // As of the new checking system, most plugins will tell the
                // player why they can't create a shop there.
                // So telling them a message would cause spam etc.
                return;
            }
            if (Util.getSecondHalf(b) != null && !p.hasPermission("quickshop.create.double")) {
                p.sendMessage(MsgUtil.getMessage("no-double-chests"));
                return;
            }

            if (Util.isBlacklisted(item.getType()) && !p.hasPermission("quickshop.bypass." + item.getType().name())) {
                p.sendMessage(MsgUtil.getMessage("blacklisted-item"));
                return;
            }

            // Finds out where the sign should be placed for the shop
            Block last = null;

            last = e.getClickedBlock().getRelative(e.getBlockFace());
            if(e.getClickedBlock().getBlockData() instanceof Directional){
                BlockFace bf = ((Directional) e.getClickedBlock().getBlockData()).getFacing();
                last = e.getClickedBlock().getRelative(bf);
            }
            // Send creation menu.
            final Info info = new Info(b.getLocation(), ShopAction.CREATE, e.getItem(), last);
            plugin.getShopManager().getActions().put(p.getName(), info);
            p.sendMessage(MsgUtil.getMessage("how-much-to-trade-for", Util.getName(info.getItem())));
        }
    }
    /**
     * Waits for a player to move too far from a shop, then cancels the menu.
     * @param e the event
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onMove(PlayerMoveEvent e) {
        if (e.isCancelled()) {
            return;
        }
        final Info info = plugin.getShopManager().getActions().get(e.getPlayer().getName());
        if (info != null) {
            final Player p = e.getPlayer();
            final Location loc1 = info.getLocation();
            final Location loc2 = p.getLocation();

            if (loc1.getWorld() != loc2.getWorld() || loc1.distanceSquared(loc2) > 25) {
                if (info.getAction() == ShopAction.CREATE) {
                    p.sendMessage(MsgUtil.getMessage("shop-creation-cancelled"));
                } else if (info.getAction() == ShopAction.BUY) {
                    p.sendMessage(MsgUtil.getMessage("shop-purchase-cancelled"));
                }
                plugin.getShopManager().getActions().remove(p.getName());
            }
        }
    }

    @EventHandler
    public void onTeleport(PlayerTeleportEvent e) {
        final PlayerMoveEvent me = new PlayerMoveEvent(e.getPlayer(), e.getFrom(), e.getTo());
        onMove(me);
    }

    @EventHandler
    public void onJoin(final PlayerJoinEvent e) {
        // Notify the player any messages they were sent
        Bukkit.getScheduler().runTaskLater(QuickShop.instance, () -> MsgUtil.flush(e.getPlayer()), 60);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent e) {
        // Remove them from the menu
        plugin.getShopManager().getActions().remove(e.getPlayer().getName());
    }

    @EventHandler
    public void onPlayerPickup(EntityPickupItemEvent e) {
        if(!(e.getEntity() instanceof Player)){
            return;
        }
        final ItemStack stack = e.getItem().getItemStack();
        try {
            if (stack.getItemMeta().getDisplayName().startsWith(ChatColor.RED + "QuickShop ")) {
                e.setCancelled(true);
                return;
                // You shouldn't be able to pick up that...
            }
            if(DisplayItem.isDisplayItem(e.getItem())){
                e.setCancelled(true);
            }
        } catch (final NullPointerException ignored) {} // if meta/displayname/stack
                                                   // is null. We don't really
                                                   // care in that case.
    }
    
    @EventHandler(priority=EventPriority.LOW, ignoreCancelled=false)
    public void onPlayerClick(InventoryClickEvent event)  {
        ItemStack item = event.getCurrentItem();
        if (item == null) {
            return;
        }
        
        ItemMeta meta = item.getItemMeta();
        if (meta != null && meta.getDisplayName() != null && meta.getDisplayName().startsWith(ChatColor.RED + "QuickShop ")) {
            event.setCurrentItem(null);
            event.setCancelled(true);
        }
    }
}