package com.kozejin;

import java.awt.Color;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.event.EventPriority;
import com.hypixel.hytale.server.core.event.events.player.PlayerChatEvent;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import javax.annotation.Nonnull;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;

public class DiscordIntegration extends JavaPlugin {
    
    private static DiscordIntegration instance;
    
    public DiscordConfig config;
    DiscordBot discordBot;
    private MessageRelay messageRelay;
    private PlayerDataStorage playerDataStorage;
    private LinkCodeManager linkCodeManager;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    public DiscordIntegration(@Nonnull JavaPluginInit init) {
        super(init);
        instance = this;
    }

    @Override
    protected void setup() {
        super.setup();
        System.out.println("[Discord Integration] Plugin loading...");
        System.out.println("[Discord Integration] Made by Kozejin");

        loadConfig();
        
        File dataFolder = new File("mods/DiscordIntegration");
        playerDataStorage = new PlayerDataStorage(dataFolder);
        linkCodeManager = new LinkCodeManager();
        
        messageRelay = new MessageRelay(config);
        
        discordBot = new DiscordBot(config, this::handleDiscordMessage);

        discordBot.start().thenAccept(success -> {
            if (success) {
                System.out.println("[Discord Integration] Successfully connected to Discord!");
                System.out.println("[Discord Integration] Two-way chat bridge is active");
            } else {
                System.out.println("[Discord Integration] Failed to connect to Discord");
                System.out.println("[Discord Integration] Please check your configuration");
            }
        });

        System.out.println("[Discord Integration] Registering event listeners...");
        
        getEventRegistry().registerAsyncGlobal(EventPriority.LAST, PlayerChatEvent.class, future ->
            future.thenApply(event -> {
                onPlayerChat(event);
                return event;
            })
        );
        
        getEventRegistry().register(com.hypixel.hytale.server.core.event.events.player.PlayerConnectEvent.class, this::onPlayerJoin);
        getEventRegistry().register(com.hypixel.hytale.server.core.event.events.player.PlayerDisconnectEvent.class, this::onPlayerLeave);
        
        getCommandRegistry().registerCommand(new LinkCommand());
        getCommandRegistry().registerCommand(new ProfileCommand());
        getCommandRegistry().registerCommand(new DiscordConfigCommand());
        
        System.out.println("[Discord Integration] Event listeners and commands registered!");

        System.out.println("[Discord Integration] Plugin enabled!");
    }

    private void onPlayerChat(PlayerChatEvent event) {
        System.out.println("[Discord Integration] Chat event received!");
        System.out.println("[Discord Integration] Cancelled: " + event.isCancelled());
        
        PlayerRef sender = event.getSender();
        String message = event.getContent();
        
        System.out.println("[Discord Integration] Processing chat: " + sender.getUsername() + ": " + message);
        
        PlayerData data = playerDataStorage.getPlayerData(sender.getUuid());
        
        if (data != null && data.getDiscordId() != null && config.isShowChatTag()) {
            event.setFormatter((playerRef, msg) -> {
                String tagText = config.getChatTagText();
                DiscordConfig.ChatTagColors colors = config.getChatTagColors();
                return Message.join(
                    Message.raw("[").color(Color.decode(colors.getBracketColor())),
                    Message.raw(tagText).color(Color.decode(colors.getTagColor())),
                    Message.raw("] ").color(Color.decode(colors.getBracketColor())),
                    Message.raw(playerRef.getUsername()).color(Color.decode(colors.getUsernameColor())),
                    Message.raw(": ").color(Color.decode(colors.getMessageColor())),
                    Message.raw(msg).color(Color.decode(colors.getMessageColor()))
                );
            });
        }
        
        handlePlayerChat(sender.getUsername(), message);
    }

    public void onDisable() {
        System.out.println("[Discord Integration] Plugin disabling...");
        
        if (playerDataStorage != null) {
            playerDataStorage.saveAllPlayers();
        }
        
        if (discordBot != null) {
            discordBot.shutdown();
        }
        
        System.out.println("[Discord Integration] Plugin disabled!");
    }
    
