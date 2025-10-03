package cn.popcraft.invitesystem.reward;

import org.bukkit.Material;
import java.util.List;

/**
 * 奖励数据模型
 */
public class Reward {
    private final RewardType type;
    // ITEM
    private final Material material;
    private final int amount;
    private final String name;
    private final List<String> lore;
    // ITEMSADDER
    private final String itemsAdderId;
    // VAULT / PLAYERPOINTS
    private final double amountNumeric; // 用于金币/点券

    // 私有构造器，通过 Builder 模式构建
    private Reward(Builder builder) {
        this.type = builder.type;
        this.material = builder.material;
        this.amount = builder.amount;
        this.name = builder.name;
        this.lore = builder.lore;
        this.itemsAdderId = builder.itemsAdderId;
        this.amountNumeric = builder.amountNumeric;
    }

    // Getters
    public RewardType getType() { return type; }
    public Material getMaterial() { return material; }
    public int getAmount() { return amount; }
    public String getName() { return name; }
    public List<String> getLore() { return lore; }
    public String getItemsAdderId() { return itemsAdderId; }
    public double getAmountNumeric() { return amountNumeric; }

    // ===== Builder Pattern =====
    public static class Builder {
        private RewardType type;
        private Material material;
        private int amount = 1;
        private String name = null;
        private List<String> lore = null;
        private String itemsAdderId = null;
        private double amountNumeric = 0.0;

        public Builder type(RewardType type) {
            this.type = type;
            return this;
        }

        public Builder material(Material material) {
            this.material = material;
            return this;
        }

        public Builder amount(int amount) {
            this.amount = amount;
            return this;
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder lore(List<String> lore) {
            this.lore = lore;
            return this;
        }

        public Builder itemsAdderId(String id) {
            this.itemsAdderId = id;
            return this;
        }

        public Builder amountNumeric(double amount) {
            this.amountNumeric = amount;
            return this;
        }

        public Reward build() {
            if (type == null) throw new IllegalArgumentException("Reward type is required");
            return new Reward(this);
        }
    }
}