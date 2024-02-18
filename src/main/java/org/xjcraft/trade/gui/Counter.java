package org.xjcraft.trade.gui;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.xjcraft.trade.StockMarket;
import org.xjcraft.trade.config.Config;
import org.xjcraft.trade.config.IconConfig;
import org.xjcraft.trade.config.MessageConfig;
import org.xjcraft.trade.utils.ItemUtil;
import org.xjcraft.trade.utils.StringUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Counter implements InventoryHolder, StockMarketGui {
    private final StockMarket plugin;
    private final Inventory inventory;
    private List<Map<String, Object>> trades;

    public Counter(StockMarket plugin, Player player) {
        this.plugin = plugin;
        inventory = Bukkit.createInventory(this, 54, Config.config.getTitle_offer());
        inventory.setItem(Slot.BAG, ItemUtil.getSwitchBagButton());
//        inventory.setItem(Slot.CLOSE, IconConfig.config.getClose());
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> update(player));
    }

    private void update(Player player) {
        trades = plugin.getManager().getTrades(player);
        plugin.getServer().getScheduler().runTask(plugin, () -> refresh(player));
    }

    private void refresh(Player player) {
        for (int i = 0; i < Slot.BAG; i++) {
            if (i < trades.size()) {
                Map<String, Object> trade = this.trades.get(i);
                ItemStack itemStack = plugin.getManager().getItemStack((String) trade.get("item"), (String) trade.get("hash"));
                ItemMeta itemMeta = itemStack.getItemMeta();
                String s = StringUtil.applyPlaceHolder(MessageConfig.config.getTrade(), new HashMap<String, String>() {{
                    put("operation", (Boolean) trade.get("sell") ? "卖出" : "收购");
                    put("type", plugin.getManager().getTranslate(itemStack));
                    put("subtype", (String) trade.get("hash"));
                    put("currency", (String) trade.get("currency"));
                    put("time", trade.get("create_time").toString());
                    put("id", String.valueOf((int) trade.get("id")) + "");
                    put("number", String.valueOf((int) trade.get("trade_number")) + "");
                    put("price", String.valueOf((int) trade.get("price")) + "");
                }});
                itemMeta.setLore(Arrays.asList(s.split("\n")));
                itemStack.setItemMeta(itemMeta);
                inventory.setItem(i, itemStack);
            } else {
                inventory.setItem(i, null);
            }
        }
    }


    @Override
    public Inventory getInventory() {
        return inventory;
    }

    @Override
    public void onClick(Player player, int slot,Boolean viewDisplay,ItemStack itemStack) {
        switch (slot) {
            case Slot.BAG:
                player.openInventory(new Bag(plugin, player).getInventory());
                break;
//            case Slot.CLOSE:
//                player.closeInventory();
//                break;
            default:
                if (slot < trades.size() && slot >= 0) {
                    collect(player, slot);
                    player.sendMessage(MessageConfig.config.getCollectMsg());
                }
                break;
        }

    }

    private void collect(Player player, int slot) {
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            synchronized (this) {
                try {
                    cancelTrade(player, slot);
                } catch (NumberFormatException e) {

                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    update(player);
                }
            }
        });
    }

    private void cancelTrade(Player player, int slot) {
        Map<String, Object> trade = trades.get(slot);
        if (trade.isEmpty()) return;
        plugin.getManager().delete("stock_trade",(int) trade.get("id"));
        plugin.getManager().cancelTrade(player, trade);
    }

    interface Slot {
        int BAG = 53;
        int CLOSE = 51;

    }

}
