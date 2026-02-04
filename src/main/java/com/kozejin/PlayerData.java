package com.kozejin;

import java.util.UUID;

public class PlayerData {
    private UUID uuid;
    private String username;
    private long firstLoginTime;
    private long lastLoginTime;
    private long totalPlayTime;
    private long currentSessionStart;
    private String discordId;
    private int totalDeaths;
    private int blocksPlaced;
    private int blocksBroken;
    private int playerKills;

    public PlayerData(UUID uuid, String username) {
        this.uuid = uuid;
        this.username = username;
        this.firstLoginTime = System.currentTimeMillis();
        this.lastLoginTime = System.currentTimeMillis();
        this.totalPlayTime = 0;
        this.currentSessionStart = -1;
        this.discordId = null;
        this.totalDeaths = 0;
        this.blocksPlaced = 0;
        this.blocksBroken = 0;
        this.playerKills = 0;
    }

    public UUID getUuid() {
        return uuid;
    }

    public void setUuid(UUID uuid) {
        this.uuid = uuid;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public long getFirstLoginTime() {
        return firstLoginTime;
    }

    public void setFirstLoginTime(long firstLoginTime) {
        this.firstLoginTime = firstLoginTime;
    }

    public long getLastLoginTime() {
        return lastLoginTime;
    }

    public void setLastLoginTime(long lastLoginTime) {
        this.lastLoginTime = lastLoginTime;
    }

    public long getTotalPlayTime() {
        return totalPlayTime;
    }

    public void setTotalPlayTime(long totalPlayTime) {
        this.totalPlayTime = totalPlayTime;
    }

    public void addPlayTime(long playTime) {
        this.totalPlayTime += playTime;
    }

    public long getCurrentSessionStart() {
        return currentSessionStart;
    }

    public void setCurrentSessionStart(long currentSessionStart) {
        this.currentSessionStart = currentSessionStart;
    }

    public String getDiscordId() {
        return discordId;
    }

    public void setDiscordId(String discordId) {
        this.discordId = discordId;
    }

    public void startSession() {
        this.currentSessionStart = System.currentTimeMillis();
        this.lastLoginTime = System.currentTimeMillis();
    }

    public void endSession() {
        if (currentSessionStart > 0) {
            long sessionDuration = System.currentTimeMillis() - currentSessionStart;
            addPlayTime(sessionDuration);
            currentSessionStart = -1;
        }
    }

    public String getFormattedPlayTime() {
        long seconds = totalPlayTime / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;

        if (days > 0) {
            return String.format("%dd %dh %dm", days, hours % 24, minutes % 60);
        } else if (hours > 0) {
            return String.format("%dh %dm", hours, minutes % 60);
        } else if (minutes > 0) {
            return String.format("%dm %ds", minutes, seconds % 60);
        } else {
            return String.format("%ds", seconds);
        }
    }
    
    public int getTotalDeaths() { return totalDeaths; }
    public void setTotalDeaths(int totalDeaths) { this.totalDeaths = totalDeaths; }
    public void incrementDeaths() { this.totalDeaths++; }
    
    public int getBlocksPlaced() { return blocksPlaced; }
    public void setBlocksPlaced(int blocksPlaced) { this.blocksPlaced = blocksPlaced; }
    public void incrementBlocksPlaced() { this.blocksPlaced++; }
    
    public int getBlocksBroken() { return blocksBroken; }
    public void setBlocksBroken(int blocksBroken) { this.blocksBroken = blocksBroken; }
    public void incrementBlocksBroken() { this.blocksBroken++; }
    
    public int getPlayerKills() { return playerKills; }
    public void setPlayerKills(int playerKills) { this.playerKills = playerKills; }
    public void incrementPlayerKills() { this.playerKills++; }
}
