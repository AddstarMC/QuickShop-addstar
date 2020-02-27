package org.maxgamer.QuickShop.Command;

import java.io.File;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;
import org.bukkit.util.BlockIterator;
import org.maxgamer.QuickShop.QuickShop;
import org.maxgamer.QuickShop.Database.Database;
import org.maxgamer.QuickShop.Database.MySQLCore;
import org.maxgamer.QuickShop.Database.SQLiteCore;
import org.maxgamer.QuickShop.Shop.ContainerShop;
import org.maxgamer.QuickShop.Shop.Shop;
import org.maxgamer.QuickShop.Shop.ShopChunk;
import org.maxgamer.QuickShop.Shop.ShopType;
import org.maxgamer.QuickShop.Util.MsgUtil;
import org.maxgamer.QuickShop.exceptions.InvalidShopException;

public class QS implements CommandExecutor {
    QuickShop plugin;

    public QS(QuickShop plugin) {
        this.plugin = plugin;
    }

    private void setUnlimited(CommandSender sender) {
        if (sender instanceof Player && sender.hasPermission("quickshop.unlimited")) {
            final BlockIterator bIt = new BlockIterator((Player) sender, 10);
            while (bIt.hasNext()) {
                final Block b = bIt.next();
                final Shop shop = plugin.getShopManager().getShop(b.getLocation());
                if (shop != null) {
                    shop.setUnlimited(!shop.isUnlimited());
                    shop.update();
                    sender.sendMessage(MsgUtil.getMessage("command.toggle-unlimited", (shop.isUnlimited() ? "unlimited"
                            : "limited")));
                    return;
                }
            }
            sender.sendMessage(MsgUtil.getMessage("not-looking-at-shop"));
        } else {
            sender.sendMessage(MsgUtil.getMessage("no-permission"));
        }
    }

