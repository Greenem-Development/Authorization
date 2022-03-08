package org.greenem.plugins.Authorization;

import org.bukkit.block.Block;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityAirChangeEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.hanging.HangingBreakByEntityEvent;
import org.bukkit.event.player.PlayerChatEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerPickupArrowEvent;
import org.bukkit.event.player.PlayerToggleFlightEvent;

/**
 * Copyright (c) Greenem
 * Please contact me if you need to use big parts of this projects for yourself
 * **/

public class EventsPrevent implements Listener {
	// Disabling everything for unauthorized players
	
	@EventHandler(priority=EventPriority.LOW)
	public void flightAttempt(PlayerToggleFlightEvent e) {
		if(Main.AuthPlayers.contains(e.getPlayer())) e.setCancelled(true);
	}
	
	@EventHandler
    public void AuthAttacking(EntityDamageByEntityEvent e) {
		if (e.getDamager() instanceof Player) {
			Player Damager = (Player) e.getDamager();
			if (Main.AuthPlayers.contains(Damager)) {
				e.setCancelled(true);
			}
		}
		if (e.getDamager() instanceof Projectile) {
            if (((Projectile) e.getDamager()).getShooter() instanceof Player) {
            	if (Main.AuthPlayers.contains((Player) ((Projectile) e.getDamager()).getShooter())) {
    				e.setCancelled(true);
    			}
    		}
        }
	}
	
	@EventHandler
    public void AuthInteracting(PlayerInteractEntityEvent e) {
		if(Main.AuthPlayers.contains(e.getPlayer())) {
			e.setCancelled(true);
        }
	}
	
	@EventHandler(priority = EventPriority.LOW)
	public void EntityTargetEvent(EntityTargetEvent e) {
		if(e.getTarget() instanceof Player) {
			Player p = (Player) e.getTarget();
	        if (e.getTarget() instanceof LivingEntity){
	            if(Main.AuthPlayers.contains(p)) {
	            	e.setCancelled(true);
	            }
	        }
		}
	}
	
	public void PlayerPickupItemEvent(PlayerPickupArrowEvent e) { //cancelling picking an arrow up
		if(Main.AuthPlayers.contains(e.getPlayer())) {
			e.setCancelled(true);
		}
	}
	
	@EventHandler
    public void onHungingBrake(HangingBreakByEntityEvent e) {
		if(e.getRemover().getType() == EntityType.PLAYER){
			if(Main.AuthPlayers.contains((Player) e.getRemover())) {
				e.setCancelled(true);
    		}
    	}
    }
	
	@EventHandler
	public void PlayerDropItemEvent (PlayerDropItemEvent e) {
		Player p = e.getPlayer();
		if(Main.AuthPlayers.contains(p)) {
			e.setCancelled(true);
		}
	}
	
	public void PlayerHitsPlayerEvt(EntityDamageByEntityEvent e) {
		if(e.getDamager() instanceof Player) {
			if(Main.AuthPlayers.contains((Player) e.getDamager())) {
				e.setCancelled(true);
			}
		}
	}
	
	@EventHandler
	public void DisableArmorStandIntaract (PlayerInteractAtEntityEvent e) {
		if(Main.AuthPlayers.contains(e.getPlayer())) {
			if(e.getRightClicked() instanceof ArmorStand) {
				e.setCancelled(true);
			}
		}
		if(e.getRightClicked() instanceof ArmorStand) {
			if(Main.AuthPlayers.contains(e.getPlayer())) {
				e.setCancelled(true);
			}
		}
	}
	
	@EventHandler
    public void DisableAuthMessages(PlayerChatEvent e) {
    	if(Main.AuthPlayers.contains(e.getPlayer())) {
    		if(!e.getMessage().startsWith("/")) {
    			e.setCancelled(true);
    		}
    	}
    }
	
	@EventHandler
	public void DamageRulesManager(EntityDamageEvent e) {
		//log(e.getEntity().getType() + "");
		if (e.getEntity().getType().toString().equals("PLAYER")){
			if(Main.AuthPlayers.contains((Player) e.getEntity())) {
					e.setCancelled(true);
			}
		}
	}
	
	@EventHandler
	public void BlockPlaceEvent(BlockPlaceEvent e) {
		if(Main.AuthPlayers.contains(e.getPlayer())){
			e.setCancelled(true);
		}
	}
	
	@EventHandler
	public void BlockBreakEvent(BlockBreakEvent e) {
		if(Main.AuthPlayers.contains(e.getPlayer())){
			e.setCancelled(true);
		}
	}
	
	@EventHandler
	public void PlayerInteractEvent(PlayerInteractEvent e){
		Player p = e.getPlayer();
        Block b = e.getClickedBlock();
        //if(e.getClickedBlock() != null) log(e.getClickedBlock().getType().toString());
        if(Main.AuthPlayers.contains(p)) {
        	e.setCancelled(true);
        }
	}
	
	@EventHandler
	public void FoodLevelChangeEvent(FoodLevelChangeEvent e) {
		if(e.getEntity() instanceof Player) {
			if(Main.AuthPlayers.contains((Player) e.getEntity())){
				e.setCancelled(true);
			}
		}
	}
	
	@EventHandler
	public void EntityAirChangeEvent(EntityAirChangeEvent e) {
		if(e.getEntity() instanceof Player) {
			Player p = (Player) e.getEntity();
			if(Main.AuthPlayers.contains((Player) e.getEntity())) {
				if(!Main.AuthAir.containsKey(p)) Main.AuthAir.put(p, Main.getAirFromFile(p));
				if(e.getAmount()<Main.AuthAir.get(p)) {
					//e.setAmount(-300);
					e.setAmount(Main.AuthAir.get(p));
				}
			}
		}
	}
	
	//Broken after 1.18
	/*@EventHandler
	public static void EEntityDismount(EntityDismountEvent e) {
		if(e.getEntity() instanceof Player) {
			//log("EntityDismountEvent");
			if(Main.AuthPlayers.contains((Player) e.getEntity())){
				e.setCancelled(true);
			}
		}
	}*/
	
	/*public static void EInventoryClick(InventoryClickEvent e) {
		if(e.getCurrentItem()!=null) {
			e.setCancelled(true);
		}
	}*/
	
//	
//	@EventHandler
//	public static void EEntityMount(EntityMountEvent e) {
//		log(Main.AuthPlayers.contains((Player) e.getEntity()) + "");
//		if(e.getEntity() instanceof Player) {
//			if(Main.AuthPlayers.contains((Player) e.getEntity())){
//				e.setCancelled(true);
//			}
//		}
//	}
}
