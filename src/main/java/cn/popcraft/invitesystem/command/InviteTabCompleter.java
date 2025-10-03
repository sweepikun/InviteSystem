package cn.popcraft.invitesystem.command;

import cn.popcraft.invitesystem.InviteSystem;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * 邀请系统命令自动补全器
 */
public class InviteTabCompleter implements TabCompleter {
    private final InviteSystem plugin;

    public InviteTabCompleter(InviteSystem plugin) {
        this.plugin = plugin;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        List<String> commands = new ArrayList<>();

        if (args.length == 1) {
            // 第一个参数，命令列表
            commands.add("create");
            commands.add("submit");
            commands.add("claim");
            commands.add("info");
            
            // 只有管理员或控制台可以使用管理命令
            if (sender.hasPermission("invite.admin") || !(sender instanceof Player)) {
                commands.add("list");
                commands.add("reload");
            }
            
            StringUtil.copyPartialMatches(args[0], commands, completions);
        } else if (args.length == 2) {
            // 第二个参数，根据不同命令提供补全
            switch (args[0].toLowerCase()) {
                case "create":
                    // create命令可以提供一个示例邀请码
                    if (sender instanceof Player) {
                        commands.add("<邀请码>");
                        StringUtil.copyPartialMatches(args[1], commands, completions);
                    }
                    break;
                    
                case "submit":
                    // submit命令可以提供一个示例邀请码
                    commands.add("<邀请码>");
                    StringUtil.copyPartialMatches(args[1], commands, completions);
                    break;
                case "list":
                    // list命令可以补全页码
                    if (sender.hasPermission("invite.admin") || !(sender instanceof Player)) {
                        commands.addAll(Arrays.asList("1", "2", "3", "4", "5"));
                        StringUtil.copyPartialMatches(args[1], commands, completions);
                    }
                    break;
            }
        }

        Collections.sort(completions);
        return completions;
    }
}