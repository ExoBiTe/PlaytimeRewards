package com.github.exobite.playtimerewards;

import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class playtimeCommand {
	
	String commandHelp = "/Playtime [player]";
	String noPermission = "Sorry, you don´t have enough Permissions to do this.";

	@SuppressWarnings("deprecation")
	public boolean onCommand(CommandSender sender, Command arg1, String arg2, String[] args) {
		
		if(sender instanceof Player){
			if(!(sender.hasPermission("timerewards.seetime"))){
				sender.sendMessage(ChatColor.RED+noPermission);
				return true;
			}
		}
		if(args == null || args.length==0){
			if(sender instanceof Player){
				sender.sendMessage(ChatColor.GREEN+"Your Playtime is "+getTime(CountMain.pData.get(((Player) sender).getUniqueId()).getPlaytimeNow())+".");
			}else{
				sender.sendMessage("Use /Playtime <player>!");
			}
		}else if(args.length==1){
			Player p = Bukkit.getPlayer(args[0]);
			String playtime;
			String name;
			if(sender instanceof Player && !(sender.hasPermission("timerewards.seetime.others"))){
				p = (Player) sender;
			}
			if(p==null){
				OfflinePlayer op = Bukkit.getOfflinePlayer(args[0]);
				if(op==null){
					sender.sendMessage(ChatColor.RED+"Player "+args[0]+" wasn´t found.");
					return true;	
				}
				if(op.hasPlayedBefore()){
					UUID id = op.getUniqueId();
					if(!CountMain.loggedPlaytimes.containsKey(id)){
						sender.sendMessage(ChatColor.RED+"An Error ocurred.");
						return true;
					}
					name = args[0];
					playtime = getTime(CountMain.loggedPlaytimes.get(id));
					
				}else{
					sender.sendMessage(ChatColor.RED+"Player "+args[0]+" wasn´t found.");
					return true;
				}
			}else{
				playtime = getTime(CountMain.pData.get(p.getUniqueId()).getPlaytimeNow());
				name = p.getName();
			}
			sender.sendMessage(ChatColor.GREEN+name+"´s Playtime is "+playtime+".");
		}else{
			sender.sendMessage(ChatColor.RED+commandHelp);
		}
		
	return true;
	}
	
	private String getTime(long time){
		time = time/1000;
		long hours = time/3600;
		long intoMinutes = time%3600;
		long minutes = intoMinutes/60;
		long seconds = intoMinutes%60;
		String rVal = hours+" hours, "+minutes+" minutes and "+seconds+" seconds";
		return rVal;
	}

}
