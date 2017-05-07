package com.github.exobite.playtimerewards.motd;

import java.net.URL;

import org.apache.commons.io.IOUtils;
import org.bukkit.scheduler.BukkitRunnable;
import org.json.simple.JSONArray;
import org.json.simple.JSONValue;
import org.json.simple.JSONObject;

import com.github.exobite.playtimerewards.CountMain;

public class UpdateCheck {
	
	final static String VERSION_URL = "https://api.spiget.org/v2/resources/32900/versions?size="+Integer.MAX_VALUE+"&spiget__ua=ExobitePlugin";
    final static String DESCRIPTION_URL = "https://api.spiget.org/v2/resources/32900/updates?size="+Integer.MAX_VALUE+"&spiget__ua=ExobitePlugin";
    
    public static void startUpdate(final String myVersion, final boolean allowBeta){
		Callback<Object[]> callback = new Callback<Object[]>(){
			public void execute(Object[] o){
				CountMain.getInstance().setUpdateData(o);
			}
		};
		getUpdate(callback, myVersion, allowBeta);
    }

    private static void getUpdate(final Callback<Object[]> cb, final String myVersion, final boolean allowBeta){
    	new BukkitRunnable() {
	        @Override
	        public void run(){
	        	try{
	    			
	    			JSONArray versionsArray = (JSONArray) JSONValue.parseWithException(IOUtils.toString(new URL(String.valueOf(VERSION_URL))));
	    			
	    			String lastVersionString = ((JSONObject) versionsArray.get(versionsArray.size() - 1)).get("name").toString();
	    			
	    			if(!(lastVersionString.equals(myVersion))){
	    				if(!allowBeta && lastVersionString.contains("BETA")){
	    					return;
	    				}
	    				JSONArray updatesArray = (JSONArray) JSONValue.parseWithException(IOUtils.toString(new URL(String.valueOf(DESCRIPTION_URL))));
	    				String updateName = ((JSONObject) updatesArray.get(updatesArray.size() - 1)).get("title").toString();
	    				final Object[] update = {lastVersionString, updateName};
	    				new BukkitRunnable() {
			                @Override
			                public void run(){
			                    cb.execute(update);
			                }
			            }.runTask(CountMain.getInstance());
	    			}
	    			return;
	    		}catch(Exception e){
	    			System.err.println("Error at getting the Version! :");
	    			System.err.println(e.getMessage());
	    			return;
	    		}
	        }
	    }.runTaskAsynchronously(CountMain.getInstance());
	}
}
