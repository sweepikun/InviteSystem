package cn.popcraft.invitesystem.task;

import cn.popcraft.invitesystem.InviteSystem;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.logging.Logger;

/**
 * 清理过期邀请码任务
 */
public class CleanupTask extends BukkitRunnable {
    private final InviteSystem plugin;
    private final Logger logger;

    public CleanupTask(InviteSystem plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
    }

    @Override
    public void run() {
        if (!plugin.getConfig().getBoolean("misc.auto-cleanup-expired-codes", true)) {
            return;
        }

        plugin.getDatabaseManager().getInviteCodeDAO().cleanupExpiredCodes()
                .thenAccept(deletedCount -> {
                    if (deletedCount > 0) {
                        logger.info("Cleaned up " + deletedCount + " expired invite codes.");
                    }
                })
                .exceptionally(throwable -> {
                    logger.severe("Error cleaning up expired invite codes: " + throwable.getMessage());
                    return null;
                });
    }
}