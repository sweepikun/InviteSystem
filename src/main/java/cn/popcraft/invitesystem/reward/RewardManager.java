package cn.popcraft.invitesystem.reward;

import cn.popcraft.invitesystem.InviteSystem;
import net.milkbowl.vault.economy.Economy;
import org.black_ixx.playerpoints.PlayerPoints;
import org.black_ixx.playerpoints.PlayerPointsAPI;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * 奖励管理器
 */
public class RewardManager {

    private final Plugin plugin;
    private final Logger logger;
    private Economy economy;
    private PlayerPointsAPI playerPointsAPI;
    private boolean itemsAdderEnabled;
    private boolean vaultEnabled;
    private boolean playerPointsEnabled;

    // 奖励配置（从 config.yml 加载）
    private List<Reward> inviterRewards;
    private List<Reward> inviteeRewards;

    public RewardManager(Plugin plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();

        // 检测依赖
        this.vaultEnabled = setupVault();
        this.playerPointsEnabled = setupPlayerPoints();
        this.itemsAdderEnabled = Bukkit.getPluginManager().isPluginEnabled("ItemsAdder");

        if (!vaultEnabled) {
            logger.warning("Vault not found! VAULT rewards will be skipped.");
        }
        if (!playerPointsEnabled) {
            logger.warning("PlayerPoints not found! PLAYERPOINTS rewards will be skipped.");
        }
        if (!itemsAdderEnabled) {
            logger.warning("ItemsAdder not found! ITEMSADDER rewards will be skipped.");
        }
    }

    // === 依赖初始化 ===
    private boolean setupVault() {
        if (!Bukkit.getPluginManager().isPluginEnabled("Vault")) return false;
        try {
            // 通过 Vault 获取 Economy
            var rsp = Bukkit.getServer().getServicesManager().getRegistration(Economy.class);
            if (rsp != null) {
                this.economy = rsp.getProvider();
                return true;
            }
        } catch (Exception ignored) {}
        return false;
    }

    private boolean setupPlayerPoints() {
        if (!Bukkit.getPluginManager().isPluginEnabled("PlayerPoints")) return false;
        PlayerPoints pp = (PlayerPoints) Bukkit.getPluginManager().getPlugin("PlayerPoints");
        if (pp != null) {
            this.playerPointsAPI = pp.getAPI();
            return true;
        }
        return false;
    }

    // === 加载配置 ===
    public void loadRewardsFromConfig() {
        this.inviterRewards = loadRewardList("rewards.inviter");
        this.inviteeRewards = loadRewardList("rewards.invitee");
    }

    @SuppressWarnings("unchecked")
    private List<Reward> loadRewardList(String path) {
        List<Reward> rewards = new ArrayList<>();
        List<Map<String, Object>> list = (List<Map<String, Object>>) plugin.getConfig().getList(path);
        if (list == null) return rewards;

        for (Map<String, Object> map : list) {
            try {
                String typeStr = (String) map.get("type");
                RewardType type = RewardType.valueOf(typeStr.toUpperCase());

                Reward.Builder builder = new Reward.Builder().type(type);

                switch (type) {
                    case ITEM:
                        String matStr = (String) map.get("material");
                        org.bukkit.Material mat = org.bukkit.Material.matchMaterial(matStr.toUpperCase());
                        if (mat == null || !mat.isItem()) {
                            logger.warning("Invalid material in reward: " + matStr);
                            continue;
                        }
                        builder.material(mat)
                               .amount((int) map.getOrDefault("amount", 1))
                               .name((String) map.get("name"))
                               .lore((List<String>) map.get("lore"));
                        break;

                    case ITEMSADDER:
                        builder.itemsAdderId((String) map.get("id"))
                               .amount((int) map.getOrDefault("amount", 1));
                        break;

                    case VAULT:
                    case PLAYERPOINTS:
                        builder.amountNumeric(((Number) map.get("amount")).doubleValue());
                        break;
                }

                rewards.add(builder.build());
            } catch (Exception e) {
                logger.severe("Failed to load reward: " + map + " - " + e.getMessage());
            }
        }
        return rewards;
    }

    // === 发放奖励 ===
    public void grantRewards(Player player, boolean isInviter) {
        List<Reward> rewards = isInviter ? inviterRewards : inviteeRewards;
        for (Reward reward : rewards) {
            grantSingleReward(player, reward);
        }
    }

    private void grantSingleReward(Player player, Reward reward) {
        switch (reward.getType()) {
            case ITEM:
                giveVanillaItem(player, reward);
                break;
            case ITEMSADDER:
                giveItemsAdderItem(player, reward);
                break;
            case VAULT:
                giveVaultMoney(player, reward);
                break;
            case PLAYERPOINTS:
                givePlayerPoints(player, reward);
                break;
        }
    }

    // --- 具体发放方法 ---
    private void giveVanillaItem(Player player, Reward r) {
        ItemStack item = new ItemStack(r.getMaterial(), Math.max(1, r.getAmount()));
        if (r.getName() != null || r.getLore() != null) {
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                if (r.getName() != null) {
                    meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', r.getName()));
                }
                if (r.getLore() != null) {
                    List<String> coloredLore = r.getLore().stream()
                            .map(line -> ChatColor.translateAlternateColorCodes('&', line))
                            .collect(Collectors.toList()); // 修复toList()错误
                    meta.setLore(coloredLore);
                }
                item.setItemMeta(meta);
            }
        }
        player.getInventory().addItem(item);
    }

    private void giveItemsAdderItem(Player player, Reward r) {
        if (!itemsAdderEnabled) return;
        try {
            // 使用更通用的方式检查ItemsAdder API
            if (dev.lone.itemsadder.api.ItemsAdder.isCustomItem(r.getItemsAdderId())) {
                ItemStack item = dev.lone.itemsadder.api.CustomStack.getInstance(r.getItemsAdderId()).getItemStack().clone();
                if (item != null) {
                    // 支持堆叠
                    item.setAmount(r.getAmount());
                    player.getInventory().addItem(item);
                } else {
                    logger.warning("ItemsAdder item could not be cloned: " + r.getItemsAdderId());
                }
            } else {
                logger.warning("ItemsAdder item not found: " + r.getItemsAdderId());
            }
        } catch (Exception e) {
            logger.severe("Error giving ItemsAdder item: " + r.getItemsAdderId() + " - " + e.getMessage());
        }
    }

    private void giveVaultMoney(Player player, Reward r) {
        if (!vaultEnabled || economy == null) return;
        economy.depositPlayer(player, r.getAmountNumeric());
        player.sendMessage(org.bukkit.ChatColor.translateAlternateColorCodes('&',
                "&a获得经济奖励: &e" + r.getAmountNumeric() + " " + economy.currencyNamePlural()));
    }

    private void givePlayerPoints(Player player, Reward r) {
        if (!playerPointsEnabled || playerPointsAPI == null) return;
        playerPointsAPI.give(player.getUniqueId(), (int) r.getAmountNumeric());
        player.sendMessage(org.bukkit.ChatColor.translateAlternateColorCodes('&',
                "&a获得点券奖励: &e" + (int) r.getAmountNumeric() + " 点"));
    }
    
    // Getters
    public boolean isVaultEnabled() {
        return vaultEnabled;
    }
    
    public boolean isPlayerPointsEnabled() {
        return playerPointsEnabled;
    }
    
    public boolean isItemsAdderEnabled() {
        return itemsAdderEnabled;
    }
}