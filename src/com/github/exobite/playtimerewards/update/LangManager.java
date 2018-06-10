package com.github.exobite.playtimerewards.update;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;

import org.apache.commons.lang3.StringUtils;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public class LangManager {
	
	private static String varValue1 = "VAR[", varValue2 = "]";
	private static char colorCode = '&';
	private static String fileName = "lang.yml";
	
	private static JavaPlugin main;
	
	public LangManager(JavaPlugin main) {
		LangManager.main = main;
		fillDefaultFile("lang.yml");
		loadMessages();
	}
	
	public static void loadMessages() {
		File f = new File(main.getDataFolder()+File.separator+"lang.yml");
		FileConfiguration fc = YamlConfiguration.loadConfiguration(f);
		for(Message m:Message.values()) {
			if(!fc.contains(m.toString())) continue;
			String msg = fc.getString(m.toString());
			for(int i=0;i<10;i++) {
				//Replace numbers 0-9 with %N%
				if(!msg.contains(i+"")) break;	//Break loop when number i isn´t found
				msg = msg.replace(i+"", "%N%");
			}
			int amount = StringUtils.countMatches(msg, varValue1 + "%N%" + varValue2);
			msg = fc.getString(m.toString());
			m.setData(msg, amount);
		}
	}
	
	public static void fillDefaultFile(String filePath) {
		if(main==null) return;
		File f = new File(main.getDataFolder()+File.separator+filePath);
		if(!f.exists()) {
			main.saveResource(fileName, true);
		}
		InputStream is = getResource(filePath);
		if(is==null) {
			System.err.println("Couldn´t find "+filePath+" in project files.");
			return;
		}
		InputStreamReader rd = new InputStreamReader(is);
		FileConfiguration fcDefault = YamlConfiguration.loadConfiguration(rd);
		boolean change = false;
		FileConfiguration fc = YamlConfiguration.loadConfiguration(f);
		for(String key:fcDefault.getKeys(true)) {
			if(!fc.contains(key)) {
				System.err.println("Couldn´t find "+key+" in the "+filePath+" file.");
				change = true;
				fc.set(key, fcDefault.getString(key, "DEFAULT_MESSAGE_NOT_FOUND"));
				
				//Debug
				/*for(String k:fcDefault.getKeys(true)) {
					System.out.println(k+" is "+fcDefault.get(k));
				}*/
			}
		}
		if(change) {
			//Save Fileconfig to file
			try {
				fc.save(f);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	public static String getFormatMessage(Message msg, String[] args, ChatColor cl) {
		String message = ChatColor.translateAlternateColorCodes(colorCode, msg.getMessage());
		if(cl!=null) {
			message = cl + message;
		}
		if(args==null && msg.getArgAmount()==0) {
			return message;
		}else if(msg.getArgAmount() == args.length) {
			for(int i=0;i<msg.getArgAmount();i++) {
				message = message.replace(varValue1+i+varValue2, args[i]);
			}
		}else {
			//Wrong argamount given
			System.err.println(main.getDescription().getName()+": Wrong Langargs given. Make sure you´ve defined enough args ("+varValue1+"x"+varValue2+" for "+msg.toString()+".");
			throw new NullPointerException();
		}
		
		return message;
	}
	
	public static void setColorCode(char colorCode) {
		LangManager.colorCode = colorCode;
	}
	
	public static void setVariableString1(String varValue1) {
		LangManager.varValue1 = varValue1;
	}
	
	public static void setVariableString2(String varValue2) {
		LangManager.varValue2 = varValue2;
	}
	
	//Code from Spigot, modified a bit
    public static InputStream getResource(String filename) {
        if (main == null) {
            throw new IllegalArgumentException("Main cannot be null");
        }
        return main.getResource(filename);
    }
    
    public static void saveResource(String resourcePath, boolean replace) {
        if (resourcePath == null || resourcePath.equals("")) {
            throw new IllegalArgumentException("ResourcePath cannot be null or empty");
        }
        
        //Checks if main is != null
        if(main==null) {
        	throw new IllegalArgumentException("LangManager.Main cannot be null or empty");
        }

        resourcePath = resourcePath.replace('\\', '/');
        InputStream in = getResource(resourcePath);
        if (in == null) {
            throw new IllegalArgumentException("The embedded resource '" + resourcePath + "' cannot be found.");
        }

        File outFile = new File(main.getDataFolder(), resourcePath);
        int lastIndex = resourcePath.lastIndexOf('/');
        File outDir = new File(main.getDataFolder(), resourcePath.substring(0, lastIndex >= 0 ? lastIndex : 0));
        if (!outDir.exists()) {
            outDir.mkdirs();
        }
        
        //File writing
        try {
            if (!outFile.exists() || replace) {
                OutputStream out = new FileOutputStream(outFile);
                byte[] buf = new byte[1024];
                int len;
                while ((len = in.read(buf)) > 0) {
                    out.write(buf, 0, len);
                }
                out.close();
                in.close();
            } else {
                System.out.println("Could not save " + outFile.getName() + " to " + outFile + " because " + outFile.getName() + " already exists.");
            }
        } catch (IOException ex) {
            System.out.println("Could not save " + outFile.getName() + " to " + outFile);
            ex.printStackTrace();
        }
    }
	
}
