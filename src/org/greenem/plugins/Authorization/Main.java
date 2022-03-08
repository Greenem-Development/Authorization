package org.greenem.plugins.Authorization;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Logger;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityAirChangeEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.hanging.HangingBreakByEntityEvent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Server;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
//import org.bukkit.craftbukkit.libs.org.apache.commons.io.filefilter.SuffixFileFilter;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerChatEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerPickupArrowEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent.Result;
import org.bukkit.event.player.PlayerToggleFlightEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
//import org.spigotmc.event.entity.EntityDismountEvent;
//import net.minecraft.server.v1_16_R3.GameRules.GameRuleCategory;
//import net.minecraft.server.v1_8_R3.PlayerSelector;
import org.bukkit.scheduler.BukkitRunnable;
import org.greenem.plugins.Authorization.ConfigData;

/**
 * Copyright (c) Greenem
 * Please contact me if you need to use big parts of this projects for yourself
 * **/

public class Main extends JavaPlugin implements Listener {
	//Basically, before the project was just one big file, but I decided to break it to different parts for the file management not to look that bad LOL
	 
	public static Logger log = Bukkit.getLogger();
	public static Properties props = new Properties();
	
	public static ArrayList<Player> AuthPlayers = new ArrayList<>();
	public static Map <Player, Location> AuthPlayersLocs = new HashMap<>();
	public static Map <Player, Integer> AuthAttempts = new HashMap<>();
	public static Map <Player, Integer> AuthAir = new HashMap<>();
		
	public static File configsDirectory;
	public static File mainConfigDirectory;
	
	public static File authDirectory;
	public static File inventoriesDirectory;
	public static File dataDirectory;
	public static File authDataDirectory;
	
	public boolean Restarting = false;
	public boolean InternalDebug = false;
		
	@Override
	public void onEnable() {
		Restarting = false;
		log(ChatColor.GREEN + "Your AuthorizationV2 plugin has been enabled!");
		
		Bukkit.getServer().getPluginManager().registerEvents(this, this);
		Bukkit.getServer().getPluginManager().registerEvents(new EventsPrevent(), this);
		
		initFiles();
		loadConfig();
		copyHelpFile();
	}
	
	@Override
	public void onDisable() {
		Restarting = true;
		//log(ChatColor.GOLD + "RELOAD");
		for (Player p : Bukkit.getServer().getOnlinePlayers()) {
			saveAuthPlayerInFile(p);
			if(!AuthPlayers.contains(p)) {
				try {
					saveInventory(p);
					if(InternalDebug) log(ChatColor.AQUA + "saved1");
				} catch (IOException e) {
					if(ConfigData.logCatchedErrors) e.printStackTrace();
				}
				saveOPInFile(p.getName());
				saveGamemodeInFile(p);
				//log("1");
				saveHeldSlotInFile(p);
				saveFlyStateInFile(p);
				saveAirInFile(p);
			}
			saveExpInFile(p);
			//if(AuthPlayers.contains(p)) AuthPlayers.remove(p);
		}
		for (Player p : Bukkit.getServer().getOnlinePlayers()) {
			boolean auth = getAuthPlayerFromFile(p);
			if(auth) {
				try {
					loadInventory(p);
				} catch (IOException e) {
					if(ConfigData.logCatchedErrors) e.printStackTrace();
				}
				p.kickPlayer("Reloading server. Rejoin please");
			}
		}
		resetAuthListFile();
		log(ChatColor.RED + "Your AuthorizationV2 plugin has been disabled.");
	}

	public boolean onCommand(CommandSender sender, Command cmd, String commandLabel, String[] args){
		Player p = Bukkit.getServer().getPlayer(sender.getName());
		/*if(cmd.getName().equalsIgnoreCase("world")){
			if(args)
			return true;
		}*/
		
		//Bukkit.getServer().broadcastMessage(commandLabel);
		if(cmd.getName().equalsIgnoreCase("register") || (cmd.getName().equalsIgnoreCase("r")) || (cmd.getName().equalsIgnoreCase("reg"))){
			if(AuthPlayers.contains(p)) {
				RegisterPlayer(p, args);
			}
			else p.sendMessage(ChatColor.RED + "You have already signed in");
		}
		if(cmd.getName().equalsIgnoreCase("login") || cmd.getName().equalsIgnoreCase("l")){
			if(AuthPlayers.contains(p)) {
				LoginPlayer(p, args);
			}
			else p.sendMessage(ChatColor.RED + "You have already signed in");
		}
		if(cmd.getName().equalsIgnoreCase("forgetsession") || cmd.getName().equalsIgnoreCase("fs")){
			ChangeLoginTime(p);
		}
		if(cmd.getName().equalsIgnoreCase("op") || cmd.getName().equalsIgnoreCase("deop")){
			//log("op toggled");
			onOPToggling(p, args);
		}
		if(cmd.getName().equalsIgnoreCase("reloadauthconfig")){
			loadConfig();
			sender.sendMessage(ChatColor.GREEN + "Successfully reloaded the AthorizationV2 config");
		}
//		if(cmd.getName().equalsIgnoreCase("reload") && args[0]!=null && args[0].equalsIgnoreCase("confirm")){
//			log(ChatColor.AQUA + "RELOAD");
//			Restarting = true;
//		}
		return false;
	}
	
	public void initFiles() {
		authDirectory = new File(Bukkit.getServer().getWorldContainer(), "files/authorization");
		inventoriesDirectory = new File(Bukkit.getServer().getWorldContainer(), "files/inventories");
		dataDirectory = new File(Bukkit.getServer().getWorldContainer(), "files/data");
		authDataDirectory = new File(dataDirectory, "authorizationData");
		configsDirectory = new File(Bukkit.getServer().getWorldContainer(), "configs");
		mainConfigDirectory = new File(configsDirectory, "Authorization");
		
		authDataDirectory.mkdirs();
		inventoriesDirectory.mkdirs();
		authDataDirectory.mkdirs();
		mainConfigDirectory.mkdirs();
	}
	
	public void copyHelpFile() {
		File f = new File(mainConfigDirectory, "confighelp.txt");
		f.getParentFile().mkdirs();
		if (!f.exists()) {
			InputStream is = getClass().getResourceAsStream("/confighelp.txt");
			OutputStream os = null;
			try {
				os = new FileOutputStream(f);
			} catch (FileNotFoundException e1) {
				e1.printStackTrace();
			}
			byte[] buffer = new byte[4096];
			int length;
			try {
				while ((length = is.read(buffer)) > 0) {
					os.write(buffer, 0, length);
				}
				os.close();
				is.close();
			} catch (IOException e) {
				if(ConfigData.logCatchedErrors) e.printStackTrace();
			}
		}
	}
	
