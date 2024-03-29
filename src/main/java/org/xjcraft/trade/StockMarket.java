package org.xjcraft.trade;

import lombok.Getter;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Connection;

import javax.sql.DataSource;

import org.bukkit.plugin.Plugin;
import org.xjcraft.CommonPlugin;
import org.xjcraft.trade.config.Config;
import org.xjcraft.trade.utils.SerializeUtil;
import com.zjyl1994.minecraftplugin.multicurrency.MultiCurrencyPlugin;

/**
 * Created by Ree on 2016/1/7.
 */
public class StockMarket extends CommonPlugin{
    private DataSource hikari;
    Plugin plugin;
    @Getter
    StockMarketManager manager;

    @Override
    public void onEnable() {
        super.onEnable();
        this.plugin = this;
        SerializeUtil.plugin = this;
        hikari = MultiCurrencyPlugin.getInstance().getHikari();

        try (Connection connection = hikari.getConnection()) {
            String[] create = {
                "CREATE TABLE IF NOT EXISTS `stock_history` (" +
                "`id` INT(11) NOT NULL AUTO_INCREMENT," +
                "`seller` VARCHAR(255)," +
                "`buyer` VARCHAR(255)," +
                "`item` VARCHAR(255)," +
                "`item_name` VARCHAR(255)," +
                "`location` VARCHAR(50),"+
                "`hash` TEXT," +
                "`price` INT(11)," +
                "`currency` VARCHAR(255)," +
                "`number` INT(11)," +
                "`create_time` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP," +
                "PRIMARY KEY (`id`)," +
                "INDEX `seller_index` (`seller`)," +
                "INDEX `buyer_index` (`buyer`)" +
                ") ENGINE=INNODB DEFAULT CHARSET=utf8 COMMENT='市场历史表';",
                "CREATE TABLE IF NOT EXISTS `stock_storage` (" +
                "`id` INT(11) NOT NULL AUTO_INCREMENT," +
                "`name` VARCHAR(255)," +
                "`item` VARCHAR(255)," +
                "`item_name` VARCHAR(255)," +
                "`hash` TEXT," +
                "`number` INT(11)," +
                "`source` VARCHAR(255)," +
                "`create_time` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP," +
                "PRIMARY KEY (`id`)," +
                "UNIQUE KEY `item_hash_unique` (`name`,`item`, `hash`(100))," +
                "INDEX `name_index` (`name`)" +
                ") ENGINE=INNODB DEFAULT CHARSET=utf8 COMMENT='市场存储表';",
                "CREATE TABLE IF NOT EXISTS `stock_trade` (" +
                "`id` INT(11) NOT NULL AUTO_INCREMENT," +
                "`player` VARCHAR(255)," +
                "`sell` BOOLEAN," +
                "`item` VARCHAR(255)," +
                "`item_name` VARCHAR(255)," +
                "`hash` TEXT," +
                "`location` VARCHAR(50),"+
                "`currency` VARCHAR(255)," +
                "`price` INT(11)," +
                "`trade_number` INT(11)," +
                "`create_time` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP," +
                "`update_time` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP," +
                "PRIMARY KEY (`id`)," +
                "INDEX `sell_index` (`sell`)," +
                "INDEX `item_index` (`item`)," +
                "INDEX `currency_index` (`currency`)," +
                "INDEX `location_index` (`location`)" +
                ") ENGINE=INNODB DEFAULT CHARSET=utf8 COMMENT='市场交易表';",
                "CREATE TABLE IF NOT EXISTS `stock_metadata` ("+
                "`id` INT(11) NOT NULL AUTO_INCREMENT,"+
                "`uuid` VARCHAR(36) NOT NULL,"+
                "`location` VARCHAR(50),"+
                "`item` VARCHAR(50),"+
                "`hash` TEXT," +
                "`item_name` VARCHAR(255)," +
                "PRIMARY KEY (`id`)," +
                "INDEX `uuid_index` (`uuid`)," +
                "INDEX `location_index` (`location`)" +
                ") ENGINE=INNODB DEFAULT CHARSET=utf8 COMMENT='商店元数据表';",
            };
            for (String s : create) {
                PreparedStatement preparedStatement = connection.prepareStatement(s);
                preparedStatement.execute();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        manager = new StockMarketManager(this, hikari);
        setupListeners(manager);
        // registerCommand(new StockMarketCommands(this, manager));
        getLogger().info("StockMarket has been enabled");
    }

    private void setupListeners(StockMarketManager manager) {
        if (Config.config.getShop_enable()) {
//            getServer().getPluginManager().registerEvents(new StockMarketListener(this,manager), this);
            getServer().getPluginManager().registerEvents(new StockMarketListener(this, manager,hikari), this);
        }

    }

    @Override
    public void onDisable() {
        super.onDisable();
        getLogger().info("StockMarket has been disabled");
    }
}
