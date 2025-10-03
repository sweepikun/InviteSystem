package cn.popcraft.invitesystem.database;

import cn.popcraft.invitesystem.InviteSystem;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * 玩家首次加入数据访问对象
 */
public class PlayerFirstJoinDAO {
    private final DatabaseManager dbManager;
    private final String tablePrefix;

    public PlayerFirstJoinDAO(DatabaseManager dbManager) {
        this.dbManager = dbManager;
        this.tablePrefix = dbManager.getTablePrefix();
    }

    public CompletableFuture<Boolean> ensureFirstJoinRecord(UUID uuid) {
        return CompletableFuture.supplyAsync(() -> {
            // 首先检查记录是否已存在
            String checkSql = "SELECT uuid FROM " + tablePrefix + "player_first_join WHERE uuid = ?";
            try (Connection conn = dbManager.getConnection();
                 PreparedStatement checkStmt = conn.prepareStatement(checkSql)) {
                checkStmt.setString(1, uuid.toString());
                ResultSet rs = checkStmt.executeQuery();
                if (rs.next()) {
                    // 记录已存在
                    return true;
                }
            } catch (SQLException e) {
                dbManager.handleSqlException(e);
                return false;
            }
            
            // 记录不存在，插入新记录
            String insertSql = "INSERT INTO " + tablePrefix + "player_first_join (uuid) VALUES (?)";
            try (Connection conn = dbManager.getConnection();
                 PreparedStatement insertStmt = conn.prepareStatement(insertSql)) {
                insertStmt.setString(1, uuid.toString());
                insertStmt.executeUpdate();
                return true;
            } catch (SQLException e) {
                // 再次检查是否是因为并发导致的重复记录
                if (e.getErrorCode() == 19 || e.getMessage().contains("UNIQUE")) {
                    // 已存在，正常
                    return true;
                }
                dbManager.handleSqlException(e);
                return false;
            }
        }, dbManager.getAsyncExecutor());
    }
    
    private boolean insertIfNotExists(UUID uuid) {
        try (Connection conn = dbManager.getConnection()) {
            // 先检查是否存在
            String selectSql = "SELECT uuid FROM " + tablePrefix + "player_first_join WHERE uuid = ?";
            try (PreparedStatement selectStmt = conn.prepareStatement(selectSql)) {
                selectStmt.setString(1, uuid.toString());
                ResultSet rs = selectStmt.executeQuery();
                if (rs.next()) {
                    // 已存在
                    return true;
                }
            }
            
            // 不存在则插入
            String insertSql = "INSERT INTO " + tablePrefix + "player_first_join (uuid) VALUES (?)";
            try (PreparedStatement insertStmt = conn.prepareStatement(insertSql)) {
                insertStmt.setString(1, uuid.toString());
                insertStmt.executeUpdate();
                return true;
            }
        } catch (SQLException e) {
            dbManager.handleSqlException(e);
            return false;
        }
    }

    public CompletableFuture<LocalDateTime> getFirstJoinTime(UUID uuid) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "SELECT first_join FROM " + tablePrefix + "player_first_join WHERE uuid = ?";
            try (Connection conn = dbManager.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, uuid.toString());
                ResultSet rs = stmt.executeQuery();
                if (rs.next()) {
                    Timestamp ts = rs.getTimestamp("first_join");
                    return ts != null ? ts.toLocalDateTime() : LocalDateTime.now();
                }
            } catch (SQLException e) {
                dbManager.handleSqlException(e);
            }
            return null;
        }, dbManager.getAsyncExecutor());
    }
}