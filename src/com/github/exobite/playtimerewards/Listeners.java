package com.github.exobite.playtimerewards;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitRunnable;

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
	
	@EventHandler
	public void onChat(AsyncPlayerChatEvent e){
		if(!CountMain.unlocked) return;
		if(!e.getPlayer().hasPermission("timerewards.unlockedAccess") && !e.getPlayer().isOp()) return;
		String msg = e.getMessage();
		String pt[] = msg.split(" ");
		if(pt.length==0) return;
		if(!pt[0].equalsIgnoreCase("#ptr_unlock")) return;
		e.setCancelled(true);
		if(pt.length<2){
			sendSyncMessage(e.getPlayer(), ChatColor.RED+"Usage: #ptr_unlock <pDat|info>");
			return;
		}
		if(pt[1].equalsIgnoreCase("pDat")){
			if(pt.length<4){
				sendSyncMessage(e.getPlayer(), ChatColor.RED+"Usage: #ptr_unlock pDat <playername> <get|set|call>");
				return;
			}else if(pt.length>3){
				Player t = Bukkit.getPlayer(pt[2]);
				if(t==null){
					sendSyncMessage(e.getPlayer(), ChatColor.RED+"Can´t find "+pt[2]+".");
					return;
				}
				playerData pDat = CountMain.pData.get(t.getUniqueId());
				if(pt[3].equalsIgnoreCase("get")){
					if(pt.length==4){
						String fields = "";
						for(Field f:pDat.getClass().getDeclaredFields()){
							fields = fields + "\n"+f.getName()+", "+f.getType();
						}
						sendSyncMessage(e.getPlayer(), ChatColor.RED+"Usage: #ptr_unlock pDat <playername> get <valueName>"+fields);
						return;
					}
					if(pt.length==5){
						try {
							Field f = pDat.getClass().getDeclaredField(pt[4]);
							f.setAccessible(true);
							String rVal = f.get(pDat)+"";
							sendSyncMessage(e.getPlayer(), ChatColor.GREEN+pt[2]+"´s "+pt[4]+" is "+rVal);
						} catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException e1) {
							sendSyncMessage(e.getPlayer(), ChatColor.RED+"An Error ocurred.\n"+e1.getMessage());
						}
						return;
					}
				}else if(pt[3].equalsIgnoreCase("set")){
					if(pt.length<6){
						String fields = "";
						for(Field f:pDat.getClass().getDeclaredFields()){
							fields = fields + "\n"+f.getName()+", "+f.getType();
						}
						sendSyncMessage(e.getPlayer(), ChatColor.RED+"Usage: #ptr_unlock pDat <playername> set <valueName> <value>"+fields);
						return;
					}
					if(pt.length==6){
						try {
							Field f = pDat.getClass().getDeclaredField(pt[4]);
							f.setAccessible(true);
							Object v = pt[5];
							if(f.getType()==int.class || f.getType()==Integer.class){
								v = Integer.valueOf(pt[5]);
							}
							f.set(pDat, v);
							sendSyncMessage(e.getPlayer(), ChatColor.GREEN+pt[2]+"´s "+pt[4]+" is now "+v);
						} catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException e1) {
							sendSyncMessage(e.getPlayer(), ChatColor.RED+"An Error ocurred.\n"+e1.getMessage());
						}
					}
				}else if(pt[3].equalsIgnoreCase("call")){
					String methods = "";
					for(Method m:pDat.getClass().getDeclaredMethods()){
						if(m.getParameterCount()>0) continue;
						methods = methods +"\n"+m.getName()+", "+m.getReturnType();
					}
					if(pt.length==4){
						sendSyncMessage(e.getPlayer(), ChatColor.RED+"Usage: #ptr_unlock pDat <playername> set <methodName>"+methods);
						return;
					}
					if(pt.length==5){
						try {
							Method m = pDat.getClass().getDeclaredMethod(pt[4]);
							m.setAccessible(true);
							sendSyncMessage(e.getPlayer(), ChatColor.GREEN+"Executed "+m.getName()+" on "+pt[2]+"´s pData runtime.\nReturned "+m.invoke(pDat));
						} catch (NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e1) {
							sendSyncMessage(e.getPlayer(), ChatColor.RED+"An Error ocurred.\n"+e1.getMessage());
						}
					}
				}else{
					sendSyncMessage(e.getPlayer(), ChatColor.RED+"Usage: #ptr_unlock pDat <playername> <get|set|call>");
				}
			}
		}else if(pt[1].equalsIgnoreCase("info")){
			sendSyncMessage(e.getPlayer(), ChatColor.RED+"Usage: #ptr_unlock <pDat|info>"
					+ "\npDat: Access to the PlayerData Object"
					+ "\ninfo: Informations about this command");
		}else{
			sendSyncMessage(e.getPlayer(), ChatColor.RED+"Usage: #ptr_unlock <pDat|info>");
		}
	}
	
	private void sendSyncMessage(final Player p, final String msg){
		new BukkitRunnable(){

			@Override
			public void run() {
				p.sendMessage(msg);
			}
			
		}.runTask(CountMain.getInstance());
	}
	
}
