package cn.popcraft.invitesystem.manager;

import cn.popcraft.invitesystem.InviteSystem;
import cn.popcraft.invitesystem.data.InviteCode;
import cn.popcraft.invitesystem.data.Invitation;
import cn.popcraft.invitesystem.util.TimeUtil;
import org.bukkit.entity.Player;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;

/**
 * 邀请管理器，处理邀请码提交和邀请关系建立
 */
public class InviteManager {
    private final InviteSystem plugin;
    private final Logger logger;

    public InviteManager(InviteSystem plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
    }

    /**
     * 处理玩家提交邀请码
     * @param player 玩家
     * @param code 邀请码
     * @return 是否成功
     */
    public CompletableFuture<Boolean> processInviteCodeSubmission(Player player, String code) {
        UUID playerUUID = player.getUniqueId();
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                // 1. 检查玩家是否满足提交条件
                if (!isPlayerEligible(player)) {
                    player.sendMessage("§c您不满足提交邀请码的条件。");
                    return false;
                }

                // 2. 检查邀请码是否存在且有效
                InviteCode inviteCode = plugin.getDatabaseManager().getInviteCodeDAO().getInviteCode(code).get();
                if (inviteCode == null) {
                    player.sendMessage("§c邀请码不存在。");
                    return false;
                }

                if (!inviteCode.isUsable()) {
                    player.sendMessage("§c邀请码已过期或已使用。");
                    return false;
                }

                // 3. 检查是否是新用户（如果配置要求）
                if (plugin.getConfig().getBoolean("invite.only-allow-first-time-users", true)) {
                    boolean hasSubmittedCode = hasPlayerSubmittedCode(playerUUID).get();
                    if (hasSubmittedCode) {
                        player.sendMessage("§c您已经提交过邀请码，不能再次提交。");
                        return false;
                    }
                }

                // 4. 检查IP限制
                if (!isIpAllowed(player)) {
                    player.sendMessage("§c您的IP地址在冷却期内，无法提交邀请码。");
                    return false;
                }

                // 5. 检查在线时间
                long requiredPlayTime = TimeUtil.parseTimeStringToSeconds(
                        plugin.getConfig().getString("invite.min-play-time", "10m"));
                long playerPlayTime = getPlayerPlayTime(player) / 20; // ticks to seconds
                
                if (playerPlayTime < requiredPlayTime) {
                    player.sendMessage("§c您需要至少在线" + TimeUtil.formatSecondsToReadable(requiredPlayTime) + "才能提交邀请码。");
                    return false;
                }

                // 6. 检查邀请人和被邀请人是否为同一人
                if (inviteCode.getCreatorUuid().equals(playerUUID)) {
                    player.sendMessage("§c您不能使用自己创建的邀请码。");
                    return false;
                }

                // 7. 创建邀请关系
                Invitation invitation = new Invitation(
                        0, // ID由数据库生成
                        playerUUID,
                        inviteCode.getCreatorUuid(),
                        code,
                        playerPlayTime,
                        false, // 邀请人未领取奖励
                        false, // 被邀请人未领取奖励
                        LocalDateTime.now()
                );

                boolean invitationCreated = plugin.getDatabaseManager().getInvitationDAO()
                        .createInvitation(invitation).get();
                
                if (!invitationCreated) {
                    player.sendMessage("§c邀请码提交失败，请稍后重试。");
                    return false;
                }

                // 8. 更新邀请码使用次数
                boolean codeUpdated = plugin.getDatabaseManager().getInviteCodeDAO()
                        .incrementUsedCount(code).get();
                
                if (!codeUpdated) {
                    logger.warning("Failed to update invite code usage count: " + code);
                }

                // 9. 记录IP邀请
                plugin.getAntiCheatManager().recordIpInvite(player);

                player.sendMessage("§a邀请码提交成功！");
                return true;
            } catch (Exception e) {
                logger.severe("Error processing invite code submission: " + e.getMessage());
                e.printStackTrace();
                player.sendMessage("§c处理邀请码时发生错误，请联系管理员。");
                return false;
            }
        });
    }

    /**
     * 检查玩家是否满足提交邀请码的条件
     * @param player 玩家
     * @return 是否满足条件
     */
    private boolean isPlayerEligible(Player player) {
        // 基本检查，可以根据需要扩展
        return player != null && player.isOnline();
    }

    /**
     * 检查玩家是否已经提交过邀请码
     * @param playerUUID 玩家UUID
     * @return 是否已提交过
     */
    private CompletableFuture<Boolean> hasPlayerSubmittedCode(UUID playerUUID) {
        return plugin.getDatabaseManager().getInvitationDAO()
                .getUnclaimedAsInvitee(playerUUID)
                .thenApply(list -> !list.isEmpty());
    }

    /**
     * 检查玩家IP是否允许提交邀请码
     * @param player 玩家
     * @return 是否允许
     */
    private boolean isIpAllowed(Player player) {
        return plugin.getAntiCheatManager().isIpAllowedForInvite(player);
    }

    /**
     * 获取玩家游戏时间（tick）
     * @param player 玩家
     * @return 游戏时间（tick）
     */
    private long getPlayerPlayTime(Player player) {
        return player.getStatistic(org.bukkit.Statistic.PLAY_ONE_MINUTE);
    }
}