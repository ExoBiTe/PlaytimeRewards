package com.github.exobite.playtimerewards;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.scheduler.BukkitRunnable;

public class Utils {
	
	String compatibleVersions[] = {"1.9", "1.10", "1.11", "1.12"};
	String noParticleVersions[] = {"1.8", "1.7"};
	
	public vCheck VersionsCheck(){
		String serverV = Bukkit.getBukkitVersion();
		vCheck val = vCheck.UNKNOWN;
		for(String x:compatibleVersions){
			if(serverV.contains(x)) val = vCheck.FULL;
		}
		for(String x:noParticleVersions){
			if(serverV.contains(x)) val = vCheck.NO_PARTICLES;
		}
		if(val == vCheck.UNKNOWN){
			System.err.println("[PlaytimeRewards] You´re using an unsupported Minecraft Version!");
		}
		return val;
	}
	
	public void wholeData(){
		new BukkitRunnable(){
			@Override
			public void run() {
				Map<UUID, Long> r = new HashMap<UUID, Long>();
				if(CountMain.mysql){
					Connection con = CountMain.con;
					try {
						PreparedStatement ps = con.prepareStatement("SELECT * FROM PR_PlayerData");
						ResultSet rs = ps.executeQuery();
						while(rs.next()){
							String uuid = rs.getString("UUID");
							Long Playtime = rs.getLong("playTime");
							r.put(UUID.fromString(uuid), Playtime);
						}
						ps.close();
						rs.close();
					} catch (SQLException e) {
						e.printStackTrace();
					}	
				}else{
					File f = CountMain.pDataDir;
					FileConfiguration playerDat = YamlConfiguration.loadConfiguration(f).options().copyDefaults(true).configuration();
					Set<String> Players = playerDat.getKeys(true);	//List of all UUIDs
					for(String id:Players){
						if(id.equals("Players")) continue;
						String uuid = id.split("\\.")[1];
						Long Playtime = playerDat.getLong("Players."+uuid+".playTime");
						r.put(UUID.fromString(uuid), Playtime);
					}
					//Should fill the Map with UUID.Playtime
				}
				if(CountMain.debugMode){
					System.out.println("All found player Records:");
					for(UUID id:r.keySet()){
						System.out.println("UUID "+id+" is "+r.get(id));
					}
				}
				CountMain.loggedPlaytimes = r;
			}
		}.runTaskAsynchronously(CountMain.getInstance());
	}
	
	public Map<Long, UUID> getTop(int amount, Map<UUID, Long> list){
		Map<Long, UUID> top = new HashMap<Long, UUID>();
		if(amount>list.size()) amount = list.size();
		for(int i=0;i<amount;i++){
			long highestPT = 0;
			UUID high = null;
			for(UUID id:list.keySet()){
				long playtime;
				if(CountMain.pData.containsKey(id)){
					
					playtime = CountMain.pData.get(id).getPlaytimeNow();
				}else{
					playtime = list.get(id);
				}
				if(playtime>highestPT && !top.containsValue(id)) {
					highestPT = playtime;
					high = id;
				}
			}
			top.put(i+1L, high);
		}
		return top;
	}
	
	public String getTime(long time){
		time = time/1000;
		long hours = time/3600;
		long intoMinutes = time%3600;
		long minutes = intoMinutes/60;
		long seconds = intoMinutes%60;
		String rVal = hours+" hours, "+minutes+" minutes and "+seconds+" seconds";
		return rVal;
	}

}
