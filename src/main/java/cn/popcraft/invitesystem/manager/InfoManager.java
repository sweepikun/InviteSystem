package cn.popcraft.invitesystem.manager;

import cn.popcraft.invitesystem.InviteSystem;
import cn.popcraft.invitesystem.data.Invitation;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.time.format.DateTimeFormatter;
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

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * 获取所有邀请记录（管理员命令）
     * @param player 管理员玩家
     * @param page 页码（从1开始）
     */
    public CompletableFuture<Void> getAllInviteRecords(Player player, int page) {
        return CompletableFuture.runAsync(() -> {
            try {
                int pageSize = 10; // 每页显示10条记录
                int offset = (page - 1) * pageSize;

                // 获取总记录数
                CompletableFuture<Integer> countFuture = plugin.getDatabaseManager().getInvitationDAO()
                        .getTotalCount();

                // 获取当前页的记录
                CompletableFuture<List<Invitation>> invitationsFuture = plugin.getDatabaseManager().getInvitationDAO()
                        .getAllWithPagination(offset, pageSize);

                // 等待两个异步操作完成
                CompletableFuture.allOf(countFuture, invitationsFuture).join();

                int totalCount = countFuture.get();
                List<Invitation> invitations = invitationsFuture.get();

                int totalPages = (int) Math.ceil((double) totalCount / pageSize);

                // 在主线程中发送消息给玩家
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    player.sendMessage("§e======= §6所有邀请记录 (第" + page + "页/共" + totalPages + "页) §e=======");
                    player.sendMessage("§e总记录数: §f" + totalCount);

                    if (invitations.isEmpty()) {
                        player.sendMessage("§c当前页没有邀请记录。");
                    } else {
                        for (int i = 0; i < invitations.size(); i++) {
                            Invitation invitation = invitations.get(i);
                            OfflinePlayer inviter = plugin.getServer().getOfflinePlayer(invitation.getInviterUuid());
                            OfflinePlayer invitee = plugin.getServer().getOfflinePlayer(invitation.getInviteeUuid());

                            String inviterName = inviter.getName() != null ? inviter.getName() : invitation.getInviterUuid().toString();
                            String inviteeName = invitee.getName() != null ? invitee.getName() : invitation.getInviteeUuid().toString();

                            player.sendMessage("§e" + (i + 1) + ". §f邀请人: §a" + inviterName +
                                    " §f被邀请人: §b" + inviteeName +
                                    " §f邀请码: §d" + invitation.getCode() +
                                    " §f时间: §7" + invitation.getCreatedAt().format(FORMATTER));
                        }
                    }

                    player.sendMessage("§e================================");
                    if (page < totalPages) {
                        player.sendMessage("§e使用 §6/invite list " + (page + 1) + " §e查看下一页");
                    } else if (page > 1) {
                        player.sendMessage("§e使用 §6/invite list " + (Math.max(1, page - 1)) + " §e查看上一页");
                    }
                });
            } catch (Exception e) {
                logger.severe("Error getting all invite records: " + e.getMessage());
                e.printStackTrace();
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    player.sendMessage("§c获取邀请记录时发生错误，请联系管理员。");
                });
            }
        });
    }
}