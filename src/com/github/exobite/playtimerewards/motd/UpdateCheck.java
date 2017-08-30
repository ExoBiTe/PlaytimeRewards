package com.github.exobite.playtimerewards.motd;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

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
	    			@SuppressWarnings("deprecation")
					JSONArray versionsArray = (JSONArray) JSONValue.parseWithException(IOUtils.toString(new URL(String.valueOf(VERSION_URL))));
	    			
	    			String lastVersionString = ((JSONObject) versionsArray.get(versionsArray.size() - 1)).get("name").toString();
	    			
	    			//System.out.println("Found "+lastVersionString+", actual is "+myVersion+". Newer: "+checkNewer(myVersion, lastVersionString));
	    			
	    			//if(!(lastVersionString.equals(myVersion))){
	    			if(checkNewer(myVersion, lastVersionString)){
	    				if(!allowBeta && lastVersionString.contains("BETA")){
	    					return;
	    				}
	    				@SuppressWarnings("deprecation")
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
    
    private static boolean checkNewer(String actualVersion, String newVersion){
		List<Integer> numbers1 = new ArrayList<Integer>();
		List<Integer> numbers2 = new ArrayList<Integer>();
		try{
			for(String s:actualVersion.replace(".", ",").split(",")){
				int toAdd = Integer.valueOf(s);
				numbers1.add(toAdd);
			}
			for(String s:newVersion.replace(".", ",").split(",")){
				int toAdd = Integer.valueOf(s);
				numbers2.add(toAdd);
			}
		}catch(Exception e){
			return true;
		}
		int size;
		if(numbers1.size()>numbers2.size()){
			size = numbers1.size();
			int missing = numbers1.size() - numbers2.size();
			for(int i=0;i<missing;i++){
				numbers2.add(0);
			}
		}else if(numbers1.size()<numbers2.size()){
			size = numbers2.size();
			int missing = numbers2.size() - numbers1.size();
			for(int i=0;i<missing;i++){
				numbers1.add(0);
			}
		}else{
			size = numbers1.size();
		}
		for(int i=0;i<size;i++){
			int i1 = numbers1.get(i);
			int i2 = numbers2.get(i);
			if(i1>i2){
				return false;
			}else if(i1<i2){
				return true;
			}
		}
		//This should never get reached
		return false;
	}
}
