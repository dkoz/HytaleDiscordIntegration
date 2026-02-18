package com.kozejin;

import java.util.UUID;

public class MessageRelay {
    private final DiscordConfig config;

    public MessageRelay(DiscordConfig config) {
        this.config = config;
    }

    public void sendToDiscord(String playerName, String message) {
        sendToDiscord(null, playerName, message);
    }

    public void sendToDiscord(UUID playerUuid, String playerName, String message) {
        System.out.println("[Discord Integration] MessageRelay.sendToDiscord called for: " + playerName);
        DiscordBot bot = DiscordIntegration.getInstance().discordBot;
        if (bot == null) {
            System.out.println("[Discord Integration] Bot is null!");
            return;
        }
        if (!bot.isConnected()) {
            System.out.println("[Discord Integration] Bot is not connected!");
            return;
        }

        if (config.isUseWebhooks()) {
            String fallbackAvatar = config.getDefaultPlayerAvatarUrl();
            bot.dispatchToDiscord(playerUuid, playerName, message, fallbackAvatar, true);
        } else {
            String formatted = config.getMessageFormat().getServerToDiscord()
                .replace("{player}", playerName)
                .replace("{message}", message);
            System.out.println("[Discord Integration] Sending to Discord: " + formatted);
            bot.sendChatMessage(formatted);
        }
    }

    public void sendJoinMessage(String playerName) {
        DiscordBot bot = DiscordIntegration.getInstance().discordBot;
        if (bot != null && bot.isConnected()) {
            String formatted = config.getMessageFormat().getJoinMessage()
                .replace("{player}", playerName);
            
            if (config.isUseWebhooks()) {
                bot.dispatchToDiscord(null, config.getServerName(), formatted, config.getServerAvatarUrl());
            } else {
                bot.sendMessage(formatted);
            }
        }
    }

    public void sendLeaveMessage(String username) {
        DiscordBot bot = DiscordIntegration.getInstance().discordBot;
        if (bot != null && bot.isConnected()) {
            String formatted = config.getMessageFormat().getLeaveMessage()
                .replace("{player}", username);
            
            if (config.isUseWebhooks()) {
                bot.dispatchToDiscord(null, config.getServerName(), formatted, config.getServerAvatarUrl());
            } else {
                System.out.println("[Discord Integration] Sending to Discord: " + formatted);
                bot.sendMessage(formatted);
            }
        }
    }

    public void sendServerStartMessage() {
        DiscordBot bot = DiscordIntegration.getInstance().discordBot;
        if (bot != null && bot.isConnected()) {
            String formatted = config.getMessageFormat().getServerStartMessage();
            
            if (config.isUseWebhooks()) {
                bot.dispatchToDiscord(null, config.getServerName(), formatted, config.getServerAvatarUrl());
            } else {
                System.out.println("[Discord Integration] Sending to Discord: " + formatted);
                bot.sendMessage(formatted);
            }
        }
    }

    public void sendServerStopMessage() {
        DiscordBot bot = DiscordIntegration.getInstance().discordBot;
        if (bot != null && bot.isConnected()) {
            String formatted = config.getMessageFormat().getServerStopMessage();
            
            if (config.isUseWebhooks()) {
                bot.dispatchToDiscord(null, config.getServerName(), formatted, config.getServerAvatarUrl());
            } else {
                System.out.println("[Discord Integration] Sending to Discord: " + formatted);
                bot.sendMessage(formatted);
            }
        }
    }

    public void sendDeathMessage(String playerName, String cause) {
        if (!config.isEnableDeathMessages()) {
            return;
        }

        DiscordBot bot = DiscordIntegration.getInstance().discordBot;
        if (bot != null && bot.isConnected()) {
            String causeText = (cause == null || cause.isEmpty()) ? "died" : cause;
            String formatted = config.getMessageFormat().getDeathMessage()
                .replace("{player}", playerName)
                .replace("{cause}", causeText);
            
            if (config.isUseWebhooks()) {
                bot.dispatchToDiscord(null, config.getServerName(), formatted, config.getServerAvatarUrl());
            } else {
                System.out.println("[Discord Integration] Sending to Discord: " + formatted);
                bot.sendMessage(formatted);
            }
        }
    }
}