    private void onPlayerJoin(com.hypixel.hytale.server.core.event.events.player.PlayerConnectEvent event) {
        PlayerRef playerRef = event.getPlayerRef();
        PlayerData data = playerDataStorage.getOrCreatePlayerData(playerRef.getUuid(), playerRef.getUsername());
        
        data.setUsername(playerRef.getUsername());
        data.startSession();
        
        System.out.println("[Discord Integration] Player joined: " + playerRef.getUsername() + " (Total playtime: " + data.getFormattedPlayTime() + ")");
        
        handlePlayerJoin(playerRef.getUsername());
        updatePlayerCount();
    }
    
    private void onPlayerLeave(com.hypixel.hytale.server.core.event.events.player.PlayerDisconnectEvent event) {
        PlayerRef playerRef = event.getPlayerRef();
        PlayerData data = playerDataStorage.getPlayerData(playerRef.getUuid());
        
        if (data != null) {
            data.endSession();
            System.out.println("[Discord Integration] Player left: " + playerRef.getUsername() + " (Session time: " + (System.currentTimeMillis() - data.getLastLoginTime()) / 1000 + "s)");
        }
        
        playerDataStorage.saveAllPlayers();
        
        handlePlayerLeave(playerRef.getUsername());
        updatePlayerCount();
    }

    public void loadConfig() {
        File configFile = new File("mods/DiscordIntegration/config.json");
        
        if (!configFile.exists()) {
            configFile.getParentFile().mkdirs();
            config = new DiscordConfig();
            saveConfig(configFile);
            System.out.println("[Discord Integration] Created default config at: " + configFile.getAbsolutePath());
            System.out.println("[Discord Integration] Please configure your bot token and channel IDs!");
        } else {
            try (FileReader reader = new FileReader(configFile)) {
                config = gson.fromJson(reader, DiscordConfig.class);
                System.out.println("[Discord Integration] Config loaded successfully");
            } catch (Exception e) {
                System.out.println("[Discord Integration] Error loading config: " + e.getMessage());
                config = new DiscordConfig();
                saveConfig(configFile);
            }
        }
    }

    public void saveConfig(File configFile) {
        try (FileWriter writer = new FileWriter(configFile)) {
            gson.toJson(config, writer);
        } catch (IOException e) {
            System.out.println("[Discord Integration] Error saving config: " + e.getMessage());
        }
    }

    private void handlePlayerChat(String username, String message) {
        if (messageRelay != null && config.isEnableInGameChat()) {
            messageRelay.sendToDiscord(username, message);
        }
    }

    private void handlePlayerJoin(String username) {
        if (messageRelay != null) {
            messageRelay.sendJoinMessage(username);
        }
    }

    private void handlePlayerLeave(String username) {
        if (messageRelay != null) {
            messageRelay.sendLeaveMessage(username);
        }
    }

    private void handleDiscordMessage(String username, String message) {
        System.out.println("[Discord -> Server] Received message from " + username + ": " + message);
        
        String formattedMessage = config.getMessageFormat().getDiscordToServer()
            .replace("{user}", username)
            .replace("{message}", message);
        
        broadcastToServer(formattedMessage);
    }

    private void broadcastToServer(String message) {
        System.out.println("[Discord -> Server] Broadcasting: " + message);
        
        Message msg = Message.raw(message);
        
        for (PlayerRef player : Universe.get().getPlayers()) {
            player.sendMessage(msg);
        }
        
        System.out.println("[Discord -> Server] Broadcast complete to " + Universe.get().getPlayers().size() + " players");
    }

    private void updatePlayerCount() {
        if (discordBot != null && discordBot.isConnected()) {
            int online = Universe.get().getPlayerCount();
            discordBot.updatePlayerCount(online, 0);
        }
    }

    public static DiscordIntegration getInstance() {
        return instance;
    }
    
    public PlayerDataStorage getPlayerDataStorage() {
        return playerDataStorage;
    }
    
    public LinkCodeManager getLinkCodeManager() {
        return linkCodeManager;
    }
    
    public void notifyDiscordLink(String discordId, String minecraftUsername, boolean success) {
        if (discordBot == null || !discordBot.isConnected()) return;
        
        if (success) {
            discordBot.sendMessage("Successfully linked to **" + minecraftUsername + "**!");
        } else {
            discordBot.sendMessage("Failed to link account.");
        }
    }
}
