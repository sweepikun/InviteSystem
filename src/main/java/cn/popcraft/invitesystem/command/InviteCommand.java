package cn.popcraft.invitesystem.command;

import cn.popcraft.invitesystem.InviteSystem;
import cn.popcraft.invitesystem.data.InviteCode;
import cn.popcraft.invitesystem.util.CodeGenerator;
import cn.popcraft.invitesystem.util.TimeUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * 邀请系统命令执行器
 */
public class InviteCommand implements CommandExecutor {
    private final InviteSystem plugin;

    public InviteCommand(InviteSystem plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendHelpMessage(sender);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "create":
                handleCreateCommand(sender, args);
                break;
            case "submit":
                handleSubmitCommand(sender, args);
                break;
            case "claim":
                handleClaimCommand(sender, args);
                break;
            case "info":
                handleInfoCommand(sender, args);
                break;
            case "list":
                handleListCommand(sender, args);
                break;
            case "reload":
                handleReloadCommand(sender, args);
                break;
            default:
                sender.sendMessage("§c未知的子命令。使用 /invite 查看帮助。");
                break;
        }

        return true;
    }

    private void sendHelpMessage(CommandSender sender) {
        sender.sendMessage("§e======= §6InviteSystem 帮助 §e=======");
        sender.sendMessage("§a/invite create [code] §7- 创建邀请码");
        sender.sendMessage("§a/invite submit <code> §7- 提交邀请码");
        sender.sendMessage("§a/invite claim §7- 领取奖励");
        sender.sendMessage("§a/invite info §7- 查看邀请信息");
        if (sender.hasPermission("invite.admin")) {
            sender.sendMessage("§a/invite list §7- 查看所有邀请记录");
            sender.sendMessage("§a/invite reload §7- 重新加载配置");
        }
        sender.sendMessage("§e================================");
    }

    private void handleCreateCommand(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§c只有玩家可以创建邀请码。");
            return;
        }

        Player player = (Player) sender;
        UUID playerUUID = player.getUniqueId();
        
        // 检查权限
        if (plugin.getConfig().getBoolean("permissions.restrict-code-creation", true) 
                && !player.hasPermission("invite.create")) {
            player.sendMessage("§c你没有权限创建邀请码。");
            return;
        }
        
        // 检查冷却时间
        if (plugin.getAntiCheatManager().isPlayerInCreateCooldown(playerUUID)) {
            player.sendMessage("§c您正在冷却期内，无法创建新的邀请码。");
            return;
        }

        String code;
        if (args.length > 1) {
            code = args[1];
            // 检查邀请码是否符合规则
            if (!isValidCode(code)) {
                player.sendMessage("§c邀请码格式不正确，只能包含字母和数字。");
                return;
            }
            
            // 检查邀请码是否已存在
            try {
                if (plugin.getDatabaseManager().getInviteCodeDAO().getInviteCode(code).get() != null) {
                    player.sendMessage("§c邀请码已存在，请使用其他邀请码。");
                    return;
                }
                createInviteCode(player, code);
            } catch (Exception e) {
                player.sendMessage("§c检查邀请码时出错，请稍后重试。");
                e.printStackTrace();
            }
        } else {
            // 生成随机邀请码
            int length = plugin.getConfig().getInt("invite.code-length", 8);
            String finalCode;
            int attempts = 0;
            do {
                finalCode = CodeGenerator.generateCode(length);
                attempts++;
                // 防止无限循环
                if (attempts > 10) {
                    player.sendMessage("§c生成唯一邀请码失败，请稍后重试。");
                    return;
                }
            } while (isCodeExistsSync(finalCode));
            
            createInviteCode(player, finalCode);
        }
    }
    
    private boolean isValidCode(String code) {
        return code != null && code.matches("[a-zA-Z0-9]+");
    }
    
    private boolean isCodeExistsSync(String code) {
        try {
            // 异步检查数据库中是否已存在该邀请码
            return plugin.getDatabaseManager().getInviteCodeDAO().getInviteCode(code).get() != null;
        } catch (Exception e) {
            plugin.getLogger().severe("Error checking if code exists: " + e.getMessage());
            return false; // 出错时假设不存在
        }
    }
    
    private void createInviteCode(Player player, String code) {
        UUID creatorUuid = player.getUniqueId();
        LocalDateTime createdAt = LocalDateTime.now();
        
        // 记录玩家创建邀请码的时间
        plugin.getAntiCheatManager().recordPlayerCreateInvite(creatorUuid);
        
        // 计算过期时间
        LocalDateTime expiresAt = null;
        String expireAfter = plugin.getConfig().getString("invite.code-expire-after", "");
        if (!expireAfter.isEmpty()) {
            long seconds = TimeUtil.parseTimeStringToSeconds(expireAfter);
            if (seconds > 0) {
                expiresAt = createdAt.plusSeconds(seconds);
            }
        }
        
        int maxUses = plugin.getConfig().getInt("invite.max-uses-per-code", 1);
        
        InviteCode inviteCode = new InviteCode(code, creatorUuid, createdAt, expiresAt, maxUses, 0);
        
        plugin.getDatabaseManager().getInviteCodeDAO().createInviteCode(inviteCode).thenAcceptAsync(success -> {
            if (success) {
                player.sendMessage("§a邀请码创建成功: §e" + code);
            } else {
                player.sendMessage("§c邀请码创建失败，请稍后重试。");
            }
        });
    }

    private void handleSubmitCommand(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§c只有玩家可以提交邀请码。");
            return;
        }

        if (args.length < 2) {
            sender.sendMessage("§c用法: /invite submit <code>");
            return;
        }

        Player player = (Player) sender;
        String code = args[1];
        
        // 处理邀请码提交
        plugin.getInviteManager().processInviteCodeSubmission(player, code);
    }

    private void handleClaimCommand(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§c只有玩家可以领取奖励。");
            return;
        }

        Player player = (Player) sender;
        
        // 处理奖励领取
        plugin.getClaimManager().processClaimRewards(player);
    }

    private void handleInfoCommand(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§c只有玩家可以查看邀请信息。");
            return;
        }

        Player player = (Player) sender;
        
        // 获取邀请信息
        plugin.getInfoManager().getPlayerInviteInfo(player);
    }

    private void handleListCommand(CommandSender sender, String[] args) {
        if (!sender.hasPermission("invite.admin")) {
            sender.sendMessage("§c你没有权限执行此命令。");
            return;
        }

        if (!(sender instanceof Player)) {
            sender.sendMessage("§c只有玩家可以查看邀请记录。");
            return;
        }

        Player player = (Player) sender;
        int page = 1;
        
        if (args.length > 1) {
            try {
                page = Integer.parseInt(args[1]);
            } catch (NumberFormatException e) {
                player.sendMessage("§c页码必须是数字。");
                return;
            }
        }
        
        // 获取所有邀请记录
        plugin.getInfoManager().getAllInviteRecords(player, page);
    }

    private void handleReloadCommand(CommandSender sender, String[] args) {
        if (!sender.hasPermission("invite.admin")) {
            sender.sendMessage("§c你没有权限执行此命令。");
            return;
        }

        plugin.reloadConfig();
        plugin.getRewardManager().loadRewardsFromConfig();
        sender.sendMessage("§a配置已重新加载。");
    }
}