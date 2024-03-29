package org.xjcraft.trade.gui;

import org.bukkit.Bukkit;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.xjcraft.trade.StockMarket;
import org.xjcraft.trade.config.Config;
import org.xjcraft.trade.config.IconConfig;
import org.xjcraft.trade.utils.ItemUtil;

public class Menu implements InventoryHolder, StockMarketGui {
    private final String currency;
    private final ItemStack item;
    private Sign sign;
    private String locString;
    Inventory inventory;
    private StockMarket plugin;


    public Menu(StockMarket plugin, Player player, String currency, String location,ItemStack item, Sign sign) {
        this.plugin = plugin;
        this.currency = currency;
        this.item = item;
        this.sign = sign;
        this.locString=location;
        inventory = Bukkit.createInventory(this, 27, Config.config.getTitle_menu());
        inventory.setItem(Slot.buy,ItemUtil.getBuy());
        inventory.setItem(Slot.sell, ItemUtil.getSell());
        inventory.setItem(Slot.bag, ItemUtil.getSwitchBagButton());
        inventory.setItem(Slot.counter, ItemUtil.getSwitchCounterButton());
    }


    @Override
    public Inventory getInventory() {
        return inventory;
    }

    @Override
    public void onClick(Player player, int slot,Boolean viewDisplay,ItemStack itemStack) {
        switch (slot) {
            case Slot.buy:
                player.openInventory(new Shop(plugin, player, currency,locString,item, sign, Shop.ShopMode.BUY).getInventory());
                break;
            case Slot.sell:
                player.openInventory(new Shop(plugin, player, currency,locString,item, sign, Shop.ShopMode.SELL).getInventory());
                break;
            case Slot.bag:
                player.openInventory(new Bag(plugin, player).getInventory());
                break;
            case Slot.counter:
                player.openInventory(new Counter(plugin, player).getInventory());
                break;
        }
    }

    interface Slot {
        int buy = 12;
        int sell = 14;
        int bag = 25;
        int counter = 26;
    }
}
