package org.maxgamer.QuickShop.Listeners;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.maxgamer.QuickShop.QuickShop;
import org.maxgamer.QuickShop.Shop.Shop;
import org.maxgamer.QuickShop.Util.MsgUtil;
import org.maxgamer.QuickShop.Util.Util;

import java.util.ArrayList;
import java.util.List;

public class LockListener implements Listener {
    private final QuickShop plugin;

    public LockListener(QuickShop plugin) {
        this.plugin = plugin;
    }

    @EventHandler(ignoreCancelled = true)
    public void onClick(PlayerInteractEvent e) {
        Block b = e.getClickedBlock();
        final Player p = e.getPlayer();
        if (e.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return; // Didn't right click it, we dont care.
        }
        if (!Util.canBeShop(b)) {
            return; // Interacted with air
        }

        Shop shop = plugin.getShopManager().getShop(b.getLocation());

        // Make sure they're not using the non-shop half of a double chest.
        if (shop == null) {
            b = Util.getSecondHalf(b);
            if (b == null) {
                return;
            }
            shop = plugin.getShopManager().getShop(b.getLocation());
            if (shop == null) {
                return;
            }
        }

        if (!p.equals(shop.getOwner().getPlayer())) {
            if (p.hasPermission("quickshop.other.open")) {
                p.sendMessage(MsgUtil.getMessage("bypassing-lock"));
                return;
            }
            p.sendMessage(MsgUtil.getMessage("that-is-locked"));
            e.setCancelled(true);
        }
    }

    /**
     * Handles hopper placement
     * @param e the event
     */
    @EventHandler(priority = EventPriority.LOW)
    public void onPlace(BlockPlaceEvent e) {
        final Block b = e.getBlock();
        try {
            if (b.getType() != Material.HOPPER) {
                return;
            }
        } catch (final NoSuchFieldError er) {
            return; // Your server doesn't have hoppers
        }
        Block c = e.getBlockAgainst();
        if (!Util.canBeShop(c)) {
            return;
        }
        final Player p = e.getPlayer();

        Shop shop = plugin.getShopManager().getShop(c.getLocation());
        if (shop == null) {
            c = Util.getSecondHalf(c);
            if (c == null) {
                return; // You didn't place a hopper on a shop. Meh.
            } else {
                shop = plugin.getShopManager().getShop(c.getLocation());
            }

            if (shop == null) {
                return;
            }
        }

        if (!p.equals(shop.getOwner().getPlayer())) {
            if (p.hasPermission("quickshop.other.open")) {
                p.sendMessage(MsgUtil.getMessage("bypassing-lock"));
                return;
            }
            p.sendMessage(MsgUtil.getMessage("that-is-locked"));
            e.setCancelled(true);
        }
    }

    /**
     * Removes chests when they're destroyed.
     * @param e the event
     */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent e) {
        Block b = e.getBlock();
        final Player p = e.getPlayer();

        // If the chest was a chest
        if (Util.canBeShop(b)) {
            final Shop shop = plugin.getShopManager().getShop(b.getLocation());
            if (shop == null) {
                return; // Wasn't a shop
            }

            // If they owned it or have bypass perms, they can destroy it
            if (!p.equals(shop.getOwner().getPlayer()) && !p.hasPermission("quickshop.other.destroy")) {
                e.setCancelled(true);
                p.sendMessage(MsgUtil.getMessage("no-permission"));
            }
        } else if (Util.checkIfSign(b)) {
            b = Util.getAttached(b);
            if (b == null) {
                return;
            }
            final Shop shop = plugin.getShopManager().getShop(b.getLocation());
            if (shop == null) {
                return;
            }

            // If they're the shop owner or have bypass perms, they can destroy
            // it.
            if (!p.equals(shop.getOwner().getPlayer()) && !p.hasPermission("quickshop.other.destroy")) {
                e.setCancelled(true);
                p.sendMessage(MsgUtil.getMessage("no-permission"));
            }
        }
    }

    /**
     * Handles shops breaking through explosions
     * @param e the event
     */
    @EventHandler(priority = EventPriority.LOW)
    public void onExplode(EntityExplodeEvent e) {
        if (e.isCancelled()) {
            return;
        }
        List<Integer> index = new ArrayList<>();
        for (Block b:e.blockList()) {
            final Shop shop = plugin.getShopManager().getShop(b.getLocation());
            if (shop != null) {
                index.add(e.blockList().indexOf(b));
            }
        }
        //to avoid modifying the list while we are parsing it
        for (int i:index){
            e.blockList().remove(i);
        }
    }
}