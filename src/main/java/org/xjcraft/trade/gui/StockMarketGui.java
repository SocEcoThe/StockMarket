package org.xjcraft.trade.gui;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public interface StockMarketGui {
    public void onClick(Player whoClicked, int slot,Boolean viewDisplay,ItemStack item);
}