	public void loadConfig() {
		File f = new File(mainConfigDirectory, "mainconfig.yml");
		f.getParentFile().mkdirs();
		
		YamlConfiguration c = YamlConfiguration.loadConfiguration(f);
		YamlConfiguration cOld = YamlConfiguration.loadConfiguration(f);
		//YamlConfiguration toDelete = YamlConfiguration.loadConfiguration(f);
		String currentKey = null;
		//String currentString;
//		boolean currentBoolean;
//		int currentInt;
//		int currentLong;

		try {
			currentKey = "log-catched-errors";
			//toDelete.set(currentKey, null);
			if (c.get(currentKey) == null) {
				c.set(currentKey, true);
			} else {
				// currentBoolean = (boolean) c.get(currentKey);
				// debug = currentBoolean;
				try {
					ConfigData.logCatchedErrors = (boolean) c.get(currentKey);
				} catch (ClassCastException e) {

				}
			}

			currentKey = "max-offline-time";
			if (c.get(currentKey) == null) {
				c.set(currentKey, 46400000);
			} else {
				// currentInt = (int) c.get(currentKey);
				// ConfigData.maxOfflineTime = currentInt;
				ConfigData.maxOfflineTime = (int) c.get(currentKey);
			}

			currentKey = "custom-join-message-enabled";
			if (c.get(currentKey) == null) {
				c.set(currentKey, true);
			} else {
				ConfigData.customJoinMessageEnabled = (boolean) c.get(currentKey);
			}

			currentKey = "custom-leave-message-enabled";
			if (c.get(currentKey) == null) {
				c.set(currentKey, true);
			} else {
				ConfigData.customLeaveMessageEnabled = (boolean) c.get(currentKey);
			}

			currentKey = "custom-join-message-text";
			if (c.get(currentKey) == null) {
				c.set(currentKey, "§f§lthe-player-name §7joined the game");
			} else {
				ConfigData.customJoinMessageText = (String) c.get(currentKey);
			}

			currentKey = "custom-leave-message-text";
			if (c.get(currentKey) == null) {
				c.set(currentKey, "§f§lthe-player-name §7left the game");
			} else {
				ConfigData.customLeaveMessageText = (String) c.get(currentKey);
			}
		} catch (Throwable t) {
			log("Error while reading \"" + currentKey + "\" from the config");
		}
		
		// DONE
		
		boolean same = true;
		
	    for(String key : c.getKeys(false)) {
	        if(!cOld.contains(key)) {
	        	same = false;
	        }
	    }
		
	    
		if (!same) {
			try {
				c.save(f);
			} catch (IOException e) {
				if(ConfigData.logCatchedErrors) e.printStackTrace();
			}
		}
	}
	
//	public int simpleStrToInt(String s) {
//		return
//	}
	
	public long strToLong(String s) {
		return strToLong(s, -2);
	}
	
	public long strToLong(String s, long defaultValue) {
		long result;
		try {
			result = Long.parseLong(s);
		}
		catch (NumberFormatException e)
		{
			result = defaultValue;
		}
		return result;
	}
	
	public int strToInt(String s) {
		return strToInt(s, -2);
	}
	
	public int strToInt(String s, int defaultValue) {
		int result;
		try {
			result = Integer.parseInt(s);
		}
		catch (NumberFormatException e)
		{
			result = -1;
		}
		return result;
	}

	//--------------------------------------------------------------------AUTHORIZATION--------------------------------------------------------------------------------------------
	
	@EventHandler
	public void OnMove (PlayerMoveEvent e) {
		//log(ChatColor.AQUA + "PlayerMoveEvent");
		if(e.isCancelled()) return;
		Player p = e.getPlayer();
//		Location pLoc = p.getLocation().clone();
//		if(pLoc.getBlock().getType().toString().toLowerCase().contains("portal")) {
//			return;
//		}
//		pLoc.add(0,1,0);
//		if(pLoc.getBlock().getType().toString().toLowerCase().contains("portal")) {
//			return;
//		}
		//log(ChatColor.AQUA + "" + AuthPlayers.contains(p));
		if(AuthPlayers.contains(p)) {
		Location portalLoc = IsBlockNearby(p, Material.NETHER_PORTAL, 2);
		if(portalLoc != null) {
			if(portalLoc.distance(p.getLocation())<=1.51) {
				if(Math.abs(portalLoc.getY()-p.getLocation().getY())<=2) {
					AuthPlayersLocs.put(p, p.getLocation());
					if(p.getGameMode().equals(GameMode.SURVIVAL) || p.getGameMode().equals(GameMode.ADVENTURE)) {
						p.setFlying(false);
						p.setAllowFlight(false);
					}
					//log(ChatColor.AQUA + "RETURN");
					return;
				}
			}
		}
		}
		if(AuthPlayers.contains(p)) {
			if(!e.getFrom().getWorld().equals(e.getTo().getWorld())) return;
			//e.setCancelled(true);
			Location loc = e.getTo().clone();
//			float y = e.getTo().getYaw();
//			float pt = e.getTo().getPitch();
//			p.teleport(loc);
//			p.getLocation().setYaw(y);
//			p.getLocation().setPitch(pt);
			p.setAllowFlight(true);
			p.setFlying(true);
			loc.setX(AuthPlayersLocs.get(p).getX());
			loc.setY(AuthPlayersLocs.get(p).getY());
			loc.setZ(AuthPlayersLocs.get(p).getZ());
			//log(ChatColor.AQUA + "TP");
			p.teleport(loc);
		}
	}

	public void PrepareJoinedPlayer(Player p) {
		for (Player p1 : Bukkit.getServer().getOnlinePlayers()) {
          p1.showPlayer(p);
          p.showPlayer(p1);
		}
		try {
			loadInventory(p);
		} catch (Exception e) {
			if(ConfigData.logCatchedErrors) e.printStackTrace();
		}
		sendJoinMessage(p);
		p.setGameMode(GameMode.SURVIVAL);
		if(p.getGameMode() != GameMode.CREATIVE && p.getGameMode() != GameMode.SPECTATOR) p.setAllowFlight(false);
		if(AuthPlayers.contains(p)) AuthPlayers.remove(p);
		if(AuthPlayersLocs.containsKey(p)) AuthPlayersLocs.remove(p);
		if(AuthAir.containsKey(p)) AuthAir.remove(p);
		p.setFlying(false);
		p.setAllowFlight(false);
		p.setCanPickupItems(true);
		setExpFromFile(p);
		setGamemodeFromFile(p);
		setHeldSlotFromFile(p);
		setFlyStateFromFile(p);
		saveAuthPlayerInFile(p);
		fixFlightAbility(p);
		
		removeAuthInFile(p);
		//saveOPInFile(p);
		//checkForPortalStuck(p);
		//log(p.getInventory().getHeldItemSlot() + "");
	}
	
	public void RegisterPlayer(Player p, String[] args)  {
		if (args.length < 2) {
			p.sendMessage(ChatColor.RED + "Incorrect command using, Try " + ChatColor.GOLD + "/register <password> <password>" + ChatColor.RED + ".");
		} else if (args[0].equals(args[1])) {
			FileOutputStream fos = null;
			props.clear();
			FileReader reader = null;
			try {
				File f = new File(authDirectory, p.getName() + ".properties");
				if (f.exists()) {
					try {
						reader = new FileReader(f);
					} catch (FileNotFoundException e) {
						if(ConfigData.logCatchedErrors) e.printStackTrace();
					}
					props.load(reader);
					reader.close();
				}
				if(!f.getParentFile().exists()) {
					f.getParentFile().mkdirs();
				}
				if (props.getProperty("password") == null) {
					props.setProperty("password", args[0]);
					props.setProperty("lastRealLoginTime", Long.toString(Calendar.getInstance().getTimeInMillis()));
					String s = p.getAddress().toString().split("/")[1].split(":")[0];
					if (s.endsWith("\\")) s.substring(0, s.length() - 1);
					props.setProperty("lastLoginAdress", s);
					props.setProperty("lastLoginPort", Integer.toString(p.getAddress().getPort()));
					try {
						int temp = 1;
						try {
							//f.createNewFile();
							fos = new FileOutputStream(f);
							props.store(fos, "");
							fos.close();
							p.sendTitle(ChatColor.ITALIC + "" + ChatColor.BOLD + "" + ChatColor.GREEN + "Now you registered!", ChatColor.ITALIC + "" + ChatColor.DARK_GREEN + "Welcome! Don't forget your password", 5, 40, 30);
							//if (AuthPlayers.contains(p)) AuthPlayers.remove(p);
							GiveOpIfNeeds(p);
//							saveNonLogin(p);
							PrepareJoinedPlayer(p);
						} catch (FileNotFoundException e) {
							if(ConfigData.logCatchedErrors) e.printStackTrace();
						}
					} catch (IOException e) {
						if(ConfigData.logCatchedErrors) e.printStackTrace();
					}
				} else {
					p.sendMessage(ChatColor.RED + "This account is already registered. You should use " + ChatColor.GOLD
							+ "/login <password>" + ChatColor.RED + ".");
				}
			} catch (Exception e) {
				if(ConfigData.logCatchedErrors) e.printStackTrace();
			}
		} else {
			p.sendMessage(ChatColor.RED + "The password which is first doesn't match the second one.");
		}
	}
	
	/*public void saveNonLogin(Player p) {
		if(true) {
			return;
		}
		if(!AuthPlayers.contains(p)) {
			try {
				saveInventory(p);
				//log(ChatColor.AQUA + "saved5");
			} catch (IOException e) {
				if(ConfigData.logCatchedErrors) e.printStackTrace();
			}
			saveOPInFile(p.getName());
			saveGamemodeInFile(p);
			//log("1");
			saveHeldSlotInFile(p);
			saveFlyStateInFile(p);
			saveAirInFile(p);
		}
		saveExpInFile(p);
	}*/
	
	public void LoginPlayer(Player p, String[] args) {
		if(args.length<1) {
			p.sendMessage(ChatColor.RED + "Incorrect command using, Try " + ChatColor.GOLD + "/login <password>"+ChatColor.RED+".");
			return;
		}
		FileOutputStream fos = null;
    	props.clear();
    	FileReader reader = null;
		try {
			File f = new File(authDirectory, p.getName()+".properties");
			if (f.exists()) {
				try {
					reader = new FileReader(f);
				} catch (FileNotFoundException e) {
					if(ConfigData.logCatchedErrors) e.printStackTrace();
				}
				props.load(reader);
				reader.close();
			}
			if(props.getProperty("password")!=null) {
				if(props.getProperty("password").equals(args[0])) {
					//and here
					props.setProperty("lastRealLoginTime", Long.toString(Calendar.getInstance().getTimeInMillis()));
					String s = p.getAddress().toString().split("/")[1].split(":")[0];
					if(s.endsWith("\\")) s.substring(0, s.length()-1);
					props.setProperty("lastLoginAdress", s);
					props.setProperty("lastLoginPort", Integer.toString(p.getAddress().getPort()));
					try {
						fos = new FileOutputStream(f);
						props.store(fos, "");
						fos.close();
						p.sendTitle(ChatColor.ITALIC + "" + ChatColor.BOLD + "" + ChatColor.GREEN + "You logged in!", ChatColor.ITALIC + "" + ChatColor.GRAY + "Welcome back!", 5, 20, 15);
						//if(AuthPlayers.contains(p)) AuthPlayers.remove(p);
						GiveOpIfNeeds(p);
//						saveNonLogin(p);
						PrepareJoinedPlayer(p);
						//p.setRemainingAir(100);
					} catch (FileNotFoundException e) {
						if(ConfigData.logCatchedErrors) e.printStackTrace();
					}
				}
				else {
					p.sendMessage(ChatColor.RED + "Incorrect password.");
					//log(AuthAttempts.get(p)+"");
					if(AuthAttempts.get(p)!=null && AuthAttempts.get(p).intValue()+1>=5) {
						p.kickPlayer("To many attempts!");
					}
					if(AuthAttempts.containsKey(p)) {
						AuthAttempts.put(p, AuthAttempts.get(p).intValue()+1);
					}
					else {
						AuthAttempts.put(p, 1);
					}
				}
			}
			else {
				p.sendMessage(ChatColor.RED + "This account is NOT registered. You should use " + ChatColor.GOLD + "/register <password> <password>"+ChatColor.RED+".");
			}
		} catch (IOException e) {
			if(ConfigData.logCatchedErrors) e.printStackTrace();
		}
	}
	
	public void ChangeLoginTime(Player p) {
//		if(!p.isOp()) {
//			p.sendMessage(ChatColor.RED + "You can't use this command.");
//			return;
//		}
		FileOutputStream fos = null;
    	props.clear();
    	FileReader reader = null;
		try {
			File f = new File(authDirectory, p.getName()+".properties");
			if (f.exists()) {
				try {
					reader = new FileReader(f);
				} catch (FileNotFoundException e) {
					if(ConfigData.logCatchedErrors) e.printStackTrace();
				}
				props.load(reader);
				reader.close();
			}
			if(props.getProperty("password")!=null) {
					//and here
					props.setProperty("lastRealLoginTime", "0");
					try {
						fos = new FileOutputStream(f);
						props.store(fos, "");
						fos.close();
						p.sendMessage(ChatColor.GREEN + "" + ChatColor.BOLD + "Successful");
					} catch (FileNotFoundException e) {
						if(ConfigData.logCatchedErrors) e.printStackTrace();
					}
			}
			else {
				p.sendMessage(ChatColor.RED + "Problem, no account (no password)");
			}
		} catch (IOException e) {
			if(ConfigData.logCatchedErrors) e.printStackTrace();
		}
	}
	
	public void saveInventory (Player p) throws IOException {
		YamlConfiguration c = new YamlConfiguration();
		c.set("inventory.armor", p.getInventory().getArmorContents());
		c.set("inventory.content", p.getInventory().getContents());
		c.save(new File(inventoriesDirectory, p.getName()+".yml"));
	}

	public void loadInventory(Player p) throws IOException {
		//File file = new File(inventoriesDirectory, p.getName()+".yml");
		YamlConfiguration c = null;
		ItemStack[] content = null;
		c = YamlConfiguration.loadConfiguration(new File(inventoriesDirectory, p.getName()+".yml"));
		try{
			content = ((List<ItemStack>) c.get("inventory.armor")).toArray(new ItemStack[0]);
		}
		catch(NullPointerException e1) {
			//ignore
		}
		if(content!=null) p.getInventory().setArmorContents(content);
		try{
			content = ((List<ItemStack>) c.get("inventory.content")).toArray(new ItemStack[0]);
		}
		catch(NullPointerException e1) {
			//ignore
		}
		if(content!=null) p.getInventory().setContents(content);
	}

	@EventHandler(priority = EventPriority.LOWEST)
	public void AllowPlayerConnect (AsyncPlayerPreLoginEvent e) {
		log("AsyncPlayerPreLoginEvent");
		FileReader reader = null;
		props.clear();
		File f = new File(authDirectory, e.getName() + ".properties");
		if (f.exists()) {
			try {
				reader = new FileReader(f);
				try {
					props.load(reader);
					reader.close();
				} catch (IOException e1) {
					e1.printStackTrace();
				}
			} catch (FileNotFoundException e1) {
				e1.printStackTrace();
			}
		}
		if(Bukkit.getServer().getPlayer(e.getName()) != null) { //если такой игрок уже онлайн
			if(props.getProperty("lastLoginAdress")!=null) {//если он зареган
				String s = e.getAddress().toString().split("/")[1].split(":")[0];
				if(s.endsWith("\\")) s.substring(0, s.length()-1);
//				log(ChatColor.AQUA + "s = " + s + " (AsyncPlayerPreLoginEvent)");
//				log(ChatColor.AQUA + "lastLoginAdress = " + props.getProperty("lastLoginAdress") + " (AsyncPlayerPreLoginEvent)");
//				log(ChatColor.AQUA + "" + props.getProperty("lastLoginAdress").equals(s));
				if(!props.getProperty("lastLoginAdress").equals(s)) {
					//если уже играющий игрок под IP, который последний раз правильно ввёл пароль
					e.disallow(Result.KICK_OTHER, ChatColor.RED + "This player is already on the server");
				}
			}
			else {
				//он не загреган, но всё равно выкидывать уже играющего (в лобби регистрации) нельзя
				e.disallow(Result.KICK_OTHER, ChatColor.RED + "This player is already on the server");
			}
		}
	}
	
	@EventHandler
	public void EPlayerQuit(PlayerQuitEvent e) {
		Player p = e.getPlayer();
		//log(e.getQuitMessage());
		onPlayerLeave(p, true);
		if (e.getPlayer().isBanned()) {
			//nothing yet
		}
		e.setQuitMessage("");
	}
	
	@EventHandler
	public void EPlayerKick(PlayerKickEvent e) {
		Player p = e.getPlayer();
		//onPlayerLeave(p, false);
	}

	public void onPlayerLeave(Player p, boolean sendMssage) {
//		log(ChatColor.AQUA + "" +	 Restarting);
//		if(AuthPlayers.contains(p)) {
//			try {
//				loadInventory(p);
//			} catch (IOException e) {
//				if(ConfigData.logCatchedErrors) e.printStackTrace();
//			}
//		}
		
//		log(AuthPlayers.toString());
		
		if(!AuthPlayers.contains(p) && Restarting == false) {
			saveOPInFile(p.getName());
			saveGamemodeInFile(p);
			//log("2");
			saveHeldSlotInFile(p);
			saveFlyStateInFile(p);
			if(sendMssage) {
				sendLeaveMessage(p);
			}
			saveAirInFile(p);
		}
		saveExpInFile(p);
		
		//PlayersBreakers.remove(p.getName());
		
		//PlayersFlyInSurvival.remove(p.getName());
		if(InternalDebug) log("onLeave: auth: " + AuthPlayers.toString());
		if(!AuthPlayers.contains(p)) { // && !p.getInventory().isEmpty() //clear1
			try {
				saveInventory(p);
				if(InternalDebug) log(ChatColor.AQUA + "saved2");
			} catch (Exception e1) {
				e1.printStackTrace();
			}
		}
		//e.setQuitMessage(ChatColor.WHITE + p.getName() + " left the server.");
//		for(Player player : p.getWorld().getPlayers()) {
//			//player.sendMessage(ChatColor.WHITE + p.getName() + " left the server.");
//		}
		AuthPlayers.remove(p);
		if(AuthPlayersLocs.containsKey(p)) AuthPlayersLocs.remove(p);
		
		removeAuthInFile(p);
	}

	public void GiveOpIfNeeds(Player p) {
    	File f = new File(authDirectory, p.getName() + ".properties");
		FileReader reader = null;
		props.clear();
		if (f.exists()) {
			try {
				reader = new FileReader(f);
				try {
					props.load(reader);
					reader.close();
					if(props.getProperty("isOP")!=null && props.getProperty("isOP").equals("1")) {
						p.setOp(true);
					}
					props.setProperty("lastLoginTime", Long.toString(Calendar.getInstance().getTimeInMillis()));
					FileOutputStream fos = new FileOutputStream(f);
					props.store(fos, "");
					fos.close();
				} catch (IOException e1) {
					e1.printStackTrace();
				}
			} catch (FileNotFoundException e1) {
				e1.printStackTrace();
			}
		}
    }
	
