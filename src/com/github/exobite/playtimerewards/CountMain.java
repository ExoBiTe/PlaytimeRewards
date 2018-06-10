package com.github.exobite.playtimerewards;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import com.github.exobite.playtimerewards.motd.AutoUpdate;
import com.github.exobite.playtimerewards.motd.MOTDGet;
import com.github.exobite.playtimerewards.motd.MessageManage;
import com.github.exobite.playtimerewards.motd.UpdateCheck;
import com.github.exobite.playtimerewards.update.LangManager;
import com.github.exobite.playtimerewards.Metrics;

public class CountMain extends JavaPlugin {
	
	static Map<UUID, playerData> pData;
	
	//New Reward File
	static Map <String, RewardObject> RewardList;
	static Map <String, Long> RewardLongs;
	public static File nfDataDir;
	
	public static File pDataDir;
	
	private static CountMain instance;
	
	public FileConfiguration config;
	
	public static vCheck loadFull;
	
	static char colorChar;
	static boolean msgUnknownLoops;
	static boolean updateNotification;
	//static boolean dobetaUpdateNotification;	//Not used anymore!
	public static boolean autoUpdate;
	static int mainClockSpeed;
	static long dataSaveInterval;
	
	static boolean forceOwnCommands;
	static boolean vanillaCount;
	static boolean allowBetaFeatures;		//Activates Beta Features
	static boolean allowMotd;
	static boolean unlocked;
	static String motd;
	private MOTDGet motdget;
	private int preAutoUpdateCounter;
	
	static int mainClockId;
	
	public static Object updateObj[];
	static boolean updateAvaible;
	static boolean downloadDone;
	
	static java.sql.Connection con;
	static boolean mysql, useSSL;
	static String user, password, database, host, port;
	static String[] mysqlDat;
	
	static ItemStack emptyItem;
	static List<Inventory> guis;
	static Map<UUID, RewardObject> itemEdit;
	
	public static Utils utils;
	public static MessageManage mm;
	
	//List of all loaded Plugin´s commands
	static List<String> Commands;
	static List<String> alias;
	
	//Loads upon first Player Join to show Offline Player Playtime
	static Map<UUID, Long> loggedPlaytimes;
	static Map<Long, UUID> topPlaytimes;
	static int topAmount;
	
	//Data for Debugging!
	public static boolean debugMode;
	
	//Set to false to prevent using the MySQL Connection!
	public static boolean allowMySQL = true;
	
	@Override
	public void onEnable(){
		long time = System.currentTimeMillis();
		utils = new Utils();
		new LangManager(this);				//New Code, Move over to v3.0 rewrite
		loadFull = utils.VersionsCheck();	//New VersionCheck
		instance = this;
		pData = new HashMap<UUID, playerData>();
		topPlaytimes = new HashMap<Long, UUID>();
		Commands = new ArrayList<String>();
		alias = new ArrayList<String>();
		itemEdit = new HashMap<UUID, RewardObject>();
		pDataDir = new File(getDataFolder() + File.separator + "playerData.yml");
		nfDataDir = new File(getDataFolder() + File.separator + "rewards.yml");
		guis = new ArrayList<Inventory>();
		if(loadFull == vCheck.FULL){
			emptyItem = new customItem(" ", null, Material.STAINED_GLASS_PANE, (short) 15, 1).is;
		}else{
			emptyItem = new customItem(" ", null, Material.WOOL, (short) 15, 1).is;
		}
		
		RewardList = new HashMap<String, RewardObject>();
		RewardLongs = new HashMap<String, Long>();
		
		getServer().getPluginManager().registerEvents(new Listeners(), this);
		
		loadConfig();
		loadRewards();
		loadOtherPluginCommands();
		
		mainClockId = mainClock();
		asyncClock();
		if(dataSaveInterval>0) saveDataClock();
		if(unlocked) System.out.println("Unlocked Mode PTR_v."+this.getDescription().getVersion());
		
		manageMetrics();
		
		//Prevent onJoin Exceptions at a reload
		if(Bukkit.getOnlinePlayers() != null){if(Bukkit.getOnlinePlayers().size()>0){
				for(Player p:Bukkit.getOnlinePlayers()){
					new playerData(p);
				}
			}
		}
		
		if(updateNotification) {
			updateAvaible = false;
			if(loadFull == vCheck.FULL && updateAvaible){				//Impossible.
				/*Calendar cal = GregorianCalendar.getInstance();
				Date d = new Date();
				cal.set(Calendar.DAY_OF_MONTH, 12);	//Day of Release
				cal.set(Calendar.MONTH, 7);			//Month of Release
				cal.set(Calendar.YEAR, 2017);		//Year of Release
				d.setHours(19);						//Release time
				d.setMinutes(5);
				cal.setTime(d);
				
				long should = cal.getTimeInMillis() + 6*3600000;
				System.out.println(System.currentTimeMillis() +">"+ should+"?");
				if(System.currentTimeMillis() > should){*/
					//System.out.println("Started Update!");
					new UpdateCheck();
					UpdateCheck.startUpdate(getDescription().getVersion(), true);
				//Auto-Update is DISABLED
				//}
			}
		}
		if(allowMotd){
			motdget = new MOTDGet();
			motdget.setMotd();
			//setMotd(new File(getDataFolder() + File.separator + "example.txt"));
		}
		
		if(mysql) {
			if(!mysql_con()) return;
		}
		
		new BukkitRunnable(){
			@Override
			public void run() {
				CountMain.utils.wholeData();
			}
		}.runTaskLater(instance, 50);
		loadUserData();
		long time2 = System.currentTimeMillis() - time;
		send("Enabled! ("+time2+"ms)");
	}
	
	private void manageMetrics(){
		//Starting Metrics/BStats sending!
		try {
			Metrics m = new Metrics(this);
			m.addCustomChart(new Metrics.SimplePie("reward_count") {
		        @Override
		        public String getValue() {
		            return RewardList.size()+"";
		        }
		    });
			m.addCustomChart(new Metrics.SimplePie("auto_update") {
		        @Override
		        public String getValue() {
		            return autoUpdate+"";
		        }
		    });
			m.addCustomChart(new Metrics.SimplePie("save_type") {
		        @Override
		        public String getValue() {
		        	if(mysql){
		        		return "MySQL-Storage";
		        	}else{
		        		return "Flat-File Storage";
		        	}
		            
		        }
		    });
			if(debugMode){
				System.out.println("mDat: reward_count: "+RewardList.size());
				System.out.println("mDat: auto_update: "+autoUpdate);
			}
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public void updateConfig(File toSave){
		try {
			InputStream is = getResource("config.yml");
			File s = new File(getDataFolder()+File.separator+"nothing_to_see_here");
			OutputStream os;
			os = new FileOutputStream(s);
			IOUtils.copy(is, os);
			os.close();
			FileConfiguration cfg = YamlConfiguration.loadConfiguration(s);
			FileConfiguration cfgOld = YamlConfiguration.loadConfiguration(toSave);
			Set<String> keys = cfg.getKeys(true);
			for(String Key:keys){
				Object e = cfgOld.get(Key, null);
				if(e==null){
					Object Value = cfg.get(Key);
					cfgOld.set(Key, Value);
				}
			}
			cfgOld.save(toSave);
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}
	
	public void setMotd(File f){
		preAutoUpdateCounter++;
		MessageManage mm = new MessageManage(f);
		motd = mm.getMotd();
		
		if(!motd.equalsIgnoreCase("none")){
			send(motd);
		}
		//Downloads the newest Update
		if(preAutoUpdateCounter==2 && autoUpdate && updateAvaible){
			new AutoUpdate();
		}
	}
	
	public void setUpdateData(Object[] obj){
		preAutoUpdateCounter++;
		updateObj = obj;
		if(updateObj.length == 2){
			//if(!checkNewer(getDescription().getVersion()+"", updateObj[0]+""))
			updateAvaible = true;
			send("\n=======================================================\n"
					+ "==  "+getDescription().getName()+"\n"
					+ "==  New update avaible:\n"
					+ "==  New version: " + updateObj[0]+"\n"
					+ "==  Your version: " + getDescription().getVersion()+"\n"
					+ "==  What's new: " + updateObj[1]+"\n"
					+ "=======================================================");
		}else{
			updateAvaible = false;
		}
		//Downloads the newest Update
		if(preAutoUpdateCounter==2 && autoUpdate && updateAvaible){
			new AutoUpdate();
		}
	}
	
	public void autoUpdateDone(){
		//Placeholder, gets called after Update download
		downloadDone = true;
		send("Downloaded Version "+updateObj[0]+"!");
		send("Restart the Server to activate the New Version!");
	}
	
	@Override
	public void onDisable(){
		
		//saves all data on disable for online players (reload) :/
		if(Bukkit.getOnlinePlayers() != null){if(Bukkit.getOnlinePlayers().size()>0){
			for(Player p:Bukkit.getOnlinePlayers()){
				playerData pDat = pData.get(p.getUniqueId());
				pDat.logOut(false, false);
			}
			}
		}
		for(String rName:RewardList.keySet()){
			RewardObject rw = RewardList.get(rName);
			if(rw.getItemChange()) rw.saveItem();
		}
		
		Bukkit.getScheduler().cancelTasks(this);
		
		pData.clear();
		RewardList.clear();
		RewardLongs.clear();
		
		try {
			if(con != null && !con.isClosed() && mysql) con.close();
		} catch (SQLException e) {
			
			e.printStackTrace();
		}
		//Installs the new Version of the Plugin.
		if(!downloadDone) return;
		File src = new File(getDataFolder()+File.separator+"PR_NewVersion.jar");
		if(!src.exists()){
			System.err.println("[PlaytimeRewards] Couldn´t find the downloaded File!\nDid it got deleted?");
			return;
		}
		File dst = new File("plugins"+File.separator+"PlaytimeRewards.jar");
		try {
			FileUtils.copyFile(src, dst);
		} catch (IOException e) {
			e.printStackTrace();
		}
		src.delete();
		send("Installed New Version!");
	}
	
	public boolean onCommand(CommandSender sender, Command arg1, String arg2, String[] args) {
		if(Commands.contains(arg2.toLowerCase()) || alias.contains(arg2.toLowerCase())){						//Producing errors..?
			if(debugMode){
				send("Another Plugin uses this Command, returning...");
			}
			return true;
		}
		
		if(arg2.toLowerCase().equals("pr") || arg2.toLowerCase().equals("playtimerewards")){
			Commands cmd = new Commands();
			return cmd.onCommand(sender, arg1, arg2, args);
		}else if(arg2.toLowerCase().equals("pt") || arg2.toLowerCase().equals("playtime")){
			playtimeCommand cmd = new playtimeCommand();
			return cmd.onCommand(sender, arg1, arg2, args);
		}else if(arg2.toLowerCase().equals("pttop") || arg2.toLowerCase().equals("playtimetop")){
			if(!sender.hasPermission("timerewards.seeTop")){
				sender.sendMessage(ChatColor.RED+"Sorry, you don´t have enough Permissions to do this.");
				return true;
			}
			String s = ChatColor.GOLD+"Displaying the top "+topAmount+" Players:";
			for(Long l:topPlaytimes.keySet()){
				UUID id = topPlaytimes.get(l);
				Long playtime;
				if(CountMain.pData.containsKey(id)){
					playtime = CountMain.pData.get(id).getPlaytimeNow();
				}else{
					playtime = loggedPlaytimes.get(id);
				}
				s = s + "\n"+ChatColor.GOLD+l+". "+ChatColor.GREEN+Bukkit.getOfflinePlayer(id).getName()+ChatColor.GOLD+": "+utils.getTime(playtime);
			}
			sender.sendMessage(s);
			return true;
		}else{
			return false;
		}
	}
	
	//Loads all Commands and aliases for other Plugins
	private void loadOtherPluginCommands(){
		PluginManager pm = this.getServer().getPluginManager();
		for(Plugin p:pm.getPlugins()){
			if(p == pm.getPlugin(this.getDescription().getName())) continue;			//Skip this Plugin
			Map<String, Map<String, Object>> cmds = p.getDescription().getCommands();
			if(cmds == null) return;
			for(String x1:cmds.keySet()){
				Map<String, Object> m2 = cmds.get(x1);
				if(!Commands.contains(x1)) Commands.add(x1.toLowerCase());
				for(String x2:m2.keySet()){
					Object ob = m2.get(x2);
					if(debugMode) System.out.println("PluginLoad: "+x1+"."+x2+"="+ob);
					if(ob instanceof String && x2.equalsIgnoreCase("aliases")){
						String al = (String) ob;
						if(!alias.contains(al)) alias.add(al.toLowerCase());
					}
				}
			}
		}
	}
	
	private void loadConfig(){
		File f = new File(getDataFolder() + File.separator + "config.yml");
		File f2 = new File(getDataFolder() + File.separator + "Config.yml");
		if(f2.exists() && !f.exists()){
			if(debugMode) send("DebugMSG: Config.yml renamed to config.yml");
			f2.renameTo(f);
		}
		f = new File(getDataFolder() + File.separator + "config.yml");
		if(!(f.exists())) {
			send("config.yml created and loaded!");
			saveResource("config.yml", true);
		}
		config = YamlConfiguration.loadConfiguration(f);
		String configV = config.getString("Main.Version");
		if(!(configV.equals(getDescription().getVersion()))){
			File f3 = new File(getDataFolder() + File.separator + "config_v"+configV+".yml");
			f.renameTo(f3);
			saveResource("config.yml", true);
			send("config.yml updated to "+getDescription().getName()+" v"+getDescription().getVersion()+"!");
		}
		
		String colorCharStr = config.getString("Main.ColorChar");
		colorChar = colorCharStr.charAt(0);
		
		mysql = config.getBoolean("Main.MySQL", false);
		msgUnknownLoops = config.getBoolean("Main.msgUnknownLoops", false);
		forceOwnCommands = config.getBoolean("Main.ForceCommands", false);
		vanillaCount = config.getBoolean("Main.vanillaPlaytime", true);				//Since 2.8 Optional, not anymore in the config
		allowBetaFeatures = config.getBoolean("Main.betaAccess", false);			//Optional, not in default Config
		if(!getDescription().getVersion().toLowerCase().contains("beta")) allowBetaFeatures = false;
		allowMotd = config.getBoolean("Notification.allowMessages", true);
		topAmount = config.getInt("Main.TopAmount", 10);
		autoUpdate = config.getBoolean("Main.autoUpdate", true);
		debugMode = config.getBoolean("Main.DebugMode", false);						//Optional, not in default Config
		if(debugMode) send("Running Plugin in Debug Mode!");
		if(debugMode) debug(f);
		if(debugMode) debug(f2);
		mainClockSpeed = config.getInt("Main.MainClockSpeed", 50);					//Optional, not in default Config
		dataSaveInterval = config.getLong("Main.DataSaveInterval", 1800) *20;
		if(mainClockSpeed!=50) send("Custom Mainclockspeed found and set to "+mainClockSpeed+"!");
		unlocked = config.getBoolean("Main.UnlockedMode", false);
		
		updateNotification = config.getBoolean("Notification.searchUpdates", true);
		//betaUpdateNotification = config.getBoolean("Notification.searchBeta", false);		//Not used anymore!
		
		if(!allowMySQL && mysql) {
			mysql = false;
			send("Sorry, but MySQL is disabled in this Version.");
		}
		
		if(mysql){
			host = config.getString("MySQL.Host", "");
			port = config.getString("MySQL.Port", "");
			user = config.getString("MySQL.User", "");
			password = config.getString("MySQL.Password", "");
			database = config.getString("MySQL.Database", "");
			useSSL = config.getBoolean("MySQL.useSSL", false);
			if(host.equals("") || port.equals("") || user.equals("") || database.equals("")){
				mysql = false;
			}
		}
		
		if(!(pDataDir.exists()) && mysql==false){
			saveResource("playerData.yml", true);
		}
	}
	
	private boolean mysql_con() {	
		if(!createCon()) {
			Bukkit.getPluginManager().disablePlugin(this);
			return false;
		}
		checkMySQL();
		return true;
	}
	
	private void checkMySQL(){
		try {
			if(!mysql) return;
			if(con == null || con.isClosed()) return;
			String mysqlURL = "jdbc:mysql://" + host + ":" + port + "/?user=" + user + "&password=" + password + "&autoReconnect=true&useSSL="+useSSL;
			Connection createDb = DriverManager.getConnection(mysqlURL);
			if(!createDb.isClosed()){
				createDb.createStatement().executeUpdate("CREATE DATABASE IF NOT EXISTS "+database);
				createDb.close();
				//send("Created MySQL Database "+database+"!");
				if(!createDb.isClosed() && debugMode){
					send("Couldn´t close the database-creating mysql connection!");
				}
			}
			con.createStatement().executeUpdate("CREATE TABLE IF NOT EXISTS PR_PlayerData (" +
					"UUID CHAR(36) PRIMARY KEY" +
					", lastLogin BIGINT(2)" +
					", playTime BIGINT(2)" +
					", sessionCount BIGINT(2)" +
					")");
			
			con.createStatement().executeUpdate("CREATE TABLE IF NOT EXISTS PR_PlayerLoops (" +
					"id MEDIUMINT NOT NULL AUTO_INCREMENT" +
					", UUID CHAR(36)" +
					", loopName VARCHAR(36)" +
					", loopValue BIGINT(2)" +
					", PRIMARY KEY (id)" +
					")");
			
			try{	//Upgrade Databases
				con.createStatement().executeUpdate("ALTER TABLE PR_PlayerData ADD sessionCount BIGINT(2);");
			}catch(SQLException e){
				//Nothing, this just gets catched if it already exists.
			}
			
		} catch (SQLException e) {
			
			e.printStackTrace();
		}
	}
	
	private boolean createCon() {
		try {
			Class.forName("com.mysql.jdbc.Driver");
		} catch(ClassNotFoundException e) {
			send("Couldn´t find the MYSQL-Drivers.");
			return false;
		}
		try {
			Connection createDb = DriverManager.getConnection("jdbc:mysql://" + host + ":" + port + "/?user=" + user + "&password=" + password + "&autoReconnect=false");
			if(!createDb.isClosed()){
				createDb.createStatement().executeUpdate("CREATE DATABASE IF NOT EXISTS "+database);
				createDb.close();
				if(!createDb.isClosed() && debugMode){
					send("Couldn´t close the database-creating mysql connection!");
				}
			}
			con = DriverManager.getConnection("jdbc:mysql://" + host + ":" + port + "/" + database + "?user=" + user + "&password=" + password + "&autoReconnect=true");
			if(!(con.isClosed())) {
				send("Connected to MySQL at "+host+":"+port);
				return true;
			}
			
		} catch(SQLException e) {
			System.err.println("["+getDescription().getName()+"] Error at creating the MySQL Connection. Error:" + e.getMessage());
			return false;
		}
		return false;
	}
	
	private void debug(File f){
		send("Checking File "+f);
		if(!(f.exists())){
			send("File "+f+" not Found!");
			return;
		}
		FileConfiguration load = YamlConfiguration.loadConfiguration(f);
		Set<String> FileCheck = load.getKeys(false);
		send("List of all found Keys(false):");
		for(String x:FileCheck){
			send(x);
		}
		Set<String> FileCheck2 = load.getKeys(true);
		send("List of all found Keys(true):");
		for(String x:FileCheck2){
			send(x);
		}
	}
	
	private void checkForOldFile(){
		File f = nfDataDir;
		File f2 = new File(getDataFolder() + File.separator + "Rewards.yml");
		FileConfiguration load = YamlConfiguration.loadConfiguration(f);
		Set<String> oldFileCheck = load.getKeys(false);
		if(oldFileCheck.contains("RewardLoops")){
			f.renameTo(new File(getDataFolder() + File.separator + "rewards_old.yml"));
			saveResource("rewards.yml", true);
			send("Old Rewards file found! Renaming it to rewards_old.yml and creating new Rewards File!");
			load = YamlConfiguration.loadConfiguration(f);
		}
		load = YamlConfiguration.loadConfiguration(f2);
		oldFileCheck = load.getKeys(false);
		if(oldFileCheck.contains("RewardLoops")){
			f2.renameTo(new File(getDataFolder() + File.separator + "rewards_old.yml"));
			saveResource("rewards.yml", true);
			send("Old Rewards file found! Renaming it to rewards_old.yml and creating new Rewards File!");
			load = YamlConfiguration.loadConfiguration(f);
		}
	}
	
	void loadRewards(){
		if(debugMode) debug(nfDataDir);
		File f = nfDataDir;
		File f2 = new File(getDataFolder() + File.separator + "Rewards.yml");
		if(f2.exists()){
			f2.renameTo(f);
		}
		if(!(f.exists())){
			saveResource("rewards.yml", true);
		}
		checkForOldFile();
		boolean gotEdit = false;
		FileConfiguration load = YamlConfiguration.loadConfiguration(f);
		if(!load.getKeys(false).contains("Rewards")){
			saveResource("rewards.yml", true);
			send("There was a fault in your Rewards.yml, reset it to default.");
		}
		load = YamlConfiguration.loadConfiguration(f);
		Set<String> RewardList = null;
		try{
			RewardList = load.getConfigurationSection("Rewards").getKeys(false);
		}catch(Exception e){
			send("Error: ");
			e.printStackTrace();
		}
		if(RewardList==null){
			
			return;
		}
		send("Loading "+RewardList.size()+" Rewards...");
		for(String rwName:RewardList){
			String px = "Rewards."+rwName+".";
			String Name = load.getString(px+"RewardName");
			String Text = load.getString(px+"RewardText");
			List<String> playerCommand = load.getStringList(px+"RewardCommand");
			List<String> consoleCommand = load.getStringList(px+"RewardConsole");
			if(playerCommand == null || playerCommand.size() == 0){
				//Old Reward File Detected.
				String oldCmd = load.getString(px+"RewardCommand");
				if(!(oldCmd==null) && !oldCmd.equals("")){
					playerCommand = new ArrayList<String>(Arrays.asList(oldCmd));
					load.set(px+"RewardCommand", playerCommand);
					gotEdit = true;
				}
			}
			if(consoleCommand == null || consoleCommand.size() == 0){
				//Old Reward File Detected.
				String oldCmd = load.getString(px+"RewardConsole");
				if(!(oldCmd==null) && !oldCmd.equals("")){
					consoleCommand = new ArrayList<String>(Arrays.asList(oldCmd));
					load.set(px+"RewardConsole", consoleCommand);
					gotEdit = true;
				}
			}
			CountType cType = CountType.valueOf(load.getString(px+"CountType"));
			boolean globalNotification= load.getBoolean(px+"GlobalNotification", false);
			boolean playerNotification= load.getBoolean(px+"PlayerNotification", true);
			long playtime = load.getLong(px+"PlaytimeNeeded");
			Object rw;
			if(loadFull == vCheck.FULL){
				rw = new RewardObj(cType, rwName, Name, Text, playerCommand, consoleCommand, playtime, globalNotification, playerNotification);
			}else{
				rw = new _smallRwObj(cType, rwName, Name, Text, playerCommand, consoleCommand, playtime, globalNotification, playerNotification);
			}
			//send("Loaded "+rwName);
			boolean isLoop = load.getBoolean(px+"LoopOptions.EnableLoop", false);
			if(isLoop){
				long minPlaytime = load.getLong(px+"LoopOptions.MinPlaytime", 0);
				long maxPlaytime = load.getLong(px+"LoopOptions.MaxPlaytime", 0);
				String Permission = load.getString(px+"LoopOptions.PermissionNeeded");
				boolean waitFirst= load.getBoolean(px+"LoopOptions.WaitFirst", false);
				if(cType != null){
					if(loadFull == vCheck.FULL){
						RewardObj t = (RewardObj) rw;
						t.setLoop(minPlaytime, maxPlaytime, Permission, waitFirst);
					}else{
						_smallRwObj t = (_smallRwObj) rw;
						t.setLoop(minPlaytime, maxPlaytime, Permission, waitFirst);
					}
				}
			}
		}
		if(gotEdit){
			try {
				load.save(f);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	private void loadUserData(){
		MOTDGet gt = new MOTDGet();
		gt.doAsyncGetFile("https://rebrand.ly/pr-userdata", gt.new rVal(){

			@Override
			public void r(Object...objects) {
				if(objects==null) return;
				if(objects.length<1) return;
				if(!(objects[0] instanceof File)) return;
				new MessageManage((File) objects[0]);
			}
			
		});
	}
	
	public void reloadConfigCustom(){
		//Clean up
		/*Bukkit.getScheduler().cancelTask(mainClockId);
		RewardLongs.clear();
		RewardList.clear();
		Commands.clear();
		alias.clear();
		
		//Load new
		loadConfig();
		loadRewards();
		loadOtherPluginCommands();
		mainClockId = mainClock();
		send("Config.yml and Rewards.yml reloaded!");*/
		
		onDisable();
		onEnable();
	}
	
	public void send(String Message){
		String msg = "["+getDescription().getName()+"] "+Message;
		System.out.println(msg);
	}
	
	public static final CountMain getInstance(){
		return instance;
	}
	
	private int mainClock(){
		int taskID = Bukkit.getScheduler().scheduleSyncRepeatingTask(this , new Runnable() {
			public void run() {
				for(Player p:Bukkit.getOnlinePlayers()){
					if(!pData.containsKey(p.getUniqueId())) new playerData(p);
					playerData pDat = pData.get(p.getUniqueId());
					if(!pDat.gotData) continue;
					//Check for Loops
					for(String str:RewardList.keySet()){
						RewardObject rw = RewardList.get(str);
						boolean canGet = true;
						if(rw.minPlaytimeBool()) { if(rw.getMinPlaytime()<pDat.getPlaytimeNow()){ canGet = false;}}		//MinPlaytime
						if(rw.maxPlaytimeBool()) { if(rw.getMaxPlaytime()>pDat.getPlaytimeNow()){ canGet = false;}}		//MaxPlaytime
						if(rw.permBool()) { if(!(p.hasPermission(rw.getPerm()))){ canGet = false;}}						//Permission
						
						if(canGet){
							pDat.getReward(rw, true);
						}
					}
				}
				new BukkitRunnable(){
					public void run(){
						if(CountMain.loggedPlaytimes != null) CountMain.topPlaytimes = CountMain.utils.getTop(CountMain.topAmount, CountMain.loggedPlaytimes);
					}
				}.runTaskAsynchronously(instance);
			}
		}, mainClockSpeed, mainClockSpeed);	//Default set to 2,5 Seconds
		return taskID;
	}
	
	private int asyncClock(){
		return Bukkit.getScheduler().scheduleSyncRepeatingTask(this, new Runnable(){

			@Override
			public void run() {
				if(mysql) checkMySQLAsync();
				if(allowMotd && loadFull != vCheck.FULL){
					if(motdget == null) motdget = new MOTDGet();
					motdget.setMotd();
				}
			}
		}, 72000, 72000);
	}
	
	private int saveDataClock(){
		return Bukkit.getScheduler().scheduleSyncRepeatingTask(this, new Runnable(){

			@Override
			public void run() {
				if(debugMode) send("[PlaytimeRewards] Saving all Data...");
				//if(mysql) checkMySQL();
				if(Bukkit.getOnlinePlayers() != null){if(Bukkit.getOnlinePlayers().size()>0){
						for(Player p:Bukkit.getOnlinePlayers()){
							playerData pDat = pData.get(p.getUniqueId());
							pDat.logOut(false, true);
						}
					}
				}
				for(String rName:RewardList.keySet()){
					RewardObject rw = RewardList.get(rName);
					if(rw.getItemChange()) rw.saveItem();
				}
			}
		}, dataSaveInterval, dataSaveInterval);
	}
	
	private void checkMySQLAsync(){
		if(!mysql) return;
		Bukkit.getScheduler().runTaskAsynchronously(CountMain.getInstance(), new Runnable(){

			@Override
			public void run() {
				try {
					if(!mysql) return;
					if(con == null || con.isClosed()) return;
					Connection createDb = DriverManager.getConnection("jdbc:mysql://" + host + ":" + port + "/?user=" + user + "&password=" + password + "&autoReconnect=false");
					if(!createDb.isClosed()){
						createDb.createStatement().executeUpdate("CREATE DATABASE IF NOT EXISTS "+database);
						createDb.close();
						//send("Created MySQL Database "+database+"!");
						if(!createDb.isClosed() && debugMode){
							send("Couldn´t close the database-creating mysql connection!");
						}
					}
					con.createStatement().executeUpdate("CREATE TABLE IF NOT EXISTS PR_PlayerData (" +
							"UUID CHAR(36) PRIMARY KEY" +
							", lastLogin BIGINT(2)" +
							", playTime BIGINT(2)" +
							", sessionCount BIGINT(2)" +
							")");
					
					con.createStatement().executeUpdate("CREATE TABLE IF NOT EXISTS PR_PlayerLoops (" +
							"id MEDIUMINT NOT NULL AUTO_INCREMENT" +
							", UUID CHAR(36)" +
							", loopName VARCHAR(36)" +
							", loopValue BIGINT(2)" +
							", PRIMARY KEY (id)" +
							")");
					
					try{	//Upgrade Databases
						con.createStatement().executeUpdate("ALTER TABLE PR_PlayerData ADD sessionCount BIGINT(2);");
					}catch(SQLException e){
						//Nothing, this just gets catched if it already exists.
					}
					
				} catch (SQLException e) {
					
					e.printStackTrace();
				}
			}
			
		});
	}

}
