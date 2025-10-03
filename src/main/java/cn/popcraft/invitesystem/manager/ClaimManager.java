package cn.popcraft.invitesystem.manager;

import cn.popcraft.invitesystem.InviteSystem;
import cn.popcraft.invitesystem.data.Invitation;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;

/**
 * 奖励领取管理器
 */
public class ClaimManager {
    private final InviteSystem plugin;
    private final Logger logger;

    public ClaimManager(InviteSystem plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
    }

    /**
     * 处理玩家领取奖励
     * @param player 玩家
     * @return 是否成功
     */
    public CompletableFuture<Boolean> processClaimRewards(Player player) {
        UUID playerUUID = player.getUniqueId();
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                // 获取玩家作为被邀请人未领取的奖励
                List<Invitation> inviteeInvitations = plugin.getDatabaseManager().getInvitationDAO()
                        .getUnclaimedAsInvitee(playerUUID).get();
                
                // 获取玩家作为邀请人未领取的奖励
                List<Invitation> inviterInvitations = plugin.getDatabaseManager().getInvitationDAO()
                        .getUnclaimedAsInviter(playerUUID).get();
                
                boolean success = true;
                
                // 发放被邀请人奖励
                if (!inviteeInvitations.isEmpty()) {
                    for (Invitation invitation : inviteeInvitations) {
                        // 在主线程发放奖励
                        plugin.getServer().getScheduler().runTask(plugin, () -> {
                            plugin.getRewardManager().grantRewards(player, false); // false表示被邀请人
                        });
                        
                        // 更新数据库状态
                        boolean updated = plugin.getDatabaseManager().getInvitationDAO()
                                .markClaimed(invitation.getId(), false).get(); // false表示被邀请人
                        
                        if (!updated) {
                            logger.warning("Failed to update claim status for invitee: " + player.getName());
                            success = false;
                        }
                    }
                    player.sendMessage("§a作为被邀请人，您已成功领取奖励！");
                }
                
                // 发放邀请人奖励
                if (!inviterInvitations.isEmpty()) {
                    for (Invitation invitation : inviterInvitations) {
                        // 在主线程发放奖励
                        plugin.getServer().getScheduler().runTask(plugin, () -> {
                            plugin.getRewardManager().grantRewards(player, true); // true表示邀请人
                        });
                        
                        // 更新数据库状态
                        boolean updated = plugin.getDatabaseManager().getInvitationDAO()
                                .markClaimed(invitation.getId(), true).get(); // true表示邀请人
                        
                        if (!updated) {
                            logger.warning("Failed to update claim status for inviter: " + player.getName());
                            success = false;
                        }
                    }
                    player.sendMessage("§a作为邀请人，您已成功领取奖励！");
                }
                
                if (inviteeInvitations.isEmpty() && inviterInvitations.isEmpty()) {
                    player.sendMessage("§c您没有可领取的奖励。");
                    return false;
                }
                
                return success;
            } catch (Exception e) {
                logger.severe("Error processing claim rewards: " + e.getMessage());
                e.printStackTrace();
                player.sendMessage("§c处理奖励领取时发生错误，请联系管理员。");
                return false;
            }
        });
    }
}