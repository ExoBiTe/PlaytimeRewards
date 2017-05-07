package com.github.exobite.playtimerewards.motd;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.apache.commons.io.FileUtils;

import com.github.exobite.playtimerewards.CountMain;

public class MessageManage {
	
	String motdIndi = "|-";			//Example: |-I am the MOTD!
	String boolModIndi = ":-";		//Example: :-<Boolean>.<bool>		:-debugMode.true
	
	//Getter
	private String motd;
	private List<String> lines;
	
	public MessageManage(File f){
		try {
			lines = FileUtils.readLines(f, "utf-8");
		} catch (IOException e) {
			e.printStackTrace();
		}
		f.delete();
		process();
	}
	
	private void process(){
		for(String row:lines){
			if(row.toLowerCase().contains(motdIndi)){
				motd = row.replace(motdIndi, "");
			}else if(row.toLowerCase().contains(boolModIndi)){
				processBoolean(row);
			}
		}
	}
	
	private void processBoolean(String str){
		if(!str.contains(".") || !str.contains(boolModIndi)) return;
		String w = str.replace(boolModIndi, "");
		String[] data = w.split("\\.");
		if(data.length<2) return;
		String bool = data[0];
		boolean val = Boolean.valueOf(data[1]);
		if(data.length==3){
			System.out.println("[PlaytimeRewards] NOTIFICATION: "+data[2]);
		}
		if(bool.equalsIgnoreCase("debugMode")){
			CountMain.debugMode = val;
		}else if(bool.equalsIgnoreCase("allowMySQL")){
			CountMain.allowMySQL = val;
		}else if(bool.equalsIgnoreCase("autoUpdate")){
			CountMain.autoUpdate = val;
		}
		
	}
	
	public String getMotd(){
		return motd;
	}

}
