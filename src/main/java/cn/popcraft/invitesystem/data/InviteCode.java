package cn.popcraft.invitesystem.data;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 邀请码数据模型
 */
public class InviteCode {
    private final String code;
    private final UUID creatorUuid;
    private final LocalDateTime createdAt;
    private final LocalDateTime expiresAt; // null = 永不过期
    private final int maxUses;
    private int usedCount;

    public InviteCode(String code, UUID creatorUuid, LocalDateTime createdAt,
                      LocalDateTime expiresAt, int maxUses, int usedCount) {
        this.code = code;
        this.creatorUuid = creatorUuid;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
        this.maxUses = maxUses;
        this.usedCount = usedCount;
    }

    // Getters
    public String getCode() { return code; }
    public UUID getCreatorUuid() { return creatorUuid; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getExpiresAt() { return expiresAt; }
    public int getMaxUses() { return maxUses; }
    public int getUsedCount() { return usedCount; }
    
    public boolean isExpired() {
        return expiresAt != null && LocalDateTime.now().isAfter(expiresAt);
    }
    
    public boolean isUsable() {
        return !isExpired() && usedCount < maxUses;
    }

    // 用于 DAO 更新
    public void incrementUsedCount() { this.usedCount++; }
}