package com.github.exobite.playtimerewards.motd;

import org.bukkit.scheduler.BukkitRunnable;

import com.github.exobite.playtimerewards.CountMain;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URL;

public class AutoUpdate {

	//Resource Download URL
	String URL = "https://api.spiget.org/v2/resources/32900/download";
	
	public AutoUpdate(){
		//getUpdate();
		//CountMain.getInstance().send("Starting autoUpdate!");
		if(CountMain.debugMode) CountMain.getInstance().send("AutoUpdate is disabled.");
	}
	
	private void getUpdate(){
	    new BukkitRunnable() {
	        @Override
	        public void run(){
	        	try {
					URL l = new URL(URL);
					InputStream is = l.openConnection().getInputStream();
					FileOutputStream fs = new FileOutputStream("plugins"+File.separator+"PlaytimeRewards"+File.separator+"PR_NewVersion.jar");
					byte[] file = new byte[1024];
					int read = is.read(file);
					while(read>0){
						fs.write(file, 0, read);
						read = is.read(file);
					}
					is.close();
					fs.close();
					new BukkitRunnable() {
						@Override
						public void run(){
							CountMain.getInstance().autoUpdateDone();
						}
					}.runTask(CountMain.getInstance());
				} catch (Exception e) {
					e.printStackTrace();
				}
	        }
	    }.runTaskAsynchronously(CountMain.getInstance());
	}

}
