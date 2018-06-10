package com.github.exobite.playtimerewards.motd;

import java.io.File;
import java.net.URL;

import org.apache.commons.io.FileUtils;
import org.bukkit.scheduler.BukkitRunnable;

import com.github.exobite.playtimerewards.CountMain;
import com.github.exobite.playtimerewards.vCheck;

public class MOTDGet {
	
	public abstract class rVal{
		
		public abstract void r(Object...objects);
		
	}
	
	//private static final String FILE_URL = "https://rebrand.ly/motd";	OLD URL, doesn´t use the new control format
	private static final String FILE_URL = "https://rebrand.ly/pr-newmotd";
	
	//public String motd;
	
	public MOTDGet(){
		//motd = "NONE";
	}

	public void setMotd() {
		if(CountMain.loadFull != vCheck.FULL) return;
		//Code from Spiget code examples
		Callback<File> callback = new Callback<File>(){
			public void execute(File f){
				CountMain.getInstance().setMotd(f);
			}
		};
		doAsyncGetFile(callback);
	}
	
	private void doAsyncGetFile(final Callback<File> cb) {
	    new BukkitRunnable() {
	        @Override
	        public void run(){
	        	try {
					final File f = new File(CountMain.getInstance().getDataFolder() + File.separator + "motd.txt");
					URL l = new URL(FILE_URL);
					FileUtils.copyURLToFile(l, f);
		            new BukkitRunnable() {
		                @Override
		                public void run(){
		                    cb.execute(f);
		                }
		            }.runTask(CountMain.getInstance());
				} catch (Exception e) {
					e.printStackTrace();
				}
	        }
	    }.runTaskAsynchronously(CountMain.getInstance());
	}
	
	public void doAsyncGetFile(final String url, final rVal rv) {
	    new BukkitRunnable() {
	        @Override
	        public void run(){
	        	try {
					final File f = new File(CountMain.getInstance().getDataFolder() + File.separator + "tempData.txt");
					URL l = new URL(url);
					FileUtils.copyURLToFile(l, f);
		            new BukkitRunnable() {
		                @Override
		                public void run(){
		                    rv.r(f);
		                }
		            }.runTask(CountMain.getInstance());
				} catch (Exception e) {
					e.printStackTrace();
				}
	        }
	    }.runTaskAsynchronously(CountMain.getInstance());
	} 
	
}
