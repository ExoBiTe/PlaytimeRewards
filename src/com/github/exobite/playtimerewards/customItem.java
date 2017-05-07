package com.github.exobite.playtimerewards;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class customItem {
	
	String Name;
	List<String> Lore;
	Material Mat;
	int Amount;
	short data;
	ItemStack is;
	Map<Enchantment, Integer> Enchants;
	
	public customItem(String Name, ArrayList<String> Lore, Material Mat, short Damage, int Amount){
		this.Name = Name;
		this.Lore = Lore;
		this.Mat = Mat;
		this.Amount = Amount;
		Enchants = new HashMap<Enchantment, Integer>();
		data = Damage;
		is = new ItemStack(this.Mat, this.Amount, data);
		ItemMeta Meta = is.getItemMeta();
		Meta.setDisplayName(this.Name);
		if(this.Lore != null) Meta.setLore(this.Lore);
		is.setItemMeta(Meta);
	}
	
	public customItem(ItemStack is){
		this.is = is;
		Amount = is.getAmount();
		Mat = is.getType();
		data = is.getDurability();
		Enchants = is.getEnchantments();
		Lore = is.getItemMeta().getLore();
	}
	
	public customItem addEnchant(Enchantment e, int lv){
		if(Enchants.containsKey(e)) return this;
		Enchants.put(e, lv);
		ItemMeta meta = is.getItemMeta();
		meta.addEnchant(e, lv, true);
		is.setItemMeta(meta);
		return this;
	}
	

}
