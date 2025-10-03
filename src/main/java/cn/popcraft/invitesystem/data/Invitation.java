package cn.popcraft.invitesystem.data;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 邀请关系数据模型
 */
public class Invitation {
    private final int id;
    private final UUID inviteeUuid;
    private final UUID inviterUuid;
    private final String code;
    private final long playTimeSeconds;
    private boolean claimedInviter;
    private boolean claimedInvitee;
    private final LocalDateTime createdAt;

    public Invitation(int id, UUID inviteeUuid, UUID inviterUuid, String code,
                     long playTimeSeconds, boolean claimedInviter, boolean claimedInvitee,
                     LocalDateTime createdAt) {
        this.id = id;
        this.inviteeUuid = inviteeUuid;
        this.inviterUuid = inviterUuid;
        this.code = code;
        this.playTimeSeconds = playTimeSeconds;
        this.claimedInviter = claimedInviter;
        this.claimedInvitee = claimedInvitee;
        this.createdAt = createdAt;
    }

    // Getters & Setters for claimed flags
    public int getId() { return id; }
    public UUID getInviteeUuid() { return inviteeUuid; }
    public UUID getInviterUuid() { return inviterUuid; }
    public String getCode() { return code; }
    public long getPlayTimeSeconds() { return playTimeSeconds; }
    public boolean isClaimedInviter() { return claimedInviter; }
    public boolean isClaimedInvitee() { return claimedInvitee; }
    public void setClaimedInviter(boolean claimed) { this.claimedInviter = claimed; }
    public void setClaimedInvitee(boolean claimed) { this.claimedInvitee = claimed; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}