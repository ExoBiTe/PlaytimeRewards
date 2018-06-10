package com.github.exobite.playtimerewards.update;

public enum Message {
	
	PLAYER_NOTIFICATION_REWARD_OBTAINED, GLOBAL_NOTIFICATION_REWARD_OBTAINED,
	
	;
	
	private String message = null;
	private int argAmount = 0;
	
	Message() {}
	
	Message(String message){
		this.message = message;
	}
	
	Message(String message, int argAmount){
		this.argAmount = argAmount;
	}
	
	public void setData(String message, int argAmount) {
		this.message = message;
		this.argAmount = argAmount;
	}
	
	public String getMessage() {
		if(message==null) {
			return this.toString();
		}
		return message;
	}
	
	public int getArgAmount() {
		return argAmount;
	}

}
