package com.github.exobite.playtimerewards;

import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class Commands {
	
	String commandHelp = "/PlaytimeRewards reload|editItem|giveReward|list|seeRewards";
	String editItemHelp = "/PlaytimeRewards editItem <RewardName>";
	String giveRewardHelp = "/PlaytimeRewards giveReward <RewardName> [Player]";
	String noPermission = "Sorry, you don´t have enough Permissions to do this.";

	public boolean onCommand(CommandSender sender, Command arg1, String arg2, String[] args) {
		
		if(args == null){
			sender.sendMessage(ChatColor.RED+commandHelp);
			return true;
		}
		if(args.length<=0){
			sender.sendMessage(ChatColor.RED+commandHelp);
			return true;
		}
		if(args[0].equalsIgnoreCase("reload")){
			if(sender instanceof Player){
				if(!(sender.hasPermission("timerewards.reload"))){
					sender.sendMessage(ChatColor.RED+noPermission);
					return true;
				}
			}
			CountMain.getInstance().reloadConfigCustom();
			sender.sendMessage(ChatColor.GREEN+"[PlaytimeRewards] Config.yml and rewards.yml reloaded!");
			return true;
		}else if(args[0].equalsIgnoreCase("editItem")){
			if(!(sender.hasPermission("timerewards.editItems"))) {
				sender.sendMessage(ChatColor.RED+noPermission);
				return true;
			}
			if(args.length!=2){
				sender.sendMessage(ChatColor.RED+editItemHelp);
				return true;
			}
			if(!(sender instanceof Player)){
				sender.sendMessage("This command is just avaible for Players.");
				return true;
			}
			RewardObject rw = CountMain.RewardList.get(args[1]);
			if(rw == null){
				sender.sendMessage(ChatColor.RED+"No Reward "+args[1]+" found.");
				return true;
			}
			Player p = (Player) sender;
			p.openInventory(rw.getInv());
			CountMain.itemEdit.put(p.getUniqueId(), rw);
			return true;
		}else if(args[0].equalsIgnoreCase("giveReward")){
			if(!(sender.hasPermission("timerewards.giveRewards"))) {
				sender.sendMessage(ChatColor.RED+noPermission);
				return true;
			}
			if(args.length<2 || args.length >3){
				sender.sendMessage(ChatColor.RED+giveRewardHelp);
				return true;
			}
			RewardObject rw = CountMain.RewardList.get(args[1]);
			if(rw == null){
				sender.sendMessage(ChatColor.RED+"No Reward "+args[1]+" found.");
				return true;
			}
			Player p;
			if(args.length==2){
				if(sender instanceof Player){
					p = (Player) sender;
				}else{
					sender.sendMessage("You need to specify a Player from the Console!");
					return true;
				}
			}else if(args.length==3){
				p = Bukkit.getPlayer(args[2]);
			}else{
				sender.sendMessage(ChatColor.RED+giveRewardHelp);
				return true;
			}
			if(p==null){
				sender.sendMessage(ChatColor.RED+"No Player "+args[2]+" found.");
			}
			playerData pDat = CountMain.pData.get(p.getUniqueId());
			pDat.getReward(rw, false);
			return true;
		}else if(args[0].equalsIgnoreCase("list")){
			if(!(sender.hasPermission("timerewards.listRewards"))) {
				sender.sendMessage(ChatColor.RED+noPermission);
				return true;
			}
			sender.sendMessage(ChatColor.GOLD+"RewardName"+ChatColor.WHITE+" | "+ChatColor.GREEN+"internalName"+ChatColor.WHITE+" | "+ChatColor.GOLD+"Time Needed"+ChatColor.WHITE+" | "+ChatColor.AQUA+"CountType");
			for(String rName:CountMain.RewardList.keySet()){
				RewardObject rw = CountMain.RewardList.get(rName);
				long time = rw.getPlaytime();
				String toSend = rw.Name()+ChatColor.RESET+" | "+ChatColor.GREEN+rw.internalName()+ChatColor.RESET+" | "+ChatColor.GOLD+getTime(time) + ChatColor.RESET+" | "+ChatColor.AQUA+rw.loopType().toString();
				sender.sendMessage(toSend);
			}
			return true;
		}else if(args[0].equalsIgnoreCase("seeRewards")){
			if(args.length==1){
				if(!(sender.hasPermission("timerewards.seeRewardsOwn"))) {
					sender.sendMessage(ChatColor.RED+noPermission);
					return true;
				}
				if(!(sender instanceof Player)){
					sender.sendMessage("You need to specify a Player! Use /playtimerewards seeRewards <player>");
					return true;
				}
				Player pTarget = (Player) sender;
				playerData pDat = CountMain.pData.get(pTarget.getUniqueId());
				//GetRewards
				List<String> output = pDat.getRewards();
				for(String send:output){
					sender.sendMessage(send);
				}
				return true;
			}else if(args.length==2){
				if(!(sender.hasPermission("timerewards.seeRewardsOthers"))) {
					sender.sendMessage(ChatColor.RED+noPermission);
					return true;
				}
				Player p = Bukkit.getPlayer(args[1]);
				if(p==null){
					sender.sendMessage(ChatColor.RED+"No Player "+args[1]+" found.");
					return true;
				}
				playerData pDat = CountMain.pData.get(p.getUniqueId());
				//GetRewards
				List<String> output = pDat.getRewards();
				for(String send:output){
					sender.sendMessage(send);
				}
				return true;
			}else{
				sender.sendMessage(ChatColor.RED+"Use /playtimerewards seeRewards [player]");
				return true;
			}
		}else{
			sender.sendMessage(ChatColor.RED+commandHelp);
			return true;
		}
		//return false;
	}
	
	private String getTime(long time){
		time = time/1000;
		long hours = time/3600;
		long intoMinutes = time%3600;
		long minutes = intoMinutes/60;
		long seconds = intoMinutes%60;
		String rVal = hours+" h, "+minutes+" min, "+seconds+" s";
		return rVal;
	}


}