    private void remove(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Only players may use that command.");
            return;
        }
        if (!sender.hasPermission("quickshop.delete")) {
            sender.sendMessage(ChatColor.RED
                    + "You do not have permission to use that command. Try break the shop instead?");
            return;
        }
        final Player p = (Player) sender;
        final BlockIterator bIt = new BlockIterator(p, 10);
        while (bIt.hasNext()) {
            final Block b = bIt.next();
            final Shop shop = plugin.getShopManager().getShop(b.getLocation());
            if (shop != null) {
                if (p.equals(shop.getOwner().getPlayer())) {
                    shop.delete();
                    sender.sendMessage(ChatColor.GREEN + "Success. Deleted shop.");
                } else {
                    p.sendMessage(ChatColor.RED + "That's not your shop!");
                }
                return;
            }
        }
        p.sendMessage(ChatColor.RED + "No shop found!");
    }

    private void export(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /qs export mysql|sqlite");
            return;
        }
        final String type = args[1].toLowerCase();
        if (type.startsWith("mysql")) {
            if (plugin.getDB().getCore() instanceof MySQLCore) {
                sender.sendMessage(ChatColor.RED + "Database is already MySQL");
                return;
            }
            final ConfigurationSection cfg = plugin.getConfig().getConfigurationSection("database");
            final String host = cfg.getString("host");
            final String port = cfg.getString("port");
            final String user = cfg.getString("user");
            final String pass = cfg.getString("password");
            final String name = cfg.getString("database");
            Properties props =  new Properties();
            props.put("user",user);
            props.put("pass",pass);
            ConfigurationSection dbprops = cfg.getConfigurationSection("properties");
            if(dbprops != null) {
                for (Map.Entry<String, Object> entry : dbprops.getValues(false).entrySet()) {
                    props.put(entry.getKey(), entry.getValue());
                }
            }
            final MySQLCore core = new MySQLCore(host, name, port,props);
            Database target;

            try {
                target = new Database(core);
                QuickShop.instance.getDB().copyTo(target);
                sender.sendMessage(ChatColor.GREEN + "Success - Exported to MySQL " + user + "@" + host + "." + name);
            } catch (final Exception e) {
                e.printStackTrace();
                sender.sendMessage(ChatColor.RED + "Failed to export to MySQL " + user + "@" + host + "." + name
                        + ChatColor.DARK_RED + " Reason: " + e.getMessage());
            }
            return;
        }
        if (type.startsWith("sql") || type.contains("file")) {
            if (plugin.getDB().getCore() instanceof SQLiteCore) {
                sender.sendMessage(ChatColor.RED + "Database is already SQLite");
                return;
            }

            final File file = new File(plugin.getDataFolder(), "shops.db");
            if (file.exists()) {
                if (!file.delete()) {
                    sender.sendMessage(ChatColor.RED
                            + "Warning: Failed to delete old shops.db file. This may cause errors.");
                }
            }

            final SQLiteCore core = new SQLiteCore(file);
            try {
                final Database target = new Database(core);
                QuickShop.instance.getDB().copyTo(target);
                sender.sendMessage(ChatColor.GREEN + "Success - Exported to SQLite: " + file.toString());
            } catch (final Exception e) {
                e.printStackTrace();
                sender.sendMessage(ChatColor.RED + "Failed to export to SQLite: " + file.toString() + " Reason: "
                        + e.getMessage());
            }
            return;
        }

        sender.sendMessage(ChatColor.RED + "No target given. Usage: /qs export mysql|sqlite");
    }

    private void setOwner(CommandSender sender, String[] args) {
        if (sender instanceof Player && sender.hasPermission("quickshop.setowner")) {
            if (args.length < 2) {
                sender.sendMessage(MsgUtil.getMessage("command.no-owner-given"));
                return;
            }
            
            OfflinePlayer newOwner = Bukkit.getOfflinePlayer(args[1]);
            if (!newOwner.hasPlayedBefore()) {
                sender.sendMessage(MsgUtil.getMessage("command.unknown-owner-given"));
                return;
            }
            final BlockIterator bIt = new BlockIterator((Player) sender, 10);
            while (bIt.hasNext()) {
                final Block b = bIt.next();
                final Shop shop = plugin.getShopManager().getShop(b.getLocation());
                if (shop != null) {
                    shop.setOwner(newOwner);
                    shop.update();

                    sender.sendMessage(MsgUtil.getMessage("command.new-owner", shop.getOwner().getName()));
                    return;
                }
            }
            sender.sendMessage(MsgUtil.getMessage("not-looking-at-shop"));
        } else {
            sender.sendMessage(MsgUtil.getMessage("no-permission"));
        }
    }

    private void refill(CommandSender sender, String[] args) {
        if (sender instanceof Player && sender.hasPermission("quickshop.refill")) {
            if (args.length < 2) {
                sender.sendMessage(MsgUtil.getMessage("command.no-amount-given"));
                return;
            }

            int add;
            try {
                add = Integer.parseInt(args[1]);
            } catch (final NumberFormatException e) {
                sender.sendMessage(MsgUtil.getMessage("thats-not-a-number"));
                return;
            }

            final BlockIterator bIt = new BlockIterator((Player) sender, 10);
            while (bIt.hasNext()) {
                final Block b = bIt.next();
                final Shop shop = plugin.getShopManager().getShop(b.getLocation());
                if (shop != null) {
                    try {
                        shop.add(shop.getItem(), add);
                    } catch (InvalidShopException e) {
                        sender.sendMessage(MsgUtil.getMessage("shop-is-invalid"));
                        QuickShop.instance.log(e.getMessage());
                        return;
                    }
                    sender.sendMessage(MsgUtil.getMessage("refill-success"));
                    return;
                }
            }
            sender.sendMessage(MsgUtil.getMessage("not-looking-at-shop"));
        } else {
            sender.sendMessage(MsgUtil.getMessage("no-permission"));
        }
    }

    private void empty(CommandSender sender, String[] args) {
        if (sender instanceof Player && sender.hasPermission("quickshop.refill")) {
            final BlockIterator bIt = new BlockIterator((Player) sender, 10);
            while (bIt.hasNext()) {
                final Block b = bIt.next();
                final Shop shop = plugin.getShopManager().getShop(b.getLocation());
                if (shop != null) {
                    if (shop instanceof ContainerShop) {
                        final ContainerShop cs = (ContainerShop) shop;
                        try {
                            cs.getInventory().clear();
                        } catch (InvalidShopException e) {
                            sender.sendMessage(MsgUtil.getMessage("shop-is-invalid"));
                            QuickShop.instance.log(e.getMessage());
                            return;
                        }
                        sender.sendMessage(MsgUtil.getMessage("empty-success"));
                        return;
                    } else {
                        sender.sendMessage(MsgUtil.getMessage("not-looking-at-shop"));
                        return;
                    }
                }
            }
            sender.sendMessage(MsgUtil.getMessage("not-looking-at-shop"));
        } else {
            sender.sendMessage(MsgUtil.getMessage("no-permission"));
        }
    }

    private void find(CommandSender sender, String[] args) {
        if (sender instanceof Player && sender.hasPermission("quickshop.find")) {
            if (args.length < 2) {
                sender.sendMessage(MsgUtil.getMessage("command.no-type-given"));
                return;
            }

            final StringBuilder sb = new StringBuilder(args[1]);
            for (int i = 2; i < args.length; i++) {
                sb.append(" ").append(args[i]);
            }
            String lookFor = sb.toString();
            lookFor = lookFor.toLowerCase();

            final Player p = (Player) sender;
            final Location loc = p.getEyeLocation().clone();

            final double minDistance = plugin.getConfig().getInt("shop.find-distance");
            double minDistanceSquared = minDistance * minDistance;
            final int chunkRadius = (int) minDistance / 16 + 1;
            Shop closest = null;

            final Chunk c = loc.getChunk();

            for (int x = -chunkRadius + c.getX(); x < chunkRadius + c.getX(); x++) {
                for (int z = -chunkRadius + c.getZ(); z < chunkRadius + c.getZ(); z++) {
                    final Chunk d = c.getWorld().getChunkAt(x, z);
                    final HashMap<Location, Shop> inChunk = plugin.getShopManager().getShops(d);
                    if (inChunk == null) {
                        continue;
                    }
                    for (final Shop shop: inChunk.values()) {
                        if (shop.getDataName().toLowerCase().contains(lookFor)
                                && shop.getLocation().distanceSquared(loc) < minDistanceSquared) {
                            closest = shop;
                            minDistanceSquared = shop.getLocation().distanceSquared(loc);
                        }
                    }
                }
            }
            if (closest == null) {
                sender.sendMessage(MsgUtil.getMessage("no-nearby-shop", args[1]));
                return;
            }
            final Location lookat = closest.getLocation().clone().add(0.5, 0.5, 0.5);

            // Hack fix to make /qs find not used by /back
            p.teleport(lookAt(loc, lookat).add(0, -1.62, 0), TeleportCause.UNKNOWN);
            p.sendMessage(MsgUtil.getMessage("nearby-shop-this-way",
                    "" + (int) Math.floor(Math.sqrt(minDistanceSquared))));

        } else {
            sender.sendMessage(MsgUtil.getMessage("no-permission"));
        }
    }

    private void setBuy(CommandSender sender) {
        if (sender instanceof Player && sender.hasPermission("quickshop.create.buy")) {
            final BlockIterator bIt = new BlockIterator((Player) sender, 10);
            while (bIt.hasNext()) {
                final Block b = bIt.next();
                final Shop shop = plugin.getShopManager().getShop(b.getLocation());
                if (shop != null && sender.equals(shop.getOwner().getPlayer())) {
                    shop.setShopType(ShopType.BUYING);
                    shop.setSignText();
                    shop.update();

                    sender.sendMessage(MsgUtil.getMessage("command.now-buying", shop.getDataName()));
                    return;
                }
            }
            sender.sendMessage(MsgUtil.getMessage("not-looking-at-shop"));
            return;
        }
        sender.sendMessage(MsgUtil.getMessage("no-permission"));
    }

    private void setSell(CommandSender sender) {
        if (sender instanceof Player && sender.hasPermission("quickshop.create.sell")) {
            final BlockIterator bIt = new BlockIterator((Player) sender, 10);
            while (bIt.hasNext()) {
                final Block b = bIt.next();
                final Shop shop = plugin.getShopManager().getShop(b.getLocation());
                if (shop != null && sender.equals(shop.getOwner().getPlayer())) {
                    shop.setShopType(ShopType.SELLING);
                    shop.setSignText();
                    shop.update();
                    sender.sendMessage(MsgUtil.getMessage("command.now-selling", shop.getDataName()));
                    return;
                }
            }
            sender.sendMessage(MsgUtil.getMessage("not-looking-at-shop"));
            return;
        }
        sender.sendMessage(MsgUtil.getMessage("no-permission"));
    }

    private void setPrice(CommandSender sender, String[] args) {
        if (sender instanceof Player && sender.hasPermission("quickshop.create.changeprice")) {
            if (args.length < 2) {
                sender.sendMessage(MsgUtil.getMessage("no-price-given"));
                return;
            }
            double price;
            try {
                price = Double.parseDouble(args[1]);
            } catch (final NumberFormatException e) {
                sender.sendMessage(MsgUtil.getMessage("thats-not-a-number"));
                return;
            }

            if (price < 0.01) {
                sender.sendMessage(MsgUtil.getMessage("price-too-cheap"));
                return;
            }
            double fee = 0;
            if (plugin.priceChangeRequiresFee) {
                fee = plugin.getConfig().getDouble("shop.fee-for-price-change");
                if (fee > 0 && plugin.getEcon().getBalance((Player)sender) < fee) {
                    sender.sendMessage(MsgUtil.getMessage("you-cant-afford-to-change-price",
                            plugin.getEcon().format(fee)));
                    return;
                }

            }
            final BlockIterator bIt = new BlockIterator((Player) sender, 10);
            // Loop through every block they're looking at upto 10 blocks away
            while (bIt.hasNext()) {
                final Block b = bIt.next();
                final Shop shop = plugin.getShopManager().getShop(b.getLocation());

                if (shop != null && (sender.equals(shop.getOwner().getPlayer()) || sender.hasPermission("quickshop.other.price"))) {
                    if (shop.getPrice() == price) {
                        // Stop here if there isn't a price change
                        sender.sendMessage(MsgUtil.getMessage("no-price-change"));
                        return;
                    }
                    if (fee > 0) {
                        if (!plugin.getEcon().withdraw((Player)sender, fee)) {
                            sender.sendMessage(MsgUtil.getMessage("you-cant-afford-to-change-price", plugin.getEcon()
                                    .format(fee)));
                            return;
                        }

                        sender.sendMessage(MsgUtil.getMessage("fee-charged-for-price-change",
                                plugin.getEcon().format(fee)));
                        if (plugin.getTaxAccount().hasPlayedBefore()) {
                            plugin.getEcon().deposit(plugin.getTaxAccount(), fee);
                        }
                    }

                    // Update the shop
                    shop.setPrice(price);
                    shop.setSignText();
                    shop.update();
                    sender.sendMessage(MsgUtil.getMessage("price-is-now", plugin.getEcon().format(shop.getPrice())));

                    // Chest shops can be double shops.
                    if (shop instanceof ContainerShop) {
                        final ContainerShop cs = (ContainerShop) shop;
                        if (cs.isDoubleShop()) {
                            final Shop nextTo = cs.getAttachedShop();

                            if (cs.isSelling()) {
                                if (cs.getPrice() < nextTo.getPrice()) {
                                    sender.sendMessage(MsgUtil.getMessage("buying-more-than-selling"));
                                }
                            } else {
                                // Buying
                                if (cs.getPrice() > nextTo.getPrice()) {
                                    sender.sendMessage(MsgUtil.getMessage("buying-more-than-selling"));
                                }
                            }
                        }
                    }
                    return;
                }
            }
            sender.sendMessage(MsgUtil.getMessage("not-looking-at-shop"));
            return;
        }
        sender.sendMessage(MsgUtil.getMessage("no-permission"));
    }

    private void clean(CommandSender sender) {
        if (sender.hasPermission("quickshop.clean")) {
            sender.sendMessage(MsgUtil.getMessage("command.cleaning"));
            final Iterator<Shop> shIt = plugin.getShopManager().getShopIterator();
            int i = 0;
            while (shIt.hasNext()) {
                final Shop shop = shIt.next();
                try {
                    if (shop.getLocation().getWorld() != null && shop.isSelling() && shop.getRemainingStock() == 0
                          && shop instanceof ContainerShop) {
                        final ContainerShop cs = (ContainerShop) shop;
                        if (cs.isDoubleShop()) {
                            continue;
                        }
                        sender.sendMessage(
                              MsgUtil.getMessage("success-removed-shop-id", cs.toString()));

                        shIt.remove(); // Is selling, but has no stock, and is a
                        // chest shop, but is not a double shop. Can
                        // be deleted safely.
                        i++;

                    }
                } catch (InvalidShopException e) {
                    sender.sendMessage(MsgUtil.getMessage("shop-is-invalid"));
                    QuickShop.instance.log(e.getMessage());
                    QuickShop.instance.log("Shop was removed");
                    shIt.remove();
                return;
            }
            }

            MsgUtil.clean();
            sender.sendMessage(MsgUtil.getMessage("command.cleaned", "" + i));
            return;
        }
        sender.sendMessage(MsgUtil.getMessage("no-permission"));
    }

    private void reload(CommandSender sender) {
        if (sender.hasPermission("quickshop.reload")) {
            sender.sendMessage(MsgUtil.getMessage("command.reloading"));
            Bukkit.getPluginManager().disablePlugin(plugin);
            Bukkit.getPluginManager().enablePlugin(plugin);
            return;
        }
        sender.sendMessage(MsgUtil.getMessage("no-permission"));
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String commandLabel, String[] args) {
        if (args.length > 0) {
            final String subArg = args[0].toLowerCase();

            if (subArg.equals("unlimited")) {
                setUnlimited(sender);
                return true;
            } else if (subArg.equals("setowner")) {
                setOwner(sender, args);
                return true;
            } else if (subArg.equals("find")) {
                find(sender, args);
                return true;
            } else if (subArg.startsWith("buy")) {
                setBuy(sender);
                return true;
            } else if (subArg.startsWith("sell")) {
                setSell(sender);
                return true;
            } else if (subArg.startsWith("price")) {
                setPrice(sender, args);
                return true;
            } else if (subArg.equals("remove")) {
                remove(sender, args);
            } else if (subArg.equals("refill")) {
                refill(sender, args);
                return true;
            } else if (subArg.equals("empty")) {
                empty(sender, args);
                return true;
            } else if (subArg.equals("clean")) {
                clean(sender);
                return true;
            } else if (subArg.equals("reload")) {
                reload(sender);
                return true;
            } else if (subArg.equals("export")) {
                export(sender, args);
                return true;
            } else if (subArg.equals("info")) {
                if (sender.hasPermission("quickshop.info")) {
                    int buying, selling, doubles, chunks, worlds;
                    buying = selling = doubles = chunks = worlds = 0;

                    int nostock = 0;

                    for (final HashMap<ShopChunk, HashMap<Location, Shop>> inWorld: plugin.getShopManager().getShops()
                            .values()) {
                        worlds++;
                        for (final HashMap<Location, Shop> inChunk: inWorld.values()) {
                            chunks++;
                            for (final Shop shop: inChunk.values()) {
                                if (shop.isBuying()) {
                                    buying++;
                                } else if (shop.isSelling()) {
                                    selling++;
                                }
                                try {
                                    if (shop instanceof ContainerShop && ((ContainerShop) shop).isDoubleShop()) {
                                        doubles++;
                                    } else if (shop.isSelling() && shop.getRemainingStock() == 0) {
                                        nostock++;
                                    }
                                } catch (InvalidShopException e) {
                                    sender.sendMessage(MsgUtil.getMessage("shop-is-invalid"));
                                    QuickShop.instance.log(e.getMessage());
                                    return true;
                                }
                            }
                        }
                    }

                    sender.sendMessage(ChatColor.RED + "QuickShop Statistics...");
                    sender.sendMessage(ChatColor.GREEN + "" + (buying + selling) + " shops in " + chunks
                            + " chunks spread over " + worlds + " worlds.");
                    sender.sendMessage(ChatColor.GREEN + "" + doubles + " double shops. ");
                    sender.sendMessage(ChatColor.GREEN + "" + nostock
                            + " selling shops (excluding doubles) which will be removed by /qs clean.");

                    return true;
                }
                sender.sendMessage(MsgUtil.getMessage("no-permission"));
                return true;
            }
        } else {
            // Invalid arg given
            sendHelp(sender);
            return true;
        }
        // No args given
        sendHelp(sender);
        return true;
    }

    /**
     * Returns loc with modified pitch/yaw angles so it faces lookat
     * 
     * @param loc
     *            The location a players head is
     * @param lookat
     *            The location they should be looking
     * @return The location the player should be facing to have their crosshairs
     *         on the location lookAt
     *         Kudos to bergerkiller for most of this function
     */
    public Location lookAt(Location loc, Location lookat) {
        // Clone the loc to prevent applied changes to the input loc
        loc = loc.clone();

        // Values of change in distance (make it relative)
        final double dx = lookat.getX() - loc.getX();
        final double dy = lookat.getY() - loc.getY();
        final double dz = lookat.getZ() - loc.getZ();

        // Set yaw
        if (dx != 0) {
            // Set yaw start value based on dx
            if (dx < 0) {
                loc.setYaw((float) (1.5 * Math.PI));
            } else {
                loc.setYaw((float) (0.5 * Math.PI));
            }
            loc.setYaw(loc.getYaw() - (float) Math.atan(dz / dx));
        } else if (dz < 0) {
            loc.setYaw((float) Math.PI);
        }

        // Get the distance from dx/dz
        final double dxz = Math.sqrt(Math.pow(dx, 2) + Math.pow(dz, 2));

        final float pitch = (float) -Math.atan(dy / dxz);

        // Set values, convert to degrees
        // Minecraft yaw (vertical) angles are inverted (negative)
        loc.setYaw(-loc.getYaw() * 180f / (float) Math.PI + 360);
        // But pitch angles are normal
        loc.setPitch(pitch * 180f / (float) Math.PI);

        return loc;
    }

    public void sendHelp(CommandSender s) {
        s.sendMessage(MsgUtil.getMessage("command.description.title"));
        if (s.hasPermission("quickshop.unlimited")) {
            s.sendMessage(ChatColor.GREEN + "/qs unlimited" + ChatColor.YELLOW + " - "
                    + MsgUtil.getMessage("command.description.unlimited"));
        }
        if (s.hasPermission("quickshop.setowner")) {
            s.sendMessage(ChatColor.GREEN + "/qs setowner <player>" + ChatColor.YELLOW + " - "
                    + MsgUtil.getMessage("command.description.setowner"));
        }
        if (s.hasPermission("quickshop.create.buy")) {
            s.sendMessage(ChatColor.GREEN + "/qs buy" + ChatColor.YELLOW + " - "
                    + MsgUtil.getMessage("command.description.buy"));
        }
        if (s.hasPermission("quickshop.create.sell")) {
            s.sendMessage(ChatColor.GREEN + "/qs sell" + ChatColor.YELLOW + " - "
                    + MsgUtil.getMessage("command.description.sell"));
        }
        if (s.hasPermission("quickshop.create.changeprice")) {
            s.sendMessage(ChatColor.GREEN + "/qs price" + ChatColor.YELLOW + " - "
                    + MsgUtil.getMessage("command.description.price"));
        }
        if (s.hasPermission("quickshop.clean")) {
            s.sendMessage(ChatColor.GREEN + "/qs clean" + ChatColor.YELLOW + " - "
                    + MsgUtil.getMessage("command.description.clean"));
        }
        if (s.hasPermission("quickshop.find")) {
            s.sendMessage(ChatColor.GREEN + "/qs find <item>" + ChatColor.YELLOW + " - "
                    + MsgUtil.getMessage("command.description.find"));
        }
        if (s.hasPermission("quickshop.refill")) {
            s.sendMessage(ChatColor.GREEN + "/qs refill <amount>" + ChatColor.YELLOW + " - "
                    + MsgUtil.getMessage("command.description.refill"));
        }
        if (s.hasPermission("quickshop.empty")) {
            s.sendMessage(ChatColor.GREEN + "/qs empty" + ChatColor.YELLOW + " - "
                    + MsgUtil.getMessage("command.description.empty"));
        }
        if (s.hasPermission("quickshop.export")) {
            s.sendMessage(ChatColor.GREEN + "/qs export mysql|sqlite" + ChatColor.YELLOW
                    + " - Exports the database to SQLite or MySQL");
        }

    }
}