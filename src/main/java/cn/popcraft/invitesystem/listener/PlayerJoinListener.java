package cn.popcraft.invitesystem.listener;

import cn.popcraft.invitesystem.InviteSystem;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

/**
 * 玩家加入事件监听器
 */
public class PlayerJoinListener implements Listener {
    private final InviteSystem plugin;
    
    public PlayerJoinListener(InviteSystem plugin) {
        this.plugin = plugin;
    }
    
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        // 记录玩家首次加入时间
        plugin.getDatabaseManager().getPlayerFirstJoinDAO()
            .ensureFirstJoinRecord(event.getPlayer().getUniqueId());
            
        // 记录玩家IP地址
        plugin.getAntiCheatManager().recordPlayerIp(event.getPlayer());
    }
}