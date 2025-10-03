package cn.popcraft.invitesystem;

import cn.popcraft.invitesystem.command.InviteCommand;
import cn.popcraft.invitesystem.database.DatabaseManager;
import cn.popcraft.invitesystem.listener.PlayerJoinListener;
import cn.popcraft.invitesystem.manager.AntiCheatManager;
import cn.popcraft.invitesystem.manager.ClaimManager;
import cn.popcraft.invitesystem.manager.InfoManager;
import cn.popcraft.invitesystem.manager.InviteManager;
import cn.popcraft.invitesystem.reward.RewardManager;
import cn.popcraft.invitesystem.task.CleanupTask;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * 邀请系统主类
 */
public class InviteSystem extends JavaPlugin {
    private static InviteSystem instance;
    private DatabaseManager databaseManager;
    private RewardManager rewardManager;
    private InviteManager inviteManager;
    private ClaimManager claimManager;
    private InfoManager infoManager;
    private AntiCheatManager antiCheatManager;
    private CleanupTask cleanupTask;

    @Override
    public void onEnable() {
        instance = this;
        
        // 保存默认配置文件
        saveDefaultConfig();
        
        // 初始化数据库
        databaseManager = new DatabaseManager(this);
        databaseManager.init();
        
        // 初始化奖励管理器
        rewardManager = new RewardManager(this);
        rewardManager.loadRewardsFromConfig();
        
        // 初始化其他管理器
        inviteManager = new InviteManager(this);
        claimManager = new ClaimManager(this);
        infoManager = new InfoManager(this);
        antiCheatManager = new AntiCheatManager(this);

        // 注册命令
        registerCommands();

        // 注册事件监听器
        registerListeners();

        // 启动定时清理任务
        startCleanupTask();

        getLogger().info("InviteSystem插件已启用!");
    }

    @Override
    public void onDisable() {
        // 关闭数据库连接
        if (databaseManager != null) {
            databaseManager.close();
        }

        // 取消定时任务
        if (cleanupTask != null) {
            cleanupTask.cancel();
        }

        getLogger().info("InviteSystem插件已禁用!");
    }

    public static InviteSystem getInstance() {
        return instance;
    }
    
    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }
    
    public RewardManager getRewardManager() {
        return rewardManager;
    }
    
    public InviteManager getInviteManager() {
        return inviteManager;
    }
    
    public ClaimManager getClaimManager() {
        return claimManager;
    }
    
    public InfoManager getInfoManager() {
        return infoManager;
    }
    
    public AntiCheatManager getAntiCheatManager() {
        return antiCheatManager;
    }
    
    private void registerCommands() {
        getCommand("invite").setExecutor(new InviteCommand(this));
    }
    
    private void registerListeners() {
        getServer().getPluginManager().registerEvents(new PlayerJoinListener(this), this);
    }
    
    private void startCleanupTask() {
        // 每小时执行一次清理任务
        cleanupTask = new CleanupTask(this);
        cleanupTask.runTaskTimerAsynchronously(this, 0L, 20L * 60 * 60); // 立即开始，每小时执行一次
    }
}