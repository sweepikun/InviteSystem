package cn.popcraft.invitesystem.manager;

import cn.popcraft.invitesystem.InviteSystem;
import cn.popcraft.invitesystem.data.Invitation;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;

/**
 * 信息查询管理器
 */
public class InfoManager {
    private final InviteSystem plugin;
    private final Logger logger;

    public InfoManager(InviteSystem plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
    }

    /**
     * 获取玩家的邀请信息
     * @param player 玩家
     */
    public CompletableFuture<Void> getPlayerInviteInfo(Player player) {
        UUID playerUUID = player.getUniqueId();
        
        return CompletableFuture.runAsync(() -> {
            try {
                // 获取玩家作为被邀请人的记录
                List<Invitation> inviteeInvitations = plugin.getDatabaseManager().getInvitationDAO()
                        .getUnclaimedAsInvitee(playerUUID).get();
                
                // 获取玩家作为邀请人的记录
                List<Invitation> inviterInvitations = plugin.getDatabaseManager().getInvitationDAO()
                        .getUnclaimedAsInviter(playerUUID).get();
                
                // 在主线程发送消息
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    player.sendMessage("§e======= §6邀请信息 §e=======");
                    
                    // 显示作为被邀请人的信息
                    player.sendMessage("§a作为被邀请人:");
                    if (inviteeInvitations.isEmpty()) {
                        player.sendMessage("  §7无记录");
                    } else {
                        for (Invitation invitation : inviteeInvitations) {
                            player.sendMessage("  §7邀请码: §f" + invitation.getCode());
                            player.sendMessage("  §7邀请人: §f" + invitation.getInviterUuid());
                            player.sendMessage("  §7提交时间: §f" + invitation.getCreatedAt().toString());
                            player.sendMessage("  §7奖励状态: §f" + (invitation.isClaimedInvitee() ? "§a已领取" : "§c未领取"));
                        }
                    }
                    
                    // 显示作为邀请人的信息
                    player.sendMessage("§a作为邀请人:");
                    if (inviterInvitations.isEmpty()) {
                        player.sendMessage("  §7无记录");
                    } else {
                        for (Invitation invitation : inviterInvitations) {
                            player.sendMessage("  §7邀请码: §f" + invitation.getCode());
                            player.sendMessage("  §7被邀请人: §f" + invitation.getInviteeUuid());
                            player.sendMessage("  §7邀请时间: §f" + invitation.getCreatedAt().toString());
                            player.sendMessage("  §7奖励状态: §f" + (invitation.isClaimedInviter() ? "§a已领取" : "§c未领取"));
                        }
                    }
                    
                    player.sendMessage("§e========================");
                });
            } catch (Exception e) {
                logger.severe("Error getting player invite info: " + e.getMessage());
                e.printStackTrace();
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    player.sendMessage("§c获取邀请信息时发生错误，请联系管理员。");
                });
            }
        });
    }

    /**
     * 获取所有邀请记录（管理员命令）
     * @param player 管理员玩家
     * @param page 页码
     */
    public CompletableFuture<Void> getAllInviteRecords(Player player, int page) {
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            player.sendMessage("§e======= §6所有邀请记录 (第" + page + "页) §e=======");
            player.sendMessage("§c注意: 此功能需要进一步完善分页查询逻辑。");
            player.sendMessage("§e================================");
        });
        return CompletableFuture.completedFuture(null);
    }
}