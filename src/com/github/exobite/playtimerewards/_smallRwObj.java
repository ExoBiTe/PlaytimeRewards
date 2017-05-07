package com.github.exobite.playtimerewards;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public class _smallRwObj implements RewardObject {
	
public boolean fullVersion;
	
	public String internalName;
	public String Name;
	public String Message;
	public List<String> playerCommands;
	public List<String> consoleCommands;
	public long playTimeNeeded;
	public boolean globalNotification;
	public boolean playerNotification;
	private customItem item;
	
	public boolean itemReward;
	public boolean pCmd;
	public boolean cCmd;
	public boolean msg;
	public boolean hasSound;
	public boolean hasParticle;
	public boolean waitFirst;
	
	public boolean hasLoop;
	
	public Inventory ItemStorage;
	public boolean itemHasChanged;
	
	public CountType loopType;
	
	public String permission;
	public long minPlaytime;
	public long maxPlaytime;
	
	public boolean needPermission;
	public boolean needMinPlaytime;
	public boolean needMaxPlaytime;
	
	public _smallRwObj(CountType Count, String internalName, String Name, String Message, List<String> playerCommand, List<String> consoleCommand, long playtimeNeeded, boolean globalNotification, boolean playerNotification){
		this.fullVersion = false;
		this.internalName = internalName;
		this.Name = ChatColor.translateAlternateColorCodes(CountMain.colorChar, Name);
		this.Message = ChatColor.translateAlternateColorCodes(CountMain.colorChar, Message);
		this.playerCommands = playerCommand;
		this.consoleCommands = consoleCommand;
		this.playTimeNeeded = playtimeNeeded*1000;	//Seconds*1000 = ms
		this.globalNotification = globalNotification;
		this.playerNotification = playerNotification;
		this.loopType = Count;
		itemReward = false;
		if(this.playerCommands==null || this.playerCommands.contains(null)) {pCmd = false;} else {pCmd = true;}
		if(this.consoleCommands==null|| this.consoleCommands.contains(null)) {cCmd = false;} else {cCmd = true;}
		if(this.Message==null) {msg = false;} else {msg = true;}
		if(this.Message.equals("")) {msg = false;} else {msg = true;}
		
		if(CountMain.RewardList.containsKey(internalName)) return;
		CountMain.RewardList.put(internalName, this);
		CountMain.RewardLongs.put(internalName, playTimeNeeded);
		
		getItem();
		
		ItemStorage = Bukkit.createInventory(null, 27, internalName);
		for(int i=0;i<27;i++){
			if(i!=13){
				ItemStorage.setItem(i, CountMain.emptyItem);
			}else{
				if(itemReward) ItemStorage.setItem(i, item.is);
			}
		}
		CountMain.guis.add(ItemStorage);
		itemHasChanged = false;
	}
	
	public void setLoop(long minPlaytime, long maxPlaytime, String Permission, boolean waitFirst){
		hasLoop = true;
		this.minPlaytime = minPlaytime;
		this.maxPlaytime = maxPlaytime;
		this.permission = Permission;
		if(permission == null){
			needPermission = false;
		}else if(permission.equals("")){
			needPermission = false;
		}else{
			needPermission = true;
		}
		if(this.maxPlaytime <= 0){
			needMaxPlaytime = false;
		}else{
			needMaxPlaytime = true;
		}
		if(this.minPlaytime <= 0){
			needMinPlaytime = false;
		}else{
			needMinPlaytime = true;
		}
		this.waitFirst = waitFirst;
	}
	
	private void getItem(){
		File f = CountMain.nfDataDir;
		FileConfiguration load = YamlConfiguration.loadConfiguration(f);
		Set<String> Keys = null;
		try{
			Keys = load.getConfigurationSection("Rewards."+internalName+".RewardItem").getKeys(false);
		}catch(Exception e){
			System.err.println("[PlaytimeRewards] There was a fault at "+internalName+"s Item.");
			System.err.println("[PlaytimeRewards] Disabling the Item for this Reward...");
			if(CountMain.debugMode) System.err.println("[PlaytimeRewards] If you want to report this as a Bug, make sure to copy the following Errorcode!");
			if(CountMain.debugMode) e.printStackTrace();
			return;
		}
		if(Keys==null) return;
		String[] neededKeys = {"Item", "Damage", "Amount", "DisplayName", "Lore"};
		for(int i=0;i<neededKeys.length;i++){
			//System.out.println("Checking for Items."+x+"."+neededKeys[i]);
			if(!(Keys.contains(neededKeys[i]))) {
				itemReward = false;
				return;
			}
			//System.out.println("Found Items."+x+"."+neededKeys[i]+" at i="+i+"!");
			if(i==neededKeys.length-1){
				String itemPath = "Rewards."+internalName+".RewardItem.";
				String MaterialString = load.getString(itemPath+"Item");
				short Damage = (short) load.getInt(itemPath+"Damage");
				int Amount = load.getInt(itemPath+"Amount");
				String Name = ChatColor.translateAlternateColorCodes(CountMain.colorChar, load.getString(itemPath+"DisplayName"));
				List<String> LoreOld = load.getStringList(itemPath+"Lore");
				ArrayList<String> Lore = new ArrayList<String>();
				for(String str:LoreOld){
					Lore.add(ChatColor.translateAlternateColorCodes(CountMain.colorChar, str));
				}
				if(MaterialString.equalsIgnoreCase("NONE") || MaterialString.equalsIgnoreCase("")){
					itemReward = false;
					return;
				}
				Material Mat = Material.getMaterial(MaterialString);
				if(Mat==null){
					System.err.println("Unkown Item: "+MaterialString);
					return;
				}
				Map<Enchantment, Integer> ench = new HashMap<Enchantment, Integer>();
				if(Keys.contains("Enchantments") && load.get("Rewards."+internalName+".RewardItem.Enchantments") != null){
					Set<String> Enchants = load.getConfigurationSection("Rewards."+internalName+".RewardItem.Enchantments").getKeys(false);
					for(String enchantName:Enchants){
						Enchantment e = Enchantment.getByName(enchantName);
						if(e==null) continue;
						ench.put(e, load.getInt("Rewards."+internalName+".RewardItem.Enchantments."+enchantName));
					}
				}
				item = new customItem(Name, Lore, Mat, Damage, Amount);
				if(!(ench.containsKey(null))){
					for(Enchantment e:ench.keySet()){
						int val = ench.get(e);
						item.addEnchant(e, val);
					}
				}
				//Item = item.is;
				itemReward = true;
			}
		}
	}
	
	public void updateItem(){
		if(ItemStorage.getItem(13) == null){
			itemReward = false;
			itemHasChanged = true;
			return;
		}else if(ItemStorage.getItem(13).getType() == Material.AIR){
			itemReward = false;
			itemHasChanged = true;
			return;
		}
		item = new customItem(ItemStorage.getItem(13));
		itemReward = true;
		itemHasChanged = true;
	}
	
	public void saveItem(){
		File f = CountMain.nfDataDir;
		if(!(f.exists())) return;
		ItemStack Item = getItemstack();
		FileConfiguration load = YamlConfiguration.loadConfiguration(f);
		Set<String> Enchants = load.getConfigurationSection("Rewards."+internalName+".RewardItem.Enchantments").getKeys(false);
		String px = "Rewards."+internalName+".RewardItem.";
		//Removing all old Enchantments
		if(Enchants != null){
			for(String ench:Enchants){
				load.set(px+"Enchantments."+ench, null);
			}
		}
		if(Item != null){
			String DisplayName;
			if(!(Item.getItemMeta().hasDisplayName())){
				DisplayName = "Reward";
			}else{
				DisplayName = Item.getItemMeta().getDisplayName();
			}
			List<String> Lore;
			if(Item.getItemMeta().getLore() == null){
				Lore = new ArrayList<String>(Arrays.asList(""));
			}else{
				Lore = Item.getItemMeta().getLore();
			}
			load.set(px+"Item", Item.getType().name());
			load.set(px+"Damage", Item.getDurability());
			load.set(px+"Amount", Item.getAmount());
			load.set(px+"DisplayName", DisplayName);
			load.set(px+"Lore", Lore);
			if(!(item.Enchants.containsKey(null))){
				for(Enchantment e:item.Enchants.keySet()){
					String name = e.getName();
					int value = item.Enchants.get(e);
					load.set(px+"Enchantments."+name, value);
				}
			}
		}else{
			load.set(px+"Item", null);
			load.set(px+"Damage", null);
			load.set(px+"Amount", null);
			load.set(px+"DisplayName", null);
			load.set(px+"Lore", null);
		}
		try {
			load.save(f);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public ItemStack getItemstack(){
		return item.is;
	}
	
	public Object getRW(){
		return this;
	}

	@Override
	public boolean fullVersion() {
		return fullVersion;
	}

	@Override
	public String internalName() {
		return internalName;
	}

	@Override
	public String Name() {
		return Name;
	}

	@Override
	public CountType loopType() {
		return loopType;
	}

	@Override
	public boolean hasLoop() {
		return hasLoop;
	}

	@Override
	public boolean waitFirst() {
		return waitFirst;
	}

	@Override
	public boolean getItemChange() {
		return itemHasChanged;
	}

	@Override
	public Inventory getInv() {
		return ItemStorage;
	}

	@Override
	public long getPlaytime() {
		return playTimeNeeded;
	}

	@Override
	public boolean pCmd() {
		return pCmd;
	}

	@Override
	public boolean cCmd() {
		return cCmd;
	}

	@Override
	public boolean playerNotification() {
		return playerNotification;
	}

	@Override
	public boolean globalNotification() {
		return globalNotification;
	}

	@Override
	public boolean msg() {
		return msg;
	}

	@Override
	public boolean hasItem() {
		return itemReward;
	}

	@Override
	public List<String> consoleCmd() {
		return consoleCommands;
	}

	@Override
	public List<String> playerCmd() {
		return playerCommands;
	}

	@Override
	public String Message() {
		return Message;
	}

	@Override
	public boolean minPlaytimeBool() {
		return needMinPlaytime;
	}

	@Override
	public boolean maxPlaytimeBool() {
		return needMaxPlaytime;
	}

	@Override
	public boolean permBool() {
		return needPermission;
	}

	@Override
	public long getMinPlaytime() {
		return minPlaytime;
	}

	@Override
	public long getMaxPlaytime() {
		return maxPlaytime;
	}

	@Override
	public String getPerm() {
		return permission;
	}

}