	@EventHandler(priority=EventPriority.LOW)
	public void EPlayerJoin(PlayerJoinEvent e) throws IOException {
		Player p = e.getPlayer();
		e.setJoinMessage("");
		fixFirstTimePlayer(p);
		p.setOp(false);
		FileReader reader = null;
		props.clear();
		File f = new File(authDirectory, p.getName() + ".properties");
		if (f.exists()) {
			try {
				reader = new FileReader(f);
				try {
					props.load(reader);
					reader.close();
				} catch (IOException e1) {
					e1.printStackTrace();
				}
			} catch (FileNotFoundException e1) {
				e1.printStackTrace();
			}
		}
		if(InternalDebug) log(f.exists() && props.getProperty("password")!=null);
		if(f.exists() && props.getProperty("password")!=null) {
			String s = p.getAddress().toString().split("/")[1].split(":")[0];
			if(InternalDebug) log(s.endsWith("\\"));
			if(s.endsWith("\\")) s.substring(0, s.length()-1);
			if(InternalDebug) log(props.getProperty("lastLoginAdress").equals(s));
			if(props.getProperty("lastLoginAdress").equals(s)) {
				if(InternalDebug) log(Math.abs(Integer.parseInt(props.getProperty("lastLoginPort"))-p.getAddress().getPort()) < 20000);
				if(Math.abs(Integer.parseInt(props.getProperty("lastLoginPort"))-p.getAddress().getPort()) < 20000) {
					if(InternalDebug) log(Math.abs(Long.parseLong(props.getProperty("lastRealLoginTime"))-Calendar.getInstance().getTimeInMillis()) < ConfigData.maxOfflineTime);
					if(Math.abs(Long.parseLong(props.getProperty("lastRealLoginTime"))-Calendar.getInstance().getTimeInMillis()) < ConfigData.maxOfflineTime) {
						p.sendTitle("", "");
						p.sendMessage(ChatColor.GREEN + "You have already logged in before.");
						GiveOpIfNeeds(p);
						PrepareJoinedPlayer(p);
						return;
					}
				}
			}
		}
		if(p.getVehicle()!=null) {
			p.leaveVehicle();
		}
		try {
			if(!p.getInventory().isEmpty()) {
				saveInventory(p);
				if(InternalDebug) log(ChatColor.AQUA + "saved3");
			}
		} catch (Exception e1) {
			e1.printStackTrace();
		}
		
		p.getInventory().clear();
		AuthPlayers.add(p);
		if(InternalDebug) log("added to auth");
		//evt.getPlayer().setCollidable(false);
		//evt.getPlayer().spigot().setCollidesWithEntities(false);
		for (Player p1 : Bukkit.getServer().getOnlinePlayers()) {
			p1.hidePlayer(p);
	        p.hidePlayer(p1);
		}
		if(props.getProperty("password")==null) {
			p.sendTitle(ChatColor.ITALIC + "" + ChatColor.BOLD + "" + ChatColor.GOLD + "You need to register", ChatColor.ITALIC + "" + ChatColor.GRAY + "/register <password> <password>", 5, 99999, 30);
			addAuthInFile(p);
		}
		else {
			p.sendTitle(ChatColor.ITALIC + "" + ChatColor.BOLD + "" + ChatColor.GOLD + "You need to login", ChatColor.ITALIC + "" + ChatColor.GRAY + "/login <password>", 5, 99999, 30);
			addAuthInFile(p);
		}
		p.setGameMode(GameMode.ADVENTURE);
		p.setOp(false);
		p.setAllowFlight(true);
		p.setFlying(true);
		p.setCanPickupItems(false);
		//AuthPlayers.add(p);
		saveAuthPlayerInFile(p);
		AuthPlayersLocs.put(p, p.getLocation());
		p.setGameMode(GameMode.ADVENTURE);
		AuthStartCountdown(p, 30);
		
		saveOPInFile(p.getName());
		saveGamemodeInFile(p);
		saveHeldSlotInFile(p);
		saveFlyStateInFile(p);
		//sendLeaveMessage(p);
		saveAirInFile(p);
		saveExpInFile(p);
		
		//PlayersBreakers.remove(p.getName());
		
		//PlayersFlyInSurvival.remove(p.getName());
		
		//log(p.getInventory().getContents().length + "");
		
		
	}

	public void AuthStartCountdown(Player p, int MaxSeconds) {
    	Long timeJoined = Calendar.getInstance().getTimeInMillis();
    	new BukkitRunnable() {
    		@Override
    		public void run() {
    			if(p==null || !p.isOnline() || (!AuthPlayers.contains(p))) {
    				cancel();
    			}
    			else {
    				if(MaxSeconds - ((Calendar.getInstance().getTimeInMillis()-timeJoined)/1000) > 0){
    					float persent;
    					//log((Calendar.getInstance().getTimeInMillis()-timeJoined)/1000 + "");
						persent = (float) (MaxSeconds - (Calendar.getInstance().getTimeInMillis()-timeJoined)/1000) / MaxSeconds; 
						if(persent > 1 || persent < 0) persent = (float) 0.999;
						p.setLevel(Integer.parseInt(Long.toString(MaxSeconds - ((Calendar.getInstance().getTimeInMillis())-timeJoined)/1000)));
						p.setExp(persent);
    				}
    				else {
    					p.kickPlayer("You were not in time while entering password");
    				}
    			}
    		}
    	}.runTaskTimer(this, 0, 20); // or Plugin variable instead of "this"
    }
	
	public void onOPToggling(Player p, String[] args) { //deprecated
		String op = "0";
		if(p.isOp()) op = "1";
		FileOutputStream fos = null;
	    props.clear();
	    FileReader reader = null;
		try {
			File f = new File(authDirectory, p.getName()+".properties");
			if (f.exists()) {
				try {
					reader = new FileReader(f);
				} catch (FileNotFoundException e) {
					if(ConfigData.logCatchedErrors) e.printStackTrace();
				}
				props.load(reader);
				reader.close();
			}
			if(props.getProperty("password")!=null && props.getProperty("isOP")!=op) {
				//and here
				props.setProperty("isOP", op);
				try {
					fos = new FileOutputStream(f);
					props.store(fos, "");
					fos.close();
					p.sendMessage(ChatColor.GREEN + "Successful");
				} catch (FileNotFoundException e) {
					if(ConfigData.logCatchedErrors) e.printStackTrace();
				}
			}
			else {
				log(ChatColor.RED + "Problem, no account (no password)");
			}
		} catch (IOException e) {
			if(ConfigData.logCatchedErrors) e.printStackTrace();
		}
	}
	
	@EventHandler
    public void ChangeProfileOp(PlayerCommandPreprocessEvent e) {
    	if(e.getPlayer().isOp()) {
    	int Op = 0;
    	if (e.getMessage().contains("/op") && e.getMessage().length()>4) {
    		Op = 1;
    	}
    	if (e.getMessage().contains("/deop") && e.getMessage().length()>6) {
    		Op = 2;
    	}
    	if(Op!=0) {
    		String p = "";
			if(Op==1) p = e.getMessage().substring(4);
			if(Op==2) p = e.getMessage().substring(6);
    		if(p != "") {
    			if(e.getPlayer().isOp()) {
    				saveOPInFile(p);
    			}
    		}
    	}
    	}
    }
	
	public void saveOPInFile(String p) {
		saveOPInFile(Bukkit.getServer().getPlayerExact(p));
	}
	
	public void saveOPInFile(Player p) {
		//if(Debug) log(p+"");
		if(AuthPlayers.contains(p)) {
			return;
		}
		//log("saveOPInFile");
		FileOutputStream fos = null;
		props.clear();
		FileReader reader = null;
		String op = "0";
		//log(p+"");
		if(p.isOp()) op = "1";
		
		try {
			File f = new File(authDirectory, p.getName()+".properties");
			checkFileExistence(f);
			if (f.exists()) {
				try {
					reader = new FileReader(f);
				} catch (FileNotFoundException e1) {
					e1.printStackTrace();
				}
				if(reader==null) {
					log("Some error while saving Op state in file");
					return;
				}
				props.load(reader);
				reader.close();
				if(props.getProperty("isOP") != null && props.getProperty("isOP").equals(op)){
					return;
				}
			}
			if(props.size() == 1 && op == "0")
			{
				f.delete();
				return;
			}
			props.setProperty("isOP", op);
			fos = new FileOutputStream(f);
			props.store(fos, "");
		} catch (IOException e1) {
			e1.printStackTrace();
			log.warning("Cannot find player's folder");
		} finally {
			if(fos != null) {
				try {
					fos.close();
				} catch (IOException e2) {
					e2.printStackTrace();
				}
			}
		}
	}
	
	public void checkFileExistence(String f) {
		File f1 = new File(f);
		if(!f1.getParentFile().exists()) {
			f1.getParentFile().mkdirs();
		}
	}
	
	public void checkFileExistence(File f) {
		if(!f.getParentFile().exists()) {
			f.getParentFile().mkdirs();
		}
	}
	
	public void saveExpInFile(Player p) {
		if(AuthPlayers.contains(p)) return;
		FileOutputStream fos = null;
		props.clear();
		FileReader reader = null;
		try {
			File f = new File(authDirectory, p.getName()+".properties");
			checkFileExistence(f);
			if (f.exists()) {
				try {
					reader = new FileReader(f);
				} catch (FileNotFoundException e1) {
					e1.printStackTrace();
				}
				props.load(reader);
				reader.close();
				int same = 0;
				if(props.getProperty("expLevel") != null){
					if(props.getProperty("expLevel").equals(Integer.toString(p.getLevel()))) {
						same++;
					}
				}
				else if(props.getProperty("expValue") != null){
					if(props.getProperty("expValue").equals(Float.toString(p.getExp()))) {
						same++;
					}
				}
				if(same==2) {
					return;
				}
			}
			props.setProperty("expLevel", Integer.toString(p.getLevel()));
			props.setProperty("expValue", Float.toString(p.getExp()));
			fos = new FileOutputStream(f);
			props.store(fos, "");
		} catch (IOException e1) {
			e1.printStackTrace();
		} finally {
			if(fos != null) {
				try {
					fos.close();
				} catch (IOException e2) {
					e2.printStackTrace();
				}
			}
		}
	}
	
	public void setExpFromFile(Player p) {
		FileOutputStream fos = null;
		props.clear();
		FileReader reader = null;
		try {
			File f = new File(authDirectory, p.getName()+".properties");
			if (f.exists()) {
				try {
					reader = new FileReader(f);
				} catch (FileNotFoundException e1) {
					e1.printStackTrace();
				}
				props.load(reader);
				reader.close();
//				try {
					if(props.getProperty("expLevel") != null && props.getProperty("expValue") != null){
						p.setLevel(Integer.parseInt(props.getProperty("expLevel")));
						p.setExp(Float.parseFloat(props.getProperty("expValue")));
					}
					else {
						p.setLevel(0);
						p.setExp(0);
					}
//				}
//				catch(IllegalArgumentException e) {
//					if(ConfigData.logCatchedErrors) e.printStackTrace();
//				}
			}
		} catch (IOException e1) {
			e1.printStackTrace();
		} finally {
			if(fos != null) {
				try {
					fos.close();
				} catch (IOException e2) {
					e2.printStackTrace();
				}
			}
		}
	}

	public void saveGamemodeInFile(Player p) {
		if(AuthPlayers.contains(p)) return;
		FileOutputStream fos = null;
		props.clear();
		FileReader reader = null;
		try {
			File f = new File(authDirectory, p.getName()+".properties");
			checkFileExistence(f);
			if (f.exists()) {
				try {
					reader = new FileReader(f);
				} catch (FileNotFoundException e1) {
					e1.printStackTrace();
				}
				props.load(reader);
				reader.close();
				if(props.getProperty("gamemode") != null && props.getProperty("gamemode").equals(p.getGameMode().toString())){
					return;
				}
			}
			props.setProperty("gamemode", p.getGameMode().toString());
			fos = new FileOutputStream(f);
			props.store(fos, "");
		} catch (IOException e1) {
			e1.printStackTrace();
		} finally {
			if(fos != null) {
				try {
					fos.close();
				} catch (IOException e2) {
					e2.printStackTrace();
				}
			}
		}
	}
	
	public void setGamemodeFromFile(Player p) {
		FileOutputStream fos = null;
		props.clear();
		FileReader reader = null;
		try {
			File f = new File(authDirectory, p.getName()+".properties");
			if (f.exists()) {
				try {
					reader = new FileReader(f);
				} catch (FileNotFoundException e1) {
					e1.printStackTrace();
				}
				props.load(reader);
				reader.close();
				if(props.getProperty("gamemode") != null){
					String g = props.getProperty("gamemode");
					if(g.equalsIgnoreCase("survival")) {
						p.setGameMode(GameMode.SURVIVAL);
					}
					if(g.equalsIgnoreCase("creative")) {
						p.setGameMode(GameMode.CREATIVE);
					}
					if(g.equalsIgnoreCase("adventure")) {
						p.setGameMode(GameMode.ADVENTURE);
					}
					if(g.equalsIgnoreCase("spectator")) {
						p.setGameMode(GameMode.SPECTATOR);
					}
				}
			}
		} catch (IOException e1) {
			e1.printStackTrace();
		} finally {
			if(fos != null) {
				try {
					fos.close();
				} catch (IOException e2) {
					e2.printStackTrace();
				}
			}
		}
	}
	
	public void saveHeldSlotInFile(Player p){
		if(AuthPlayers.contains(p)) {
			return;
		}
		int slot = p.getInventory().getHeldItemSlot();
		saveAnythingInFile(p, "heldSlot", slot + "", authDirectory);
	}
	
	public void setHeldSlotFromFile(Player p) {
		if(AuthPlayers.contains(p)) {
			return;
		}
		String slot = getAnythingFromFile(p, "heldSlot", authDirectory);
		if(slot == null) return;
		p.getInventory().setHeldItemSlot(Integer.parseInt(slot));
	}
	
	public void saveFlyStateInFile(Player p){
		if(AuthPlayers.contains(p)) {
			return;
		}
		boolean fly = p.isFlying();
		saveAnythingInFile(p, "flyState", fly + "", authDirectory);
	}
	
	public void setFlyStateFromFile(Player p) {
		if(AuthPlayers.contains(p)) {
			return;
		}
		String fly = getAnythingFromFile(p, "flyState", authDirectory);
		if(fly == null) return;
		if(Boolean.parseBoolean(fly) == true && ((p.getGameMode().equals(GameMode.CREATIVE)) || (p.getGameMode().equals(GameMode.SPECTATOR)))) {
			p.setAllowFlight(true);
			p.setFlying(true);
		}
		else {
			p.setFlying(false);
			p.setAllowFlight(false);
		}
	}
	
	public void saveAuthPlayerInFile(Player p){
		boolean auth = AuthPlayers.contains(p);
		saveAnythingInFile(p, "auth", Boolean.toString(auth), authDirectory);
		//log("saveAuthPlayerInFile");
	}
	
	public boolean getAuthPlayerFromFile(Player p) {
		String auth = getAnythingFromFile(p, "auth", authDirectory);
		if(auth == null) return false;
		if(Boolean.parseBoolean(auth) == true){
			return true;
		}
		return false;
	}
	
	public void saveAirInFile(Player p){
		int air = p.getRemainingAir();
		saveAnythingInFile(p, "air", Integer.toString(air), authDirectory);
		//log("saveAirInFile");
	}
	
	public static int getAirFromFile(Player p) {
		String air = getAnythingFromFile(p, "air", authDirectory);
		//log(air);
		if(air != null) return Integer.parseInt(air);
		return 300;
	}
	
	public void saveAnythingInFile(Player p, String name, String value, File dir) {
		if(AuthPlayers.contains(p) && name != "auth") return;
		FileOutputStream fos = null;
		props.clear();
		FileReader reader = null;
		try {
			File f = new File(dir, p.getName()+".properties");
			checkFileExistence(f);
			if (f.exists()) {
				try {
					reader = new FileReader(f);
				} catch (FileNotFoundException e1) {
					//e1.printStackTrace();
					log("Some error because some Auth file was not file, ignoring it");
					return;
				}
				props.load(reader);
				reader.close();
				if(props.getProperty(name) != null && props.getProperty(name).equals(value)){
					return;
				}
			}
			props.setProperty(name, value);
			fos = new FileOutputStream(f);
			props.store(fos, "");
		} catch (IOException e1) {
			e1.printStackTrace();
		} finally {
			if(fos != null) {
				try {
					fos.close();
				} catch (IOException e2) {
					e2.printStackTrace();
				}
			}
		}
	}
	
	public static String getAnythingFromFile(Player p, String name, File dir) {
		FileOutputStream fos = null;
		props.clear();
		FileReader reader = null;
		try {
			File f = new File(dir, p.getName()+".properties");
			if (f.exists()) {
				try {
					reader = new FileReader(f);
				} catch (FileNotFoundException e1) {
					e1.printStackTrace();
				}
				props.load(reader);
				reader.close();
				return (props.getProperty(name));
			}
		} catch (IOException e1) {
			e1.printStackTrace();
		} finally {
			if(fos != null) {
				try {
					fos.close();
				} catch (IOException e2) {
					e2.printStackTrace();
				}
			}
		}
		return null;
	}
	
	public void fixFirstTimePlayer(Player p) {
		String Level = getAnythingFromFile(p, "expLevel", authDirectory);
		String Exp = getAnythingFromFile(p, "expValue", authDirectory);
		if(Level==null || Exp==null) {
			saveExpInFile(p);
		}
		YamlConfiguration c = null;
		ItemStack[] content1 = null;
		ItemStack[] content2 = null;
		c = YamlConfiguration.loadConfiguration(new File(inventoriesDirectory, p.getName()+".yml"));
		try{
			content1 = ((List<ItemStack>) c.get("inventory.armor")).toArray(new ItemStack[0]);
		}
		catch(NullPointerException e1) {
			//ignore
		}
		try{
			content2 = ((List<ItemStack>) c.get("inventory.content")).toArray(new ItemStack[0]);
		}
		catch(NullPointerException e1) {
			//ignore
		}
		if(content1==null && content2==null) {
			try {
				saveInventory(p);
				if(InternalDebug) log(ChatColor.AQUA + "saved4");
			} catch (IOException e) {
				//if(ConfigData.logCatchedErrors) e.printStackTrace();
			}
		}
		String isOP = getAnythingFromFile(p, "isOP", authDirectory);
		if(isOP==null) {
			saveOPInFile(p.getName());
		}
		String gamemode = getAnythingFromFile(p, "gamemode", authDirectory);
		if(gamemode==null) {
			saveGamemodeInFile(p);
		}
		String flyState = getAnythingFromFile(p, "flyState", authDirectory);
		if(flyState==null) {
			saveFlyStateInFile(p);
		}
		String heldSlot = getAnythingFromFile(p, "heldSlot", authDirectory);
		if(heldSlot==null) {
			saveHeldSlotInFile(p);
		}
	}
	
	//-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------
	
	public Location IsBlockNearby(Player p, Material material, int distanse) {
        ArrayList<Location> temp = new ArrayList<Location>();
        ArrayList<Double> dis = new ArrayList<Double>();
 
        Location loc = new Location(p.getWorld(), p.getLocation().getBlock().getX()-distanse, p.getLocation().getBlock().getY()-distanse, p.getLocation().getBlock().getZ()-distanse);
        //p.sendMessage("Drop event work");//For debug
        //p.sendMessage(loc.getX() + " " + loc.getZ());
        for(int i=1; i <= 2*distanse+1 ; i++) {
            for(int j =1; j <= 2*distanse+1 ; j++ ) {
            	for(int k =1; k <= 2*distanse+1 ; k++ ) {
            		//Bukkit.getServer().broadcastMessage(loc.getX() + " " + loc.getY() + " " + loc.getZ());
            		//Bukkit.getServer().broadcastMessage(loc.getBlock().getType().toString());
                	if(loc.getBlock().getType() == material) {
                		//Bukkit.getServer().broadcastMessage("Latern: " + loc);
                    	temp.add(loc.getBlock().getLocation());
                	}
            		loc.setX(loc.getX()+1);
            	}
            	loc.setX(p.getLocation().getBlock().getX()-distanse);
            	loc.setY(loc.getY()+1);
            }
            loc.setY(p.getLocation().getBlock().getY()-distanse);
        	loc.setZ(loc.getZ()+1);
        }
        for(int i = 0; i<temp.size(); i++) {
        	dis.add(temp.get(i).distance(p.getLocation()));
        }
        Collections.sort(dis);
        for(int i = 0; i<temp.size(); i++) {
        	if(temp.get(i).distance(p.getLocation()) == dis.get(0).doubleValue()) {
        		//p.sendMessage(temp.get(i) + "");
        		temp.get(i).add(0.5, 0, 0.5);
        		return temp.get(i);
        	}
        }
        //p.sendMessage("Temp :" + temp);//For debug
        //p.sendMessage("Dis :" + dis);//For debug
        //p.spawnParticle(Particle.BLOCK_CRACK, new Location(loc.getWorld(), loc.getX(), loc.getY(), loc.getZ()), 100, blood.createBlockData());
        return null;
	}
	
	public void fixFlightAbility(Player p) {
		if(p.getGameMode().equals(GameMode.CREATIVE) || p.getGameMode().equals(GameMode.SPECTATOR)) {
			p.setAllowFlight(true);
		}
	}
	
	public void sendJoinMessage(Player p) {
		//replaceAll("&", "§");
		String s = null;
		if(ConfigData.customJoinMessageEnabled) {
			s = ConfigData.customJoinMessageText.replaceAll("&", "§").replaceAll("the-player-name", p.getName());
		}
		else {
			s = ChatColor.YELLOW + p.getName() + " joined the game";
		}
		for (Player player : Bukkit.getServer().getOnlinePlayers()) {
			player.sendMessage(s);
		}
		log(s);
	}
	
	public void sendLeaveMessage(Player p) {
		String s = null;
		if(ConfigData.customLeaveMessageEnabled) {
			s = ConfigData.customLeaveMessageText.replaceAll("&", "§").replaceAll("the-player-name", p.getName());
		}
		else {
			s = ChatColor.YELLOW + p.getName() + " left the game";
		}
		//ChatColor.WHITE + "" + ChatColor.BOLD + p.getName() + ChatColor.GRAY + " left the game";
		for (Player player : Bukkit.getServer().getOnlinePlayers()) {
			player.sendMessage(s);
		}
		log(s);
	}

	public void addAuthInFile(Player p) {
		try {
			addPlayerToAuthFile(p.getName());
		} catch (IOException e) {
			if(ConfigData.logCatchedErrors) e.printStackTrace();
		}
	}
	
	public void removeAuthInFile(Player p) {
		try {
			removePlayerFromAuthFile(p.getName());
		} catch (IOException e) {
			if(ConfigData.logCatchedErrors) e.printStackTrace();
		}
	}
	
	public void addPlayerToAuthFile(String name) throws IOException {
		if(Bukkit.getServer().getPlayer(name) == null) {
			//log(ChatColor.GRAY + "[Warning] The player is not online.");
			return;
		}
		Player p = Bukkit.getServer().getPlayer(name);
		if(p!=null && ifPlayerIsAddedinAuthFile(p) == true) {
			//log(ChatColor.GRAY + "[Warning] The player is already added.");
			return;
		}
		
		YamlConfiguration c = new YamlConfiguration();
		String[] players = getFileAuthPlayers();
		ArrayList<String> list = new ArrayList<>();
		if(players!=null) {
			for (int i = 0; i<players.length; i++) {
				list.add(players[i]);
			}
		}
		if(p!=null && !list.contains(p.getName())) {
			list.add(p.getName());
		}
		c.set("playersinauth", list);
		c.save(new File(authDataDirectory, "authInfo.yml"));
		//log(ChatColor.GREEN + "Saved info.");
	}
	
	public void removePlayerFromAuthFile(String name) throws IOException {
		if(Bukkit.getServer().getPlayer(name) == null) {
			//log(ChatColor.GRAY + "[Warning] The player is not online.");
			return;
		}
		Player p = Bukkit.getServer().getPlayer(name);
		if(p!=null && ifPlayerIsAddedinAuthFile(p) == false) {
			//log(ChatColor.GRAY + "[Warning] The player is already removed.");
			return;
		}
		
		YamlConfiguration c = new YamlConfiguration();
		String[] players = getFileAuthPlayers();
		ArrayList<String> list = new ArrayList<String>();
		if(players!=null) {
			for (int i = 0; i<players.length; i++) {
				list.add(players[i]);
			}
		}
		if(p!=null && list.contains(p.getName())) {
			list.remove(p.getName());
		}
		c.set("playersinauth", list);
		c.save(new File(authDataDirectory, "authInfo.yml"));
		//log(ChatColor.GREEN + "Saved info.");
	}
	
	public String[] getFileAuthPlayers() throws IOException {
		YamlConfiguration c = null;
		String[] content = null;
		c = YamlConfiguration.loadConfiguration(new File(authDataDirectory, "authInfo.yml"));
		try{
			content = ((List<String>) c.get("playersinauth")).toArray(new String[0]);
		}
		catch(NullPointerException e1) {
			//ignore
		}
		return content;
	}
	
	public boolean ifPlayerIsAddedinAuthFile(Player p) throws IOException{
		String[] players = getFileAuthPlayers();
		ArrayList<String> list = new ArrayList<>();
		if(players!=null) {
			for (int i = 0; i<players.length; i++) {
				if(players[i].equals(p.getName())) {
					return true;
				}
			}
		}
		return false;
	}
	
	public void resetAuthListFile() {
		YamlConfiguration c = new YamlConfiguration();
		try {
			c.save(new File(authDataDirectory, "authInfo.yml"));
		} catch (IOException e) {
			if(ConfigData.logCatchedErrors) e.printStackTrace();
		}
		log(ChatColor.WHITE + "Auth list has been reseted");
	}
	
	public void log(String s) {
		Bukkit.getConsoleSender().sendMessage(s);
	}
	
	public void log(int s) {
		log(s + "");
	}
	
	public void log(boolean s) {
		log(s + "");
	}
	
	public void log(double s) {
		log(s + "");
	}
	
	public void log(float s) {
		log(s + "");
	}
	
	public void log(char s) {
		log(s + "");
	}
	
	public void log(short s) {
		log(s + "");
	}
	
	public void main(String[] args) { // tests
		File mainConfigDirectory = new File("G:\\SERVERS\\Dev Server 1.16.5\\configs\\AuthorizationV2");

		File f = new File(mainConfigDirectory, "mainconfig.yml");
		f.getParentFile().mkdirs();
		if (f.exists()) {
			YamlConfiguration c = YamlConfiguration.loadConfiguration(f);
		} else {
			try {
				f.createNewFile();
			} catch (IOException e2) {
				e2.printStackTrace();
			}
			Path from = null;
			try {
				from = Paths.get(Main.class.getResource("/mainconfig.yml").toURI());
			} catch (URISyntaxException e1) {
				e1.printStackTrace();
			}
			// Paths.get("/mainconfig.yml");
			Path to = f.toPath();
			try {
				Files.copy(from, to, StandardCopyOption.REPLACE_EXISTING);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
}