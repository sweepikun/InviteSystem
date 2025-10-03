package cn.popcraft.invitesystem.database;

import cn.popcraft.invitesystem.InviteSystem;
import cn.popcraft.invitesystem.data.InviteCode;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * 邀请码数据访问对象
 */
public class InviteCodeDAO {

    private final DatabaseManager dbManager;
    private final String tablePrefix;

    public InviteCodeDAO(DatabaseManager dbManager) {
        this.dbManager = dbManager;
        this.tablePrefix = dbManager.getTablePrefix();
    }

    // 创建邀请码
    public CompletableFuture<Boolean> createInviteCode(InviteCode code) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "INSERT INTO " + tablePrefix + "invite_codes (code, creator_uuid, expires_at, max_uses, used_count) VALUES (?, ?, ?, ?, ?)";
            try (Connection conn = dbManager.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, code.getCode());
                stmt.setString(2, code.getCreatorUuid().toString());
                stmt.setTimestamp(3, code.getExpiresAt() == null ? null : Timestamp.valueOf(code.getExpiresAt()));
                stmt.setInt(4, code.getMaxUses());
                stmt.setInt(5, code.getUsedCount());
                return stmt.executeUpdate() > 0;
            } catch (SQLException e) {
                dbManager.handleSqlException(e);
                return false;
            }
        }, dbManager.getAsyncExecutor());
    }

    // 通过 code 查询（用于提交时验证）
    public CompletableFuture<InviteCode> getInviteCode(String code) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "SELECT * FROM " + tablePrefix + "invite_codes WHERE code = ?";
            try (Connection conn = dbManager.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, code);
                ResultSet rs = stmt.executeQuery();
                if (rs.next()) {
                    return mapResultSetToInviteCode(rs);
                }
            } catch (SQLException e) {
                dbManager.handleSqlException(e);
            }
            return null;
        }, dbManager.getAsyncExecutor());
    }

    // 增加使用次数（原子操作）
    public CompletableFuture<Boolean> incrementUsedCount(String code) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "UPDATE " + tablePrefix + "invite_codes SET used_count = used_count + 1 WHERE code = ?";
            try (Connection conn = dbManager.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, code);
                return stmt.executeUpdate() > 0;
            } catch (SQLException e) {
                dbManager.handleSqlException(e);
                return false;
            }
        }, dbManager.getAsyncExecutor());
    }

    // 清理过期码（可选）
    public CompletableFuture<Integer> cleanupExpiredCodes() {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "DELETE FROM " + tablePrefix + "invite_codes WHERE expires_at IS NOT NULL AND expires_at < ?";
            try (Connection conn = dbManager.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setTimestamp(1, Timestamp.valueOf(LocalDateTime.now()));
                return stmt.executeUpdate();
            } catch (SQLException e) {
                dbManager.handleSqlException(e);
                return 0;
            }
        }, dbManager.getAsyncExecutor());
    }

    private InviteCode mapResultSetToInviteCode(ResultSet rs) throws SQLException {
        String code = rs.getString("code");
        UUID creator = UUID.fromString(rs.getString("creator_uuid"));
        Timestamp createdAtTs = rs.getTimestamp("created_at");
        Timestamp expiresAtTs = rs.getTimestamp("expires_at");
        int maxUses = rs.getInt("max_uses");
        int usedCount = rs.getInt("used_count");

        LocalDateTime createdAt = createdAtTs != null ? createdAtTs.toLocalDateTime() : LocalDateTime.now();
        LocalDateTime expiresAt = expiresAtTs != null ? expiresAtTs.toLocalDateTime() : null;

        return new InviteCode(code, creator, createdAt, expiresAt, maxUses, usedCount);
    }
}