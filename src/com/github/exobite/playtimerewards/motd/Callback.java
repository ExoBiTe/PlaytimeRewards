package com.github.exobite.playtimerewards.motd;

public interface Callback<T> {
	
    public void execute(T response);
    
}
