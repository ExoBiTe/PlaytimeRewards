package com.github.exobite.playtimerewards;

import java.util.List;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public interface RewardObject {
	
	public Object getRW();
	public boolean fullVersion();
	
	public String internalName();
	public String Name();
	public CountType loopType();
	
	public boolean hasLoop();
	public boolean waitFirst();
	public boolean getItemChange();
	
	public Inventory getInv();
	public long getPlaytime();
	
	public boolean pCmd();
	public boolean cCmd();
	public boolean playerNotification();
	public boolean globalNotification();
	public boolean msg();
	public boolean hasItem();
	
	public List<String> consoleCmd();
	public List<String> playerCmd();
	public String Message();
	public ItemStack getItemstack();
	
	public boolean minPlaytimeBool();
	public boolean maxPlaytimeBool();
	public boolean permBool();
	public long getMinPlaytime();
	public long getMaxPlaytime();
	public String getPerm();
	
	public void saveItem();
	
	public void updateItem();
	
	public void setLoop(long minPlaytime, long maxPlaytime, String Permission, boolean waitFirst);

}
