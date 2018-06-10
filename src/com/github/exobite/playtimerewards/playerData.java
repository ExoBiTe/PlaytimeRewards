package com.github.exobite.playtimerewards;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Statistic;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;

import com.github.exobite.playtimerewards.motd.Callback;
import com.github.exobite.playtimerewards.update.LangManager;
import com.github.exobite.playtimerewards.update.Message;

public class playerData {
	
	private UUID uuid;
	private List<String> Names;
	private Map<String, Long> Rewards;
	private long lastLogin;
	private long Logout;
	private long joinNow;
	private long playtime;
	private long sessionCount;
	
	private Map<String, Long> mysqlLoginRewards;
	
	public boolean gotData;
	
	playerData(Player p){
		Rewards = new HashMap<String, Long>();
		uuid = p.getUniqueId();
		//System.out.println("Loaded Data for "+p.getName()+", "+p.getUniqueId()+".");
		joinNow = System.currentTimeMillis();
		CountMain.pData.put(uuid, this);
		gotData = false;
		if(CountMain.mysql){
			getMySQLData();
			mysqlLoginRewards = new HashMap<String, Long>();
		}else{
			getFileData();
			gotData = true;
		}
	}
	
	private void getFileData(){
		File f = CountMain.pDataDir;
		Player p = p();
		if(p==null){
			System.err.println("[PlaytimeRewards] Error, Player with UUID "+uuid+" seems to be offline!");
			System.err.println("[PlaytimeRewards] Aborting Action on this Player!");
			CountMain.pData.remove(uuid);
			return;
		}
		FileConfiguration playerDat = YamlConfiguration.loadConfiguration(f).options().copyDefaults(true).configuration();
		Set<String> Players = playerDat.getKeys(true);
		if(!(Players.contains("Players."+uuid))){
			//System.out.println("No data for Player "+p.getName()+" found. Creating it...");
			List<String> nameList = new ArrayList<String>(Arrays.asList(p.getName()));
			try {
				playerDat.set("Players."+uuid+".lastLogin", joinNow);
				playerDat.set("Players."+uuid+".playTime", 0);
				playerDat.set("Players."+uuid+".Names", nameList);
				playerDat.set("Players."+uuid+".Sessions", 1);
				//playerDat.set("Players."+uuid+".Rewards", "nothing.");
				playerDat.save(f);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		if(CountMain.vanillaCount){
			playtime = (p().getStatistic(Statistic.PLAY_ONE_TICK) /20)*1000;	//	Tick/20 = Seconds. *1000 = Milliseconds
		}
		if(playerDat.contains("Players."+uuid+".RewardData")==false){
			playerDat.createSection("Players."+uuid+".RewardData");
			try {
				playerDat.save(f);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		Set<String> LoopList = playerDat.getConfigurationSection("Players."+uuid+".RewardData").getKeys(false);
		
		boolean saveFile = false;
		for(String str: LoopList){
			if(!(CountMain.RewardList.containsKey(str))) {
				if(CountMain.msgUnknownLoops){
					System.out.println("[PlaytimeRewards] Couldn´t find the Reward "+str+".");
					System.out.println("[PlaytimeRewards] Deleting it from "+p.getName()+"´s saved Data.");
				}
				playerDat.set("Players."+uuid+".RewardData."+str, null);
				saveFile = true;
				continue;
			}
			long value = playerDat.getLong("Players."+uuid+".RewardData."+str);
			if(value>=0){
				RewardObject rw = CountMain.RewardList.get(str);
				if(rw == null && CountMain.msgUnknownLoops){
					System.err.println("[PlaytimeRewards] Unknown Reward "+str+".");
					continue;
				}
				Rewards.put(rw.internalName(), value);
			}
		}
		
		if(saveFile){
			try {
				playerDat.save(f);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		Names = playerDat.getStringList("Players."+uuid+".Names");
		lastLogin = playerDat.getLong("Players."+uuid+".lastLogin");
		playtime = playerDat.getLong("Players."+uuid+".playTime");
		sessionCount = playerDat.getLong("Players."+uuid+".Sessions") +1;
		
		if(Names==null){
			Names = new ArrayList<String>(Arrays.asList(p.getName()));
		}
		if(!(Names.contains(p.getName()))) Names.add(p.getName());
	}
	
	public void logOut(boolean removeFromList, boolean async){
		Logout =  System.currentTimeMillis();
		long newPlaytime = Logout - joinNow;
		playtime = playtime + newPlaytime;
		if(CountMain.vanillaCount) playtime = (p().getStatistic(Statistic.PLAY_ONE_TICK) /20)*1000;
		if(!gotData){
			if(removeFromList) CountMain.pData.remove(uuid);
			return;
		}
		if(CountMain.mysql && async){
			saveDataToMySQLAsync();
		}else if(CountMain.mysql && !async){
			saveDataToMySQLSync();
		}else if(!CountMain.mysql && async){
			saveDataToFileAsync();
		}else if(!CountMain.mysql && !async){
			saveDataToFileSync();
		}else{
			//WUT?!
		}
		if(removeFromList) CountMain.pData.remove(uuid);
	}
	
	
	private void saveDataToFileSync(){
		File f = CountMain.pDataDir;
		FileConfiguration playerDat = YamlConfiguration.loadConfiguration(f);
		playerDat.options().copyHeader(true);
		try {
			playerDat.set("Players."+uuid+".lastLogin", joinNow);
			playerDat.set("Players."+uuid+".playTime", playtime);
			playerDat.set("Players."+uuid+".Names", Names);
			playerDat.set("Players."+uuid+".Sessions", sessionCount);
			if(Rewards != null){ if(Rewards.size()>0){
				for(String str:Rewards.keySet()){
					if(CountMain.RewardList.get(str).loopType() != CountType.SESSIONTIME){
						playerDat.set("Players."+uuid+".RewardData."+str, Rewards.get(str));
					}
				}
			}}
			playerDat.save(f);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private void saveDataToFileAsync(){
		new BukkitRunnable(){
			@Override
			public void run() {
				File f = CountMain.pDataDir;
				FileConfiguration playerDat = YamlConfiguration.loadConfiguration(f);
				playerDat.options().copyHeader(true);
				try {
					playerDat.set("Players."+uuid+".lastLogin", joinNow);
					playerDat.set("Players."+uuid+".playTime", playtime);
					playerDat.set("Players."+uuid+".Names", Names);
					playerDat.set("Players."+uuid+".Sessions", sessionCount);
					if(Rewards != null){ if(Rewards.size()>0){
						for(String str:Rewards.keySet()){
							if(CountMain.RewardList.get(str).loopType() != CountType.SESSIONTIME){
								playerDat.set("Players."+uuid+".RewardData."+str, Rewards.get(str));
							}
						}
					}}
					playerDat.save(f);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}.runTaskAsynchronously(CountMain.getInstance());
	}
	
	public void getReward(RewardObject r, boolean check){
		if(CountMain.debugMode) System.out.println("Reward "+r.internalName()+" checking...");
		Player p = p();
		if(p==null){
			System.err.println("[PlaytimeRewards] Error, Player with UUID "+uuid+" seems to be offline!");
			System.err.println("[PlaytimeRewards] Aborting Action on this Player!");
			logOut(true, true);
			return;
		}
		if(Rewards.containsKey(r.internalName()) && check && r.hasLoop() && !checkLoopTime(r)){
			return;
		}
		if(CountMain.debugMode) System.out.println("Check 1");
		if(Rewards.containsKey(r.internalName()) && check && !r.hasLoop()){
			return;
		}
		if(CountMain.debugMode) System.out.println("Check 2");
		if(!(Rewards.containsKey(r.internalName())) && check){
			if(r.loopType() == CountType.PLAYTIME){
				if(r.getPlaytime() > getPlaytimeNow()) return;
			}else if(r.loopType() == CountType.REALTIME){
				if(r.getPlaytime() > System.currentTimeMillis()) return;
			}else if(r.loopType() == CountType.SESSIONTIME){
				if(r.getPlaytime() > getSessionTimeNow()) return;
			}
		}
		if(CountMain.debugMode) System.out.println("Check 3");
		if(!Rewards.containsKey(r.internalName()) && check && r.hasLoop() && r.waitFirst()){
			if(r.loopType() == CountType.PLAYTIME){
				Rewards.put(r.internalName(), getPlaytimeNow());
			}else if(r.loopType() == CountType.REALTIME){
				Rewards.put(r.internalName(), System.currentTimeMillis());
			}else if(r.loopType() == CountType.SESSIONTIME){
				Rewards.put(r.internalName(), getSessionTimeNow());
			}
			return;
		}
		if(CountMain.debugMode) System.out.println("Check 4");
		
		
		if(!r.hasLoop()){
			Rewards.put(r.internalName(), 777L);
		}else if(r.loopType() == CountType.PLAYTIME){
			Rewards.put(r.internalName(), getPlaytimeNow());
		}else if(r.loopType() == CountType.REALTIME){
			Rewards.put(r.internalName(), System.currentTimeMillis());
		}else if(r.loopType() == CountType.SESSIONTIME){
			Rewards.put(r.internalName(), getSessionTimeNow());
		}
		
		//Rewards.put(r.internalName, System.currentTimeMillis());
		if(r.pCmd()) {
			for(String playerCommand:r.playerCmd()){
				p.performCommand(setPlayer(playerCommand));
			}
		}
		if(r.cCmd()) {
			CountMain inst = CountMain.getInstance();
			for(String consoleCommand:r.consoleCmd()){
				String toExecute = setPlayer(consoleCommand);
				inst.getServer().dispatchCommand(inst.getServer().getConsoleSender(), toExecute);
			}
		}
		if(r.playerNotification()){
			//String playerMsg = ChatColor.GREEN+"You obtained the Reward "+ChatColor.GOLD+r.Name()+ChatColor.GREEN+"!";
			String playerMsg = LangManager.getFormatMessage(Message.PLAYER_NOTIFICATION_REWARD_OBTAINED, new String[] {r.Name()}, null);
			p.sendMessage(playerMsg);
		}
		if(r.msg()) p.sendMessage(setPlayer(r.Message()));
		if(r.hasItem()) {
			if(p.getInventory().firstEmpty()>0){
				ItemStack is = r.getItemstack();
				if(is.getItemMeta().getLore()!=null){
					List<String> newLore = new ArrayList<String>();
					ItemMeta meta = is.getItemMeta();
					for(String s:meta.getLore()){
						newLore.add(setPlayer(s));
					}
					meta.setLore(newLore);
					is.setItemMeta(meta);
				}
				p.getInventory().addItem(is);
			}else{
				p.getLocation().getWorld().dropItemNaturally(p.getLocation(), r.getItemstack());
			}
			
		}
		if(r.globalNotification()){
			//String globalMsg = ChatColor.GREEN+p.getName()+" obtained the Reward "+ChatColor.GOLD+r.Name()+ChatColor.GREEN+"!";
			String globalMsg = LangManager.getFormatMessage(Message.GLOBAL_NOTIFICATION_REWARD_OBTAINED, new String[] {p.getName(), r.Name()}, null);
			for(Player x:Bukkit.getOnlinePlayers()){
				x.sendMessage(globalMsg);
			}
		}
		//Sounds und Particles werden über die Reward Class gesteuert!
		//Zum leichteren umsetzen der Versionskompatibilität
		if(!r.fullVersion()) return;
		RewardObj rwFull = (RewardObj) r;
		if(rwFull.hasSound){
			rwFull.doSounds(p);
		}
		if(rwFull.hasParticle){
			rwFull.doParticles(p);
		}
	}
	
	private boolean checkLoopTime(RewardObject r){
		if(!(Rewards.containsKey(r.internalName()))) return false;
		if(r.loopType() == CountType.PLAYTIME){
			long pTimeNeed = r.getPlaytime() + Rewards.get(r.internalName());
			if(getPlaytimeNow()>=pTimeNeed) return true;
		}else if(r.loopType() == CountType.REALTIME){
			long pTimeNeed = r.getPlaytime() + Rewards.get(r.internalName());
			if(System.currentTimeMillis() >= pTimeNeed) return true;
		}else if(r.loopType() == CountType.SESSIONTIME){
			long lastGet = r.getPlaytime() + Rewards.get(r.internalName());
			if(getSessionTimeNow() >= lastGet) return true;
		}else{
			return false;
		}
		return false;
	}
	
	private Player p(){
		return Bukkit.getPlayer(uuid);
	}
	
	long getPlaytimeNow(){
		long rVal = 0L;
		if(CountMain.vanillaCount){
			rVal = (p().getStatistic(Statistic.PLAY_ONE_TICK)/20)*1000;
		}else{
			rVal = playtime+System.currentTimeMillis()-joinNow;
		}
		return rVal;
	}
	
	long getSessionTimeNow(){
		return System.currentTimeMillis() - joinNow;
	}
	
	List<String> getRewards(){
		List<String> rVal = new ArrayList<String>();
		rVal.add(ChatColor.GOLD+"Here are "+p().getName()+"s earned Rewards!");
		rVal.add(ChatColor.BLUE+"Internal Name "+ChatColor.GOLD+"|"+ChatColor.RESET+" Display Name ");//+ChatColor.GOLD+"|"+ChatColor.GREEN+" Value");
		for(String rwName:Rewards.keySet()){
			RewardObject r = CountMain.RewardList.get(rwName);
			if(!(CountMain.RewardList.containsKey(rwName))) continue;
			String toAdd = ChatColor.BLUE+r.internalName() +ChatColor.GOLD+" | "+ChatColor.RESET+r.Name();//+ChatColor.GOLD+" | "+ChatColor.GREEN+Rewards.get(rwName);
			rVal.add(toAdd);
		}
		return rVal;
	}
	
	private String setPlayer(String old){
		String r = old;
		if(r.contains("<player>")) r = r.replace("<player>", p().getName());
		return r;
	}
	
	
	
	
	//Mysql Stuff
	
	private void getMySQLData(){
		if(CountMain.debugMode) CountMain.getInstance().send("Trying to get MySQL Data for Player "+p().getName()+".");
		Callback<Object[]> callback = new Callback<Object[]>(){
			@Override
			public void execute(Object[] response) {
				setData(response);
			}
		};
		getMysqlAsync(callback);
	}
	
	@SuppressWarnings("unchecked")
	private void setData(Object[] data){	//This checks for valid returned data from the mysql and sets it for the playerdat
		if(data.length!=2) mysqlFail();
		if(data[0] == null || data[1] == null){
			mysqlFail();
			return;
		}
		if(!(data[0] instanceof List<?>)){
			mysqlFail();
			return;
		}
		if(!(data[1] instanceof Map<?, ?>)){
			mysqlFail();
			return;
		}
		List<String> dataList = (List<String>) data[0];
		Map<String, Long> rewardList = (Map<String, Long>) data[1];
		this.lastLogin = Long.valueOf(dataList.get(0));
		this.playtime = Long.valueOf(dataList.get(1));
		this.sessionCount = Long.valueOf(dataList.get(2)) +1;
		
		for(String str:rewardList.keySet()){
			if(!CountMain.RewardList.containsKey(str)){
				if(CountMain.msgUnknownLoops){
					System.out.println("[PlaytimeRewards] Couldn´t find the Reward "+str+".");
					//System.out.println("[PlaytimeRewards] Deleting it from "+p().getName()+"´s saved Data.");
				}
				//mysqlFail();
				continue;
			}else{
				long val = rewardList.get(str);
				if(CountMain.RewardList.get(str).loopType() == CountType.SESSIONTIME) val = 0L;	//SESSIONTIME FIX (RESET)
				Rewards.put(str, val);
			}
		}
		
		gotData = true;
	}
	
	private void mysqlFail(){
		System.err.println("Couldn´t access data for Player "+p().getName()+", "+uuid+" from mysql.");
	}
	
	private void getMysqlAsync(final Callback<Object[]> cb) {
		final UUID uuid = this.uuid;
	    new BukkitRunnable() {
	        @Override
	        public void run(){
	        	try {
	        		Connection con = CountMain.con;
	        		
	        		//Check if user exists
	        		boolean exist = false;
	        		String Query0 = "SELECT EXISTS(SELECT * FROM PR_PlayerData WHERE UUID = ?)";
					PreparedStatement prStmt0 = con.prepareStatement(Query0);
					prStmt0.setString(1, uuid+"");
					ResultSet result =  prStmt0.executeQuery();
					while(result.next()){
						if(result.getInt(1)>0) exist = true;
					}
					
					result.close();
					prStmt0.close();
					
					if(CountMain.debugMode) System.out.println("Player exist: "+exist);
					
					final List<String> list = new ArrayList<String>();
					final Map<String, Long> rewards = new HashMap<String, Long>();
					
					if(exist){
						String query = "SELECT * FROM PR_PlayerData where UUID = ?";
		        		PreparedStatement prstmt = con.prepareStatement(query);
		        		prstmt.setString(1, uuid+"");
		        		ResultSet rs = prstmt.executeQuery();

		        		while(rs.next()){
		        			list.add(rs.getLong("lastLogin")+"");		//0, lastLogin
		        			list.add(rs.getLong("playTime")+"");		//1, playtime
		        			list.add(rs.getLong("sessionCount")+"");	//2, sessioncount
		        		}
		        		rs.close();
		        		prstmt.close();
		        		
		        		String query2 = "SELECT * FROM PR_PlayerLoops WHERE UUID = ?";
		        		PreparedStatement prstmt2 = con.prepareStatement(query2);
		        		prstmt2.setString(1, uuid+"");
		        		ResultSet rs2 = prstmt2.executeQuery();
		        		
		        		while(rs2.next()){
		        			rewards.put(rs2.getString("loopName"), rs2.getLong("loopValue"));
		        		}
		        		prstmt2.close();
		        		rs2.close();
					}else{
						String Query = "INSERT INTO PR_PlayerData (UUID, lastLogin, playTime, sessionCount) VALUES (?, 0, ?, 1)";
						PreparedStatement prstmt = con.prepareStatement(Query);
		        		prstmt.setString(1, uuid+"");
		        		prstmt.setLong(2, p().getStatistic(Statistic.PLAY_ONE_TICK) /20);
		        		prstmt.execute();
		        		prstmt.close();
		        		list.add(0+"");
		        		list.add((p().getStatistic(Statistic.PLAY_ONE_TICK) /20)+"");
		        		list.add(0+"");
					}
					mysqlLoginRewards = rewards;
	        		final Object[] obj = {list, rewards};
	        		
		            new BukkitRunnable() {
		                @Override
		                public void run(){
		                    cb.execute(obj);
		                }
		            }.runTask(CountMain.getInstance());
				} catch (SQLException e) {
					System.err.println("[PlaytimeRewards] Getting Login-Data for Player "+p().getName()+", "+uuid+" from MySQL ran into an error!");
					System.err.println("[PlaytimeRewards] If you want to report this as a Bug, make sure to copy the following Errorcode!");
					e.printStackTrace();
				}
	        }
	    }.runTaskAsynchronously(CountMain.getInstance());
	}
	
	private void saveDataToMySQLAsync(){
		Bukkit.getScheduler().runTaskAsynchronously(CountMain.getInstance(), new Runnable(){

			@Override
			public void run() {
				try{
					//Try to check for not needed replacements
					Map<String, Long> uploadRewards = new HashMap<String, Long>();
					List<String> delEntrys = new ArrayList<String>();
					if(mysqlLoginRewards != null){
						for(String x:Rewards.keySet()){
							if(mysqlLoginRewards.containsKey(x)){
								Long val1 = mysqlLoginRewards.get(x);
								Long val2 = Rewards.get(x);
								if(val1 > val2 || val1 < val2){		//IF NOT EQUALS
									uploadRewards.put(x, val2);
									delEntrys.add(x);
								}
							}else{
								uploadRewards.put(x, Rewards.get(x));
							}
						}
					}else{
						uploadRewards = Rewards;
					}
					
					//building the delete Query
					String delQuery = "DELETE FROM PR_PlayerLoops WHERE (UUID, loopName) IN (";	//(1,2), (uuid, name)...
					int counter = 0;
					for(String x:delEntrys){
						counter++;
						if(counter==1){
							String toAdd = "('"+uuid+"','"+x+"')";		//"('uuid', 'name')"
							delQuery = delQuery + toAdd;
						}else{
							String toAdd = ", ('"+uuid+"','"+x+"')";	//", ('uuid', 'name')"
							delQuery = delQuery + toAdd;
						}
					}
					delQuery = delQuery + ")";
					if(CountMain.debugMode) System.out.println(delQuery);
					
					
					Connection con = CountMain.con;
					String Query = "UPDATE PR_PlayerData SET lastLogin = ?, playTime = ?, sessionCount = ? WHERE UUID = ?";
					PreparedStatement prstmt = con.prepareStatement(Query);
	        		prstmt.setString(4, uuid+"");
	        		prstmt.setLong(1, joinNow);
	        		prstmt.setLong(2, playtime);
	        		prstmt.setLong(3, sessionCount);
	        		prstmt.execute();
	        		prstmt.close();
	        		
	        		if(delEntrys.size()!=0){		//Execute just if there are deleteable entrys
	        			PreparedStatement prstmt2 = con.prepareStatement(delQuery);
		        		prstmt2.execute();
		        		prstmt2.close();
	        		}

	        		String QueryStart = "INSERT INTO PR_PlayerLoops (UUID, loopName, loopValue) VALUES ";		//(?, 0, 0), (?, 0, 0), [...]
	        		
	        		int amount = 0;
	        		for(String str:uploadRewards.keySet()){
	        			amount++;
	        			String struuid = uuid+"";
	        			if(amount==1){
	        				QueryStart = QueryStart + "('"+struuid+"', '"+str+"', "+uploadRewards.get(str)+")";
	        			}else{
	        				QueryStart = QueryStart + ", ('"+struuid+"', '"+str+"', "+uploadRewards.get(str)+")";
	        			}
	        		}
	        		//QueryStart = QueryStart + ";";
	        		if(uploadRewards.size()!=0){		//Execute just if there are deleteable entrys
	        			if(CountMain.debugMode) System.out.println(QueryStart);
		        		PreparedStatement prstmt3 = con.prepareStatement(QueryStart);
		        		prstmt3.execute();
		        		prstmt3.close();
	        		}
	        		
				}catch(SQLException e){
					
					e.printStackTrace();
				}
			}
			
		});
	}
	
	private void saveDataToMySQLSync(){
		Bukkit.getScheduler().runTask(CountMain.getInstance(), new Runnable(){

			@Override
			public void run() {
				try{
					//Try to check for not needed replacements
					Map<String, Long> uploadRewards = new HashMap<String, Long>();
					List<String> delEntrys = new ArrayList<String>();
					if(mysqlLoginRewards != null){
						for(String x:Rewards.keySet()){
							if(mysqlLoginRewards.containsKey(x)){
								Long val1 = mysqlLoginRewards.get(x);
								Long val2 = Rewards.get(x);
								if(val1 > val2 || val1 < val2){		//IF NOT EQUALS
									uploadRewards.put(x, val2);
									delEntrys.add(x);
								}
							}else{
								uploadRewards.put(x, Rewards.get(x));
							}
						}
					}else{
						uploadRewards = Rewards;
					}
					
					//building the delete Query
					String delQuery = "DELETE FROM PR_PlayerLoops WHERE (UUID, loopName) IN (";	//(1,2), (uuid, name)...
					int counter = 0;
					for(String x:delEntrys){
						counter++;
						if(counter==1){
							String toAdd = "('"+uuid+"','"+x+"')";		//"('uuid', 'name')"
							delQuery = delQuery + toAdd;
						}else{
							String toAdd = ", ('"+uuid+"','"+x+"')";	//", ('uuid', 'name')"
							delQuery = delQuery + toAdd;
						}
					}
					delQuery = delQuery + ")";
					if(CountMain.debugMode) System.out.println(delQuery);
					
					
					Connection con = CountMain.con;
					String Query = "UPDATE PR_PlayerData SET lastLogin = ?, playTime = ?, sessionCount = ? WHERE UUID = ?";
					PreparedStatement prstmt = con.prepareStatement(Query);
	        		prstmt.setString(4, uuid+"");
	        		prstmt.setLong(1, joinNow);
	        		prstmt.setLong(2, playtime);
	        		prstmt.setLong(3, sessionCount);
	        		prstmt.execute();
	        		prstmt.close();
	        		
	        		if(delEntrys.size()!=0){		//Execute just if there are deleteable entrys
	        			PreparedStatement prstmt2 = con.prepareStatement(delQuery);
		        		prstmt2.execute();
		        		prstmt2.close();
	        		}

	        		String QueryStart = "INSERT INTO PR_PlayerLoops (UUID, loopName, loopValue) VALUES ";		//(?, 0, 0), (?, 0, 0), [...]
	        		
	        		int amount = 0;
	        		for(String str:uploadRewards.keySet()){
	        			amount++;
	        			String struuid = uuid+"";
	        			if(amount==1){
	        				QueryStart = QueryStart + "('"+struuid+"', '"+str+"', "+uploadRewards.get(str)+")";
	        			}else{
	        				QueryStart = QueryStart + ", ('"+struuid+"', '"+str+"', "+uploadRewards.get(str)+")";
	        			}
	        		}
	        		//QueryStart = QueryStart + ";";
	        		if(uploadRewards.size()!=0){		//Execute just if there are deleteable entrys
	        			if(CountMain.debugMode) System.out.println(QueryStart);
		        		PreparedStatement prstmt3 = con.prepareStatement(QueryStart);
		        		prstmt3.execute();
		        		prstmt3.close();
	        		}
	        		
				}catch(SQLException e){
					
					e.printStackTrace();
				}
			}
			
		});
	}
}
