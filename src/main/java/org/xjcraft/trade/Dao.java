package org.xjcraft.trade;

import lombok.Getter;
import lombok.Setter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.meta.ItemMeta;
import org.xjcraft.trade.config.SpecialItemConfig;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.sql.Connection;
import java.sql.ResultSetMetaData;

import javax.sql.DataSource;

/**
 * Created by Ree on 2016/1/21.
 */
public class Dao {
    @Getter
    @Setter
    private DataSource hikari;
    private StockMarket plugin;

    public Dao(DataSource db, StockMarket plugin) {
        this.hikari = db;
        this.plugin = plugin;
    }

    public List<Map<String, Object>> getStorage(Player player) {
        String sql = "SELECT * FROM stock_storage WHERE name = ? ORDER BY id ASC LIMIT 52";
        List<Map<String, Object>> resultList = new ArrayList<>();

        try (Connection connection = hikari.getConnection();
                PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setString(1, player.getName());
            ResultSet rs = preparedStatement.executeQuery();
            ResultSetMetaData rsmd = rs.getMetaData();
            int columnCount = rsmd.getColumnCount();
            if (rs.next()) {
                Map<String, Object> row = new HashMap<>();
                for (int i = 1; i <= columnCount; i++) {
                    row.put(rsmd.getColumnName(i), rs.getObject(i));
                }
                resultList.add(row);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return resultList;
    }

    public List<Map<String, Object>> getStorage(Boolean sell, String currency) {
        String sql = "SELECT * FROM stock_storage WHERE sell = ? AND currency = ? ORDER BY id ASC LIMIT 53";
        List<Map<String, Object>> resultList = new ArrayList<>();
        try (Connection connection = hikari.getConnection();
                PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setBoolean(1, sell);
            preparedStatement.setString(2, currency);
            ResultSet rs = preparedStatement.executeQuery();
            ResultSetMetaData rsmd = rs.getMetaData();
            int columnCount = rsmd.getColumnCount();
            if (rs.next()) {
                Map<String, Object> row = new HashMap<>();
                for (int i = 1; i <= columnCount; i++) {
                    row.put(rsmd.getColumnName(i), rs.getObject(i));
                }
                resultList.add(row);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return resultList;
    }

    public void setStorage(String name, Integer number, Player source, String item, String hash, String item_name) {
        String sql = "INSERT INTO stock_storage (name, number, source, item, hash,item_name) VALUES (?, ?, ?, ?, ?,?)";
        Connection connection = null;
        try {
            connection = hikari.getConnection();
            try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
                preparedStatement.setString(1, name);
                preparedStatement.setInt(2, number);
                preparedStatement.setString(3, source.getName());
                preparedStatement.setString(4, item);
                preparedStatement.setString(5, hash);
                preparedStatement.setString(6, item_name);
                preparedStatement.executeUpdate();
                connection.commit();
            } catch (SQLException e) {
                connection.rollback();
                e.printStackTrace();
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        } finally {
            if (connection != null) {
                try {
                    connection.close();
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
            }
        }
    }

    public void setStorage(String type, String subtype, String player, Integer number, String source,
            String item_name) {
        String sql = "INSERT INTO stock_storage (item, hash, name, number, source, item_name) "
                + "VALUES (?, ?, ?, ?, ?, ?) "
                + "ON DUPLICATE KEY UPDATE "
                + "number = number + VALUES(number), "
                + "source = VALUES(source), "
                + "item_name = VALUES(item_name);";
        Connection connection = null;
        try {
            connection = hikari.getConnection();
            try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
                preparedStatement.setString(1, type);
                preparedStatement.setString(2, subtype);
                preparedStatement.setString(3, player);
                preparedStatement.setInt(4, number);
                preparedStatement.setString(5, source);
                preparedStatement.setString(6, item_name);
                preparedStatement.executeUpdate();
                connection.commit();
            } catch (SQLException e) {
                connection.rollback();
                e.printStackTrace();
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        } finally {
            if (connection != null) {
                try {
                    connection.close();
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
            }
        }
    }

    public ResultSet getCustomItem(String name) {
        String sql = "SELECT * FROM stock_custom_item WHERE meta = ?";
        try (Connection connection = hikari.getConnection();
                PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setString(1, name);
            ResultSet rs = preparedStatement.executeQuery();
            return rs;
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    public List<Map<String, Object>> getTrades(Player player) {
        String sql = "SELECT * FROM stock_trade WHERE player = ?";
        List<Map<String, Object>> resultList = new ArrayList<>();

        try (Connection connection = hikari.getConnection();
                PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setString(1, player.getName());
            ResultSet rs = preparedStatement.executeQuery();
            ResultSetMetaData rsmd = rs.getMetaData();
            int columnCount = rsmd.getColumnCount();
            while (rs.next()) {
                Map<String, Object> row = new HashMap<>();
                for (int i = 1; i <= columnCount; i++) {
                    row.put(rsmd.getColumnName(i), rs.getObject(i));
                }
                resultList.add(row);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return resultList;
    }

    public List<Map<String, Object>> getTrade(String location, String currency, String itme, Boolean sell) {
        String sql = "SELECT * FROM stock_trade WHERE sell = ? AND currency = ? AND item = ? AND location = ? ORDER BY price ASC LIMIT 53";
        List<Map<String, Object>> resultList = new ArrayList<>();

        try (Connection connection = hikari.getConnection();
                PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setBoolean(1, sell);
            preparedStatement.setString(2, currency);
            preparedStatement.setString(3, itme);
            preparedStatement.setString(4, location);
            ResultSet rs = preparedStatement.executeQuery();
            ResultSetMetaData rsmd = rs.getMetaData();
            int columnCount = rsmd.getColumnCount();
            while (rs.next()) {
                Map<String, Object> row = new HashMap<>();
                for (int i = 1; i <= columnCount; i++) {
                    row.put(rsmd.getColumnName(i), rs.getObject(i));
                }
                resultList.add(row);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return resultList;
    }

    public Map<String, ItemMeta> getSpecials(String name) {
        return SpecialItemConfig.config.getOrDefault(name, new SpecialItemConfig()).getItemMetas();
    }

    public void save(Integer hashcode, String className, ItemMeta itemMeta) {
        SpecialItemConfig itemConfig = SpecialItemConfig.config.getOrDefault(className, new SpecialItemConfig());
        SpecialItemConfig.config.put(className, itemConfig);
        itemConfig.getItemMetas().put(hashcode + "", itemMeta);
        // o.setId(uuid);
        plugin.saveConfig(SpecialItemConfig.class);
    }

    public List<Map<String, Object>> getSells(String currency, String item, String subType) {
        List<Map<String, Object>> resultList = new ArrayList<>();
        String sql = "SELECT * FROM stock_trade WHERE currency = ? AND item = ? ";
        if (subType != null) {
            sql += "AND hash = ? ";
        }
        sql += "AND sell = TRUE ORDER BY price ASC";
        try (Connection connection = hikari.getConnection();
                PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setString(1, currency);
            preparedStatement.setString(2, item);
            if (subType != null) {
                preparedStatement.setString(3, subType);
            }
            ResultSet rs = preparedStatement.executeQuery();
            while (rs.next()) {
                Map<String, Object> row = new HashMap<>();
                ResultSetMetaData rsmd = rs.getMetaData();
                int columnCount = rsmd.getColumnCount();
                for (int i = 1; i <= columnCount; i++) {
                    row.put(rsmd.getColumnName(i), rs.getObject(i));
                }
                resultList.add(row);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return resultList;
    }

    public List<Map<String, Object>> getBuys(String currency, String item, String subType) {
        List<Map<String, Object>> resultList = new ArrayList<>();
        String sql = "SELECT * FROM stock_trade WHERE currency = ? AND item = ? ";
        if (subType != null) {
            sql += "AND hash = ? ";
        }
        sql += "AND sell = FALSE ORDER BY price DESC";
        try (Connection connection = hikari.getConnection();
                PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setString(1, currency);
            preparedStatement.setString(2, item);
            if (subType != null) {
                preparedStatement.setString(3, subType);
            }
            ResultSet rs = preparedStatement.executeQuery();
            while (rs.next()) {
                Map<String, Object> row = new HashMap<>();
                ResultSetMetaData rsmd = rs.getMetaData();
                int columnCount = rsmd.getColumnCount();
                for (int i = 1; i <= columnCount; i++) {
                    row.put(rsmd.getColumnName(i), rs.getObject(i));
                }
                resultList.add(row);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return resultList;
    }

    public void setMetadata(UUID uuid, String location, String item,String itemName,String subType) {
        String sql = "INSERT INTO stock_metadata (uuid,location,item,item_name,hash) VALUES (?,?,?,?,?)";
        Connection connection = null;
        try {
            connection = hikari.getConnection();
            try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
                preparedStatement.setString(1, uuid.toString());
                preparedStatement.setString(2, location);
                preparedStatement.setString(3, item);
                preparedStatement.setString(4, itemName);
                preparedStatement.setString(5, subType);
                preparedStatement.executeUpdate();
                connection.commit();
            } catch (SQLException e) {
                connection.rollback();
                e.printStackTrace();
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        } finally {
            if (connection != null) {
                try {
                    connection.close();
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
            }
        }
    }

    public void setTrade(String location, String name, int price, String itemName, String field, String revise) {
        String sql = "UPDATE stock_trade SET " + field
                + " = ? WHERE location = ? AND player = ? AND price = ? AND item_name = ?";
        Connection connection = null;
        try {
            connection = hikari.getConnection();
            try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
                preparedStatement.setString(1, revise);
                preparedStatement.setString(2, location);
                preparedStatement.setString(3, name);
                preparedStatement.setInt(4, price);
                preparedStatement.setString(5, itemName);
                preparedStatement.executeUpdate();
                connection.commit();
            } catch (SQLException e) {
                connection.rollback();
                e.printStackTrace();
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        } finally {
            if (connection != null) {
                try {
                    connection.close();
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
            }
        }
    }

    public List<Map<String, Object>> getMetadata(String location, String item) {
        String sql = "SELECT * FROM stock_metadata WHERE location = ? AND item_name = ?";
        List<Map<String, Object>> resultList = new ArrayList<>();

        try (Connection connection = hikari.getConnection();
                PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setString(1, location);
            preparedStatement.setString(2, item);
            ResultSet rs = preparedStatement.executeQuery();
            while (rs.next()) {
                Map<String, Object> row = new HashMap<>();
                ResultSetMetaData rsmd = rs.getMetaData();
                int columnCount = rsmd.getColumnCount();
                for (int i = 1; i <= columnCount; i++) {
                    row.put(rsmd.getColumnName(i), rs.getObject(i));
                }
                resultList.add(row);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return resultList;
    }

    public void setTrade(String type, String subType, String currency, Integer price, boolean sell, Player buyer,
            Integer amount, String type_name, String location) {
        String sql = "INSERT INTO stock_trade (item, hash, currency, price, sell, player, trade_number,item_name,location) VALUES (?, ?, ?, ?, ?, ?, ?,?,?)";
        Connection connection = null;
        try {
            connection = hikari.getConnection();
            try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
                preparedStatement.setString(1, type);
                preparedStatement.setString(2, subType);
                preparedStatement.setString(3, currency);
                preparedStatement.setInt(4, price);
                preparedStatement.setBoolean(5, sell);
                preparedStatement.setString(6, buyer.getName());
                preparedStatement.setInt(7, amount);
                preparedStatement.setString(8, type_name);
                preparedStatement.setString(9, location);
                preparedStatement.executeUpdate();
                connection.commit();
            } catch (SQLException e) {
                connection.rollback();
                e.printStackTrace();
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        } finally {
            if (connection != null) {
                try {
                    connection.close();
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
            }
        }
    }

    public void processSells(Player buyer, String name, String itme, int price, String currency, int tradeNumber,
            String itemName, String location, boolean delete) {
        String insertHistorySql = "INSERT INTO stock_history (seller, buyer, item, hash, price, currency, number,item_name,location) VALUES (?, ?, ?, ?, ?, ?, ?,?,?)";
        String deleteSellSql = "DELETE FROM stock_trade WHERE location = ? AND player = ? AND price = ? AND item_name = ?";
        Connection connection = null;

        try {
            connection = hikari.getConnection();
            try (PreparedStatement insertHistoryStmt = connection.prepareStatement(insertHistorySql)) {
                insertHistoryStmt.setString(1, buyer.getName());
                insertHistoryStmt.setString(2, name);
                insertHistoryStmt.setString(3, itme);
                insertHistoryStmt.setString(4, "");
                insertHistoryStmt.setInt(5, price);
                insertHistoryStmt.setString(6, currency);
                insertHistoryStmt.setInt(7, tradeNumber);
                insertHistoryStmt.setString(8, itemName);
                insertHistoryStmt.setString(9, location);
                insertHistoryStmt.executeUpdate();
            }
            // 删除原有的 sell 记录
            if (delete) {
                try (PreparedStatement deleteSellStmt = connection.prepareStatement(deleteSellSql)) {
                    deleteSellStmt.setString(1, location);
                    deleteSellStmt.setString(2, name);
                    deleteSellStmt.setInt(3, price);
                    deleteSellStmt.setString(4, itemName);
                    deleteSellStmt.executeUpdate();
                }
            }
            connection.commit(); // 提交事务
        } catch (SQLException e) {
            if (connection != null) {
                try {
                    connection.rollback(); // 在出错时回滚事务
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
            }
            e.printStackTrace();
        } finally {
            if (connection != null) {
                try {
                    connection.close();
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
            }
        }
    }

    public HashMap<String, String> fetchPlaceHolder(Player buyer, String name, String itemName, int price,
            String currency, int tradeNumber) {
        HashMap<String, String> map = new HashMap<>();
        map.put("buyer", buyer.getName());
        map.put("seller", name);
        map.put("type", itemName);
        map.put("currency", currency);
        map.put("price", String.valueOf(price));
        map.put("number", String.valueOf(tradeNumber));
        return map;
    }

    public void delete(String field, int id) {
        String deleteSellSql = "DELETE FROM " + field + " WHERE id = ?";
        Connection connection = null;
        try {
            connection = hikari.getConnection();
            try (PreparedStatement preparedStatement = connection.prepareStatement(deleteSellSql)) {
                preparedStatement.setInt(1, id);
                preparedStatement.executeUpdate();
                connection.commit();
            } catch (SQLException e) {
                connection.rollback();
                e.printStackTrace();
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        } finally {
            if (connection != null) {
                try {
                    connection.close();
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
            }
        }
    }
}
