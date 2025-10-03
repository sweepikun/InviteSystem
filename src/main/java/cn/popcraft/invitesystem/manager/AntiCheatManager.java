package cn.popcraft.invitesystem.manager;

import cn.popcraft.invitesystem.InviteSystem;
import org.bukkit.entity.Player;

import java.net.InetSocketAddress;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * 防作弊管理器
 */
public class AntiCheatManager {
    private final InviteSystem plugin;
    private final Logger logger;
    
    // IP地址与邀请记录的映射（用于防小号）
    private final Map<String, IpInviteRecord> ipInviteRecords = new ConcurrentHashMap<>();
    
    // 玩家UUID与IP地址的映射
    private final Map<UUID, String> playerIpMap = new ConcurrentHashMap<>();
    
    // 玩家创建邀请码的冷却时间记录
    private final Map<UUID, LocalDateTime> playerCreateCooldown = new ConcurrentHashMap<>();

    public AntiCheatManager(InviteSystem plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
    }

    /**
     * 记录玩家IP地址
     * @param player 玩家
     */
    public void recordPlayerIp(Player player) {
        InetSocketAddress address = player.getAddress();
        if (address != null) {
            String ip = address.getAddress().getHostAddress();
            playerIpMap.put(player.getUniqueId(), ip);
        }
    }

    /**
     * 检查IP是否允许邀请
     * @param player 玩家
     * @return 是否允许
     */
    public boolean isIpAllowedForInvite(Player player) {
        InetSocketAddress address = player.getAddress();
        if (address == null) {
            return false;
        }
        
        String ip = address.getAddress().getHostAddress();
        int maxInviteesPerIp = plugin.getConfig().getInt("invite.max-invitees-per-ip", 3);
        int ipCooldownHours = plugin.getConfig().getInt("invite.ip-cooldown-hours", 24);
        
        // 如果限制为-1，表示禁用IP限制
        if (maxInviteesPerIp == -1) {
            return true;
        }
        
        IpInviteRecord record = ipInviteRecords.get(ip);
        if (record == null) {
            return true;
        }
        
        // 检查邀请人数是否超过限制
        if (record.getInviteCount() >= maxInviteesPerIp) {
            return false;
        }
        
        // 检查是否在冷却期内
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime lastInviteTime = record.getLastInviteTime();
        if (lastInviteTime != null && 
            now.isBefore(lastInviteTime.plusHours(ipCooldownHours))) {
            return false;
        }
        
        return true;
    }

    /**
     * 记录IP邀请
     * @param player 玩家
     */
    public void recordIpInvite(Player player) {
        InetSocketAddress address = player.getAddress();
        if (address == null) {
            return;
        }
        
        String ip = address.getAddress().getHostAddress();
        IpInviteRecord record = ipInviteRecords.computeIfAbsent(ip, k -> new IpInviteRecord());
        record.incrementInviteCount();
        record.setLastInviteTime(LocalDateTime.now());
    }
    
    /**
     * 检查玩家是否在创建邀请码的冷却期内
     * @param playerUUID 玩家UUID
     * @return 是否在冷却期内
     */
    public boolean isPlayerInCreateCooldown(UUID playerUUID) {
        int cooldownMinutes = plugin.getConfig().getInt("invite.create-cooldown-minutes", 0);
        if (cooldownMinutes <= 0) {
            return false; // 无冷却时间限制
        }
        
        LocalDateTime lastCreate = playerCreateCooldown.get(playerUUID);
        if (lastCreate == null) {
            return false; // 从未创建过邀请码
        }
        
        LocalDateTime now = LocalDateTime.now();
        return now.isBefore(lastCreate.plusMinutes(cooldownMinutes));
    }
    
    /**
     * 记录玩家创建邀请码的时间
     * @param playerUUID 玩家UUID
     */
    public void recordPlayerCreateInvite(UUID playerUUID) {
        playerCreateCooldown.put(playerUUID, LocalDateTime.now());
    }

    /**
     * IP邀请记录类
     */
    private static class IpInviteRecord {
        private int inviteCount = 0;
        private LocalDateTime lastInviteTime = null;

        public synchronized int getInviteCount() {
            return inviteCount;
        }

        public synchronized void incrementInviteCount() {
            this.inviteCount++;
        }

        public synchronized LocalDateTime getLastInviteTime() {
            return lastInviteTime;
        }

        public synchronized void setLastInviteTime(LocalDateTime lastInviteTime) {
            this.lastInviteTime = lastInviteTime;
        }
    }
}