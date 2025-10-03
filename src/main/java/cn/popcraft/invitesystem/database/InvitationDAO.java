package cn.popcraft.invitesystem.database;

import cn.popcraft.invitesystem.InviteSystem;
import cn.popcraft.invitesystem.data.Invitation;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * 邀请关系数据访问对象
 */
public class InvitationDAO {

    private final DatabaseManager dbManager;
    private final String tablePrefix;

    public InvitationDAO(DatabaseManager dbManager) {
        this.dbManager = dbManager;
        this.tablePrefix = dbManager.getTablePrefix();
    }

    // 记录一次成功邀请
    public CompletableFuture<Boolean> createInvitation(Invitation inv) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "INSERT INTO " + tablePrefix + "invitations " +
                    "(invitee_uuid, inviter_uuid, code, play_time_seconds, claimed_inviter, claimed_invitee) " +
                    "VALUES (?, ?, ?, ?, ?, ?)";
            try (Connection conn = dbManager.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                stmt.setString(1, inv.getInviteeUuid().toString());
                stmt.setString(2, inv.getInviterUuid().toString());
                stmt.setString(3, inv.getCode());
                stmt.setLong(4, inv.getPlayTimeSeconds());
                stmt.setBoolean(5, inv.isClaimedInviter());
                stmt.setBoolean(6, inv.isClaimedInvitee());
                return stmt.executeUpdate() > 0;
            } catch (SQLException e) {
                dbManager.handleSqlException(e);
                return false;
            }
        }, dbManager.getAsyncExecutor());
    }

    // 获取某玩家作为被邀请人、未领取的记录
    public CompletableFuture<List<Invitation>> getUnclaimedAsInvitee(UUID playerUuid) {
        return queryInvitations("invitee_uuid = ? AND claimed_invitee = FALSE", playerUuid.toString());
    }

    // 获取某玩家作为邀请人、未领取的记录
    public CompletableFuture<List<Invitation>> getUnclaimedAsInviter(UUID playerUuid) {
        return queryInvitations("inviter_uuid = ? AND claimed_inviter = FALSE", playerUuid.toString());
    }

    private CompletableFuture<List<Invitation>> queryInvitations(String whereClause, String uuidStr) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "SELECT * FROM " + tablePrefix + "invitations WHERE " + whereClause;
            List<Invitation> list = new ArrayList<>();
            try (Connection conn = dbManager.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, uuidStr);
                ResultSet rs = stmt.executeQuery();
                while (rs.next()) {
                    list.add(mapResultSetToInvitation(rs));
                }
            } catch (SQLException e) {
                dbManager.handleSqlException(e);
            }
            return list;
        }, dbManager.getAsyncExecutor());
    }

    // 更新领取状态（原子）
    public CompletableFuture<Boolean> markClaimed(int invitationId, boolean isInviter) {
        return CompletableFuture.supplyAsync(() -> {
            String column = isInviter ? "claimed_inviter" : "claimed_invitee";
            String sql = "UPDATE " + tablePrefix + "invitations SET " + column + " = TRUE WHERE id = ?";
            try (Connection conn = dbManager.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, invitationId);
                return stmt.executeUpdate() > 0;
            } catch (SQLException e) {
                dbManager.handleSqlException(e);
                return false;
            }
        }, dbManager.getAsyncExecutor());
    }

    private Invitation mapResultSetToInvitation(ResultSet rs) throws SQLException {
        int id = rs.getInt("id");
        UUID invitee = UUID.fromString(rs.getString("invitee_uuid"));
        UUID inviter = UUID.fromString(rs.getString("inviter_uuid"));
        String code = rs.getString("code");
        long playTime = rs.getLong("play_time_seconds");
        boolean claimedInviter = rs.getBoolean("claimed_inviter");
        boolean claimedInvitee = rs.getBoolean("claimed_invitee");
        Timestamp createdAtTs = rs.getTimestamp("created_at");
        LocalDateTime createdAt = createdAtTs != null ? createdAtTs.toLocalDateTime() : LocalDateTime.now();

        return new Invitation(id, invitee, inviter, code, playTime, claimedInviter, claimedInvitee, createdAt);
    }
}