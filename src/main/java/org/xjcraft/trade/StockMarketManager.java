package org.xjcraft.trade;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.zjyl1994.minecraftplugin.multicurrency.services.BankService;
import com.zjyl1994.minecraftplugin.multicurrency.utils.OperateResult;
import com.zjyl1994.minecraftplugin.multicurrency.utils.TxTypeEnum;

import org.bukkit.Material;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;
import org.xjcraft.trade.config.MessageConfig;
import org.xjcraft.trade.gui.Callback;
import org.xjcraft.trade.gui.Menu;
import org.xjcraft.trade.gui.Shop;
import org.xjcraft.trade.utils.ItemUtil;
import org.xjcraft.utils.JSON;
import org.xjcraft.utils.TranslationManager;
import org.xjcraft.trade.utils.StringUtil;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.ArrayList;
import java.util.Base64;
import java.io.File;

import javax.sql.DataSource;

// StockMarketManager类是负责处理游戏内交易市场相关操作的核心类。
public class StockMarketManager {
    private StockMarket plugin;
    private Dao dao;
    private final String SHOP_ACCOUNT = "$SHOP";
    private Map<String, String> map;

    public StockMarketManager(StockMarket stockMarket, DataSource hikari) {
        plugin = stockMarket;
        this.dao = new Dao(hikari, plugin);
        File modFolder = new File("mods");
        TranslationManager manager = new TranslationManager(plugin, modFolder);
        this.map = manager.getMergedTranslations(manager.loadOriginalTranslations());
    }

    // 建立一个基于Guava的缓存，主要用于存储物品和名称之间的关系
    // 该缓存的最大大小为1000，写入后的过期时间为10分钟
    Cache<ItemStack, String> build = CacheBuilder.newBuilder()
            .maximumSize(1000)
            .expireAfterWrite(10, TimeUnit.MINUTES)
            .build();

    /**
     * getTranslate方法用于获取给定物品的中文翻译名称
     * 
     * @param itemStack 要查询的物品
     * @return 返回物品的翻译名称
     */
    public String getTranslate(ItemStack itemStack) {
        // 如果物品的元数据（如自定义名称）不为空，那么返回该自定义名称
        if (itemStack.getItemMeta() != null && !StringUtil.isEmpty(itemStack.getItemMeta().getDisplayName())) {
            return itemStack.getItemMeta().getDisplayName();
        }
        // 如果没有自定义名称，则从map中获取物品的默认翻译名称
        return map.getOrDefault(
                "block." + itemStack.getType().getKey().getNamespace() + "." + itemStack.getType().getKey().getKey(),
                map.getOrDefault("item." + itemStack.getType().getKey().getNamespace() + "."
                        + itemStack.getType().getKey().getKey(), itemStack.getType().getKey().getKey()));
    }

    public Dao getDao() {
        return dao;
    }

    /**
     * getMeta方法根据物品和hashcode来获取物品的meta信息
     * 
     * @param itemStack 目标物品
     * @param hashcode  该物品的hashcode
     * @return 返回物品的元数据
     */
    public ItemMeta getMeta(ItemStack itemStack, String hashcode) {
        ItemMeta itemMeta = itemStack.getItemMeta();
        Class<? extends ItemMeta> aClass = itemMeta.getClass();
        String className = aClass.getName().replaceAll("org\\.bukkit\\.craftbukkit\\..*?\\.inventory\\.", "");
        Map<String, ItemMeta> specials = dao.getSpecials(className);
        return specials.get(hashcode);
    }

    /**
     * getItemStack方法根据物品的类型和序列化过的ItemMeta来获取实际的物品
     * 
     * @param type     物品的类型
     * @param hashcode 物品序列化过的ItemMeta
     * @return 返回对应的物品
     */
    public ItemStack getItemStack(String type, String hashcode) {
        Material material = Material.valueOf(type);
        ItemStack itemStack = new ItemStack(material);
        if (!hashcode.isEmpty()) {
            ItemMeta meta = deserializeItemMeta(hashcode);
            itemStack.setItemMeta(meta);
        }
        return itemStack;
    }

    /**
     * 获取被反序列化的ItemMeta数据
     * 这是从之前的代码改过来的，原先是获取hash
     * 但是hash无法兼容一些mod，所以此处魔改了该方法
     * 
     * @param item 物品
     * @return 物品的子类型，如果没有子类型则返回空字符串
     */
    public String getSubType(ItemStack item) {
        ItemMeta itemMeta = item.getItemMeta();
        return serializeItemMeta(itemMeta);
    }

    // 序列化ItemMeta
    public String serializeItemMeta(ItemMeta itemMeta) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try {
            BukkitObjectOutputStream dataOutput = new BukkitObjectOutputStream(outputStream);
            dataOutput.writeObject(itemMeta);
            dataOutput.close();
            return Base64.getEncoder().encodeToString(outputStream.toByteArray());
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }

    // 反序列化ItemMeta
    public ItemMeta deserializeItemMeta(String itemMetaString) {
        try {
            ByteArrayInputStream inputStream = new ByteArrayInputStream(Base64.getDecoder().decode(itemMetaString));
            BukkitObjectInputStream dataInput = new BukkitObjectInputStream(inputStream);
            ItemMeta itemMeta = (ItemMeta) dataInput.readObject();
            dataInput.close();
            return itemMeta;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public List<Map<String, Object>> getSells(String currency, String type, String subType) {
        return dao.getSells(currency, type, subType);
    }

    public List<Map<String, Object>> getBuys(String currency, String itemLabel, String itemHash) {
        return dao.getBuys(currency, itemLabel, itemHash);
    }

    public ItemStack removeMeta(ItemStack item) {
        item = item.clone();

        ItemMeta itemMeta = item.getItemMeta();

        List<String> lore = new ArrayList<>(itemMeta.hasLore() ? itemMeta.getLore() : new ArrayList<>());
        lore.removeIf(line -> line.startsWith("价格：") || line.startsWith("买卖者："));

        itemMeta.setLore(lore);
        item.setItemMeta(itemMeta);

        return item;
    }

    public int getPrice(ItemStack itemstack) {
        ItemMeta itemMeta = itemstack.getItemMeta();

        int p = 1;

        List<String> lore = itemMeta.getLore();

        if (lore != null) {
            for (String line : lore) {
                if (line.startsWith("价格：")) {
                    String priceWithCurrency = line.substring(3); // 截取价格和货币的部分
                    String[] parts = priceWithCurrency.split(" "); // 按空格分割字符串
                    if (parts.length >= 2) {
                        p = Integer.valueOf(parts[0]); // 获取价格
                    }
                }
            }
        }

        return p;
    }

    public String getName(ItemStack itemstack) {
        ItemMeta itemMeta = itemstack.getItemMeta();
        String name = "";

        List<String> lore = itemMeta.getLore();

        if (lore != null) {
            for (String line : lore) {
                if (line.startsWith("买卖者：")) {
                    String priceWithCurrency = line.substring(4);
                    String[] parts = priceWithCurrency.split(" ");
                    name = parts[0];
                }
            }
        }

        return name;
    }

    public int getAmount(ItemStack itemstack) {
        ItemMeta itemMeta = itemstack.getItemMeta();
        int amount = -1;

        List<String> lore = itemMeta.getLore();

        if (lore != null) {
            for (String line : lore) {
                if (line.startsWith("买卖者：")) {
                    amount = itemstack.getAmount();
                }
            }
        }
        return amount;
    }

    /**
     * 购买操作
     *
     * @param buyer
     * @param currency
     * @param itemStack
     * @param price
     * @param amount
     * @param callback
     */
    public void buy(Player buyer, String currency, ItemStack itemStack, Integer price, Integer amount,
            Callback callback, String location) {
        // ItemStack item = removeMeta(itemStack);
        String type = itemStack.getType().name();
        String subType = getSubType(removeMeta(itemStack));
        int cPrice = getPrice(itemStack);
        int cAmount = getAmount(itemStack);
        String sell_name = getName(itemStack);
        String itemName = getTranslate(itemStack);

        synchronized (this) {
            Integer total = price * amount;
            OperateResult operateResult = BankService.queryCurrencyBalance(buyer.getName(), currency);
            BigDecimal data = (BigDecimal) operateResult.getData();
            if (data.intValue() < total) {
                buyer.sendMessage(MessageConfig.config.getMoreMoney());
                callback.onDone(false);
                return;
            }

            if (cAmount == -1) {
                // put buy order
                OperateResult result = BankService.transferTo(buyer.getName(), SHOP_ACCOUNT, currency,
                        new BigDecimal(amount * price), TxTypeEnum.SHOP_TRADE_OUT,
                        String.format("order %s %s with %s %s  each", amount, type, price, currency));
                if (!result.getSuccess()) {
                    buyer.sendMessage("error:" + result.getReason());
                    callback.onDone(false);
                    return;
                }

                dao.setTrade(type, subType, currency, price, false, buyer, amount, itemName, location);
                buyer.sendMessage(String.format("将以%d %s的价格发出一笔购买%d个%s的订单", price, currency, amount, itemName));
            } else {
                OperateResult result = BankService.transferTo(buyer.getName(), sell_name,
                        currency, new BigDecimal((amount * cPrice)),
                        TxTypeEnum.SHOP_TRADE_OUT, String.format("buy %s %s with %s %s each",
                                cAmount, type, cPrice, currency));
                if (!result.getSuccess()) {
                    buyer.sendMessage("error:" + operateResult.getReason());
                    callback.onDone(false);
                    return;
                }
                cAmount -= amount;
                if (cAmount == 0) {
                    dao.processSells(buyer, sell_name, type, cPrice, currency, amount, itemName, location, true);
                } else {
                    dao.processSells(buyer, sell_name, type, cPrice, currency, amount, itemName, location, false);
                    dao.setTrade(location, sell_name, cPrice, itemName, "trade_number",
                            String.valueOf(cAmount));
                }
                gain(type, subType, sell_name, amount, buyer.getName(), itemName);
                buyer.sendMessage(StringUtil.applyPlaceHolder(MessageConfig.config.getBuyHint(),
                        dao.fetchPlaceHolder(buyer, sell_name, type, cPrice, currency, amount)));
            }
            callback.onDone(true);
            return;
        }

    }

    /**
     * 出售操作
     *
     * @param seller
     * @param currency
     * @param itemStack
     * @param price
     * @param amount
     * @param callback
     */
    public void sell(Player seller, String currency, ItemStack itemStack, int price, int amount, Callback callback,
            String location) {
        ItemStack item = removeMeta(itemStack);
        String type = itemStack.getType().name();
        String subType = getSubType(item);
        int cPrice = getPrice(itemStack);
        int cAmount = getAmount(itemStack);
        String buy_name = getName(itemStack);
        String itemName = getTranslate(itemStack);

        synchronized (this) {
            int itemNumber = ItemUtil.getItemNumber(seller, item);
            if (itemNumber < amount) {
                seller.sendMessage(MessageConfig.config.getMoreItem());
                callback.onDone(false);
                return;
            }

            if (cAmount == -1) {
                ItemUtil.removeBagItem(seller, itemStack, amount);
                dao.setTrade(type, subType, currency, price, true, seller, amount, itemName, location);
                seller.sendMessage(String.format("将以%d %s的价格发出一笔出售%d个%s的订单", price, currency, amount, itemName));
            } else {
                OperateResult result = BankService.transferTo(SHOP_ACCOUNT, seller.getName(), currency,
                        new BigDecimal((amount * cPrice)),
                        TxTypeEnum.SHOP_TRADE_OUT, String.format("sell %s %s with %s %s each",
                                amount, type, cPrice, currency));
                if (!result.getSuccess()) {
                    seller.sendMessage("error:" + result.getReason());
                    callback.onDone(false);
                    return;
                }
                cAmount -= amount;
                if (cAmount == 0) {
                    dao.processSells(seller, buy_name, type, cPrice, currency, amount, itemName, location, true);
                } else {
                    dao.processSells(seller, buy_name, type, cPrice, currency, amount, itemName, location, false);
                    dao.setTrade(location, buy_name, cPrice, itemName, "trade_number",
                            String.valueOf(cAmount));
                }
                pay(seller, item, buy_name, amount);
                seller.sendMessage(StringUtil.applyPlaceHolder(MessageConfig.config.getSellHint(),
                        dao.fetchPlaceHolder(seller, buy_name, type, cPrice, currency, amount)));
            }
            callback.onDone(true);
            return;
        }
    }

    /**
     * 出售入他人库
     *
     * @param seller
     * @param item
     * @param buyer
     * @param tradeNumber
     */
    private void pay(Player seller, ItemStack item, String buyer, Integer tradeNumber) {
        try {
            ItemUtil.removeBagItem(seller, item, tradeNumber);
            dao.setStorage(buyer, tradeNumber, seller, item.getType().name(), getSubType(item), getTranslate(item));

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 购买入库
     *
     * @param type
     * @param subtype
     * @param source
     * @param number
     * @param player
     */
    public void gain(String type, String subtype, String source, Integer number, String player, String item_name) {
        dao.setStorage(type, subtype, player, number, source, item_name);
    }

    public List<Map<String, Object>> getStorage(Player player) {
        return dao.getStorage(player);
    }

    public List<Map<String, Object>> getTrades(Player player) {
        return dao.getTrades(player);
    }

    public void cancelTrade(Player player, Map<String, Object> trade) {
        if ((boolean) trade.get("sell")) {
            gain((String) trade.get("item"), (String) trade.get("hash"), "refund", (Integer) trade.get("trade_number"),
                    player.getName(), (String) trade.get("item_name"));
        } else {
            int total = (int) trade.get("price") * (int) trade.get("trade_number");
            OperateResult result = BankService.transferTo(SHOP_ACCOUNT, player.getName(),
                    (String) trade.get("currency"), new BigDecimal(total), TxTypeEnum.SHOP_TRADE_OUT,
                    String.format("refound %s %s with %s %s", total, (String) trade.get("item_name"),
                            trade.get("price"), trade.get("currency")));
            if (!result.getSuccess()) {
                player.sendMessage("error:" + result.getReason());
                plugin.getLogger().warning("玩家退款不成功！" + JSON.toJSONString(trade));
                return;
            }
        }
    }

    public void openIndex(Player player, Sign sign, String currency, ItemStack itemStack, String location) {
        Menu shop = new Menu(plugin, player, currency, location, itemStack, sign);
        player.openInventory(shop.getInventory());
        // if (StringUtil.isEmpty(sign.getLine(2))) {
        // Menu shop = new Menu(plugin, player, currency, itemStack, sign);
        // player.openInventory(shop.getInventory());
        // } else {
        // Shop shop = new Shop(plugin, player, currency, itemStack, sign,
        // Shop.ShopMode.SIMPLE);
        // player.openInventory(shop.getInventory());
        // }
    }

    // public Map<String, Object> getTradeById(Map<String, Object> trade) {
    // return dao.getTrade((int) trade.get("id"));
    // }

    public void delete(String field, int id) {
        dao.delete(field, id);
    }
}
