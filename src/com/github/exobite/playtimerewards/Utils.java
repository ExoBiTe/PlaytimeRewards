package com.github.exobite.playtimerewards;

import org.bukkit.Bukkit;

public class Utils {
	
	String compatibleVersions[] = {"1.9", "1.10", "1.11"};
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

}
