package org.maxgamer.QuickShop.Shop;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;
import org.maxgamer.QuickShop.QuickShop;

/**
 * @author Netherfoam
 * A display item, that spawns a block above the chest and cannot be interacted with.
 */
public class DisplayItem{
	//private QuickShop plugin;
	private Shop shop;
	private ItemStack iStack;
	private Item item;
	private Location displayLoc;
	
	/**
	 * Creates a new display item.
	 * @param plugin The plugin creating the display item.
	 * @param shop The shop (See Shop)
	 * @param iStack The item stack to clone properties of the display item from.
	 */
	public DisplayItem(/*QuickShop plugin,*/ Shop shop, ItemStack iStack){
		//this.plugin = plugin;
		this.shop = shop;
		this.iStack = iStack.clone();
		this.displayLoc = shop.getLocation().clone().add(0.5, 1.2, 0.5);
		
		if(displayLoc.getWorld() != null){
			this.removeDupe();
			this.spawn();
		}
	}
	
	/**
	 * Spawns the dummy item on top of the shop.
	 */
	public void spawn(){
		if(shop.getLocation().getWorld() == null) return;
		
		this.item = shop.getLocation().getWorld().dropItem(this.getDisplayLocation(), this.iStack);
		this.item.setVelocity(new Vector(0, 0.01, 0));
		this.item.setPickupDelay(Integer.MAX_VALUE);  
	}
	
	/**
	 * Spawns the new display item and removes duplicate items.
	 */
	public void respawn(){
		remove();
		spawn();
	}
	
	/**
	 * Removes all items floating ontop of the chest
	 * that aren't the display item.
	 */
	public boolean removeDupe(){
		if(shop.getLocation().getWorld() == null) return false;
		QuickShop qs = (QuickShop) Bukkit.getPluginManager().getPlugin("QuickShop");
		Location displayLoc = shop.getLocation().getBlock().getRelative(0, 1, 0).getLocation();
		
		boolean removed = false;
		
		Chunk c = displayLoc.getChunk();
		for (Entity e : c.getEntities()) {
			if(!(e instanceof Item)) continue;
			if(this.item != null && e.getEntityId() == this.item.getEntityId()) continue;
			Location eLoc = e.getLocation().getBlock().getLocation();
			
			//if(eLoc.equals(displayLoc) || eLoc.equals(obj)) {
			//if(eLoc.distanceSquared(displayLoc))
			if(eLoc.equals(displayLoc) || eLoc.equals(shop.getLocation())){
				ItemStack near = ((Item) e).getItemStack();
				//if its the same its a dupe
				if(this.shop.matches(near)){
					e.remove();
					qs.log("[Debug] Removed dupe @" + near.getType());
					removed = true;
				}
			}
		}
		return removed;
	}
	
	/**
	 * Removes the display item.
	 */
	public void remove(){
		if(this.item == null) return;
		this.item.remove();
	}
	
	/**
	 * @return Returns the exact location of the display item.  (1 above shop block, in the center)
	 */
	public Location getDisplayLocation(){
		return this.displayLoc;
	}
	
	/**
	 * Returns the reference to this shops item. Do not modify.
	 */
	public Item getItem(){
		return this.item;
	}
}