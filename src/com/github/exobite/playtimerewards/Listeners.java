package com.github.exobite.playtimerewards;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class Listeners implements Listener {
	
	@EventHandler
	public void onJoin(PlayerJoinEvent e){
		Player p = e.getPlayer();
		new playerData(p);
		//Loads all Offline Playerdata
		if(CountMain.loggedPlaytimes==null){
			CountMain.utils.wholeData();
		}
	}
	
	@EventHandler
	public void onQuit(PlayerQuitEvent e){
		Player p = e.getPlayer();
		playerData pDat = CountMain.pData.get(p.getUniqueId());
		pDat.logOut(true, true);
	}
	
	@EventHandler
	public void onInvClick(InventoryClickEvent e){
		if(!(e.getWhoClicked() instanceof Player)) return;
		if(!(CountMain.guis.contains(e.getInventory()))) return;
		if(e.getCurrentItem() == null) return;
		if(e.getCurrentItem().equals(CountMain.emptyItem)) e.setCancelled(true);
	}
	
	@EventHandler
	public void onInvClose(InventoryCloseEvent e){
		if(!(CountMain.guis.contains(e.getInventory()))) return;
		if(!(CountMain.itemEdit.containsKey(e.getPlayer().getUniqueId()))) return;
		RewardObject rw = CountMain.itemEdit.get(e.getPlayer().getUniqueId());
		rw.updateItem();
		CountMain.itemEdit.remove(e.getPlayer().getUniqueId());
		e.getPlayer().sendMessage(ChatColor.GREEN+"Successfully changed the Rewarditem of "+rw.internalName()+"!");
	}
	
	@EventHandler
	public void onCommand(PlayerCommandPreprocessEvent e){
		if(!CountMain.forceOwnCommands) return;
		Player p = e.getPlayer();
		String msg = e.getMessage();
		String command = msg.split(" ")[0];
		command = command.replace("/", "");
		if(!command.equalsIgnoreCase("Playtime") && !command.equalsIgnoreCase("pt") && !command.equalsIgnoreCase("PlaytimeRewards") && !command.equalsIgnoreCase("pr")) return;
		e.setCancelled(true);
		//p.sendMessage("Command is "+command);
		String argstr = msg.replace("/", "");
		
		if(argstr.contains(" ")){
			argstr = argstr.replace(command+" ", "");
		}else{
			argstr = argstr.replace(command, "");
		}
		//p.sendMessage("Argstr: ["+argstr+"]");
		
		String args[] = argstr.split(" ");
		if(args[0].equals("")) args = null;
		
		if(command.equalsIgnoreCase("Playtime") || command.equalsIgnoreCase("pt")){
			new playtimeCommand().onCommand(p, null, null, args);
			e.setCancelled(true);
		}else if(command.equalsIgnoreCase("PlaytimeRewards") || command.equalsIgnoreCase("pr")){
			new Commands().onCommand(p, null, null, args);
			e.setCancelled(true);
		}
	}
	
}
