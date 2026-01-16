package com.kozejin;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.requests.GatewayIntent;

import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;

public class DiscordBot extends ListenerAdapter {
    private final DiscordConfig config;
    private final BiConsumer<String, String> onDiscordMessage;
    private JDA jda;
    private TextChannel textChannel;

    public DiscordBot(DiscordConfig config, BiConsumer<String, String> onDiscordMessage) {
        this.config = config;
        this.onDiscordMessage = onDiscordMessage;
    }

    public CompletableFuture<Boolean> start() {
        CompletableFuture<Boolean> future = new CompletableFuture<>();

        if (!config.isEnabled()) {
            System.out.println("[Discord] Bot is disabled in config");
            future.complete(false);
            return future;
        }

        if ("YOUR_BOT_TOKEN_HERE".equals(config.getBotToken())) {
            System.out.println("[Discord] Please set your bot token in the config!");
            future.complete(false);
            return future;
        }

        try {
            System.out.println("[Discord] Starting Discord bot...");

            jda = JDABuilder.createDefault(config.getBotToken())
                .enableIntents(
                    GatewayIntent.GUILD_MESSAGES,
                    GatewayIntent.MESSAGE_CONTENT
                )
                .addEventListeners(this)
                .build();

            jda.awaitReady();

            textChannel = jda.getTextChannelById(config.getChannelId());
            if (textChannel == null) {
                System.out.println("[Discord] Could not find channel with ID: " + config.getChannelId());
                future.complete(false);
                return future;
            }

            System.out.println("[Discord] Bot connected successfully to channel: " + textChannel.getName());
            updatePlayerCount(0, 0);
            future.complete(true);

        } catch (Exception e) {
            System.out.println("[Discord] Failed to start bot: " + e.getMessage());
            e.printStackTrace();
            future.complete(false);
        }

        return future;
    }

    public void shutdown() {
        System.out.println("[Discord] Shutting down Discord bot...");
        if (jda != null) {
            jda.shutdown();
            jda = null;
        }
        textChannel = null;
    }

    public void sendMessage(String message) {
        if (textChannel != null) {
            textChannel.sendMessage(message).queue(
                success -> System.out.println("[Discord] Message sent: " + message),
                error -> System.out.println("[Discord] Failed to send message: " + error.getMessage())
            );
        }
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        if (event.getAuthor().isBot()) return;
        
        String channelId = event.getChannel().getId();
        String username = event.getAuthor().getName();
        String message = event.getMessage().getContentDisplay();

        if (channelId.equals(config.getCommandChannelId())) {
            if (message.equalsIgnoreCase("!link")) {
                handleLinkCommand(event);
                return;
            }
            
            if (message.toLowerCase().startsWith("!profile")) {
                handleProfileCommand(event, message);
                return;
            }
            
            if (message.toLowerCase().startsWith("!players")) {
                handlePlayersCommand(event, message);
                return;
            }
        }

        if (channelId.equals(config.getChannelId())) {
            onDiscordMessage.accept(username, message);
        }
    }

    private void handleLinkCommand(MessageReceivedEvent event) {
        String discordId = event.getAuthor().getId();
        String discordUsername = event.getAuthor().getName();

        LinkCodeManager linkManager = DiscordIntegration.getInstance().getLinkCodeManager();
        String code = linkManager.generateCode(discordId, discordUsername);

        MessageEmbed embed = new EmbedBuilder()
            .setTitle("Account Linking")
            .setColor(0x5865F2)
            .addField("Your Link Code", "`" + code + "`", false)
            .addField("How to Link", "Use `/link " + code + "` in-game", false)
            .addField("Important", "Code expires in 5 minutes", false)
            .setFooter("Discord Integration", null)
            .build();

        event.getAuthor().openPrivateChannel().queue(privateChannel -> {
            privateChannel.sendMessageEmbeds(embed).queue(
                success -> {
                    MessageEmbed successEmbed = new EmbedBuilder()
                        .setTitle("Link Code Sent")
                        .setColor(0x00FF00)
                        .setDescription("Check your DMs for your link code!")
                        .setFooter("Discord Integration", null)
                        .build();
                    event.getChannel().sendMessageEmbeds(successEmbed).queue();
                    System.out.println("[Discord Integration] Generated link code for " + discordUsername + ": " + code);
                },
                error -> {
                    MessageEmbed errorEmbed = new EmbedBuilder()
                        .setTitle("DM Failed")
                        .setColor(0xFF0000)
                        .setDescription("Could not send you a DM. Please enable DMs from server members.")
                        .setFooter("Discord Integration", null)
                        .build();
                    event.getChannel().sendMessageEmbeds(errorEmbed).queue();
                    System.out.println("[Discord Integration] Failed to DM link code to " + discordUsername);
                }
            );
        });
    }
    
    private void handleProfileCommand(MessageReceivedEvent event, String message) {
        String[] parts = message.split("\\s+");
        String discordId = event.getAuthor().getId();
        
        PlayerDataStorage storage = DiscordIntegration.getInstance().getPlayerDataStorage();
        PlayerData playerData = null;
        String targetUsername = null;
        
        if (parts.length > 1) {
            targetUsername = parts[1];
            for (PlayerData data : storage.getAllPlayers().values()) {
                if (data.getUsername().equalsIgnoreCase(targetUsername)) {
                    playerData = data;
                    break;
                }
            }
            
            if (playerData == null) {
                MessageEmbed embed = new EmbedBuilder()
                    .setTitle("Player Not Found")
                    .setColor(0xFF0000)
                    .setDescription("Player `" + targetUsername + "` not found!")
                    .setFooter("Discord Integration", null)
                    .build();
                event.getChannel().sendMessageEmbeds(embed).queue();
                return;
            }
        } else {
            playerData = storage.getPlayerByDiscordId(discordId);
            
            if (playerData == null) {
                MessageEmbed embed = new EmbedBuilder()
                    .setTitle("Account Not Linked")
                    .setColor(0xFFAA00)
                    .setDescription("Your Discord account is not linked!")
                    .addField("How to Link", "Use `!link` to get a link code, then use `/link <code>` in-game", false)
                    .setFooter("Discord Integration", null)
                    .build();
                event.getChannel().sendMessageEmbeds(embed).queue();
                return;
            }
            targetUsername = playerData.getUsername();
        }
        
        String discordTag = playerData.getDiscordId() != null ? "<@" + playerData.getDiscordId() + ">" : "Not linked";
        long firstLogin = playerData.getFirstLoginTime();
        String firstLoginDate = new java.text.SimpleDateFormat("MMM dd, yyyy").format(new java.util.Date(firstLogin));
        
        MessageEmbed embed = new EmbedBuilder()
            .setTitle("Player Profile: " + targetUsername)
            .setColor(0x00FF00)
            .addField("Total Playtime", playerData.getFormattedPlayTime(), true)
            .addField("First Login", firstLoginDate, true)
            .addField("Discord", discordTag, false)
            .setFooter("Discord Integration", null)
            .build();
        
        event.getChannel().sendMessageEmbeds(embed).queue();
        System.out.println("[Discord Integration] Profile requested for: " + targetUsername);
    }
    
    private void handlePlayersCommand(MessageReceivedEvent event, String message) {
        if (!hasAdminRole(event)) {
            MessageEmbed errorEmbed = new EmbedBuilder()
                .setTitle("Access Denied")
                .setColor(0xFF0000)
                .setDescription("You need admin permissions to use this command.")
                .setFooter("Discord Integration", null)
                .build();
            event.getChannel().sendMessageEmbeds(errorEmbed).queue();
            return;
        }
        
        com.hypixel.hytale.server.core.universe.Universe universe = 
            com.hypixel.hytale.server.core.universe.Universe.get();
        java.util.Collection<com.hypixel.hytale.server.core.universe.PlayerRef> onlinePlayers = universe.getPlayers();
        int playerCount = onlinePlayers.size();
        
        if (playerCount == 0) {
            MessageEmbed embed = new EmbedBuilder()
                .setTitle("Server Status")
                .setColor(0x00FFFF)
                .setDescription("No players are currently online.")
                .setFooter("Discord Integration", null)
                .build();
            event.getChannel().sendMessageEmbeds(embed).queue();
            return;
        }
        
        java.util.List<String> playerNames = new java.util.ArrayList<>();
        for (com.hypixel.hytale.server.core.universe.PlayerRef player : onlinePlayers) {
            playerNames.add(player.getUsername());
        }
        
        String[] parts = message.split("\\s+");
        int page = 1;
        
        if (parts.length > 1) {
            try {
                page = Integer.parseInt(parts[1]);
                if (page < 1) page = 1;
            } catch (NumberFormatException e) {
                page = 1;
            }
        }
        
        int playersPerPage = 15;
        int totalPages = (int) Math.ceil((double) playerCount / playersPerPage);
        
        if (page > totalPages) {
            page = totalPages;
        }
        
        sendPlayersPage(event, playerNames, page, totalPages, playersPerPage);
        System.out.println("[Discord Integration] Players list page " + page + " requested by admin (" + playerCount + " online)");
    }
    
    private boolean hasAdminRole(MessageReceivedEvent event) {
        String adminRoleId = config.getAdminRoleId();
        if (adminRoleId == null || adminRoleId.isEmpty()) {
            return true;
        }
        
        return event.getMember() != null && 
               event.getMember().getRoles().stream()
                   .anyMatch(role -> role.getId().equals(adminRoleId));
    }
    
    private void sendPlayersPage(MessageReceivedEvent event, java.util.List<String> playerNames, int page, int totalPages, int playersPerPage) {
        int startIndex = (page - 1) * playersPerPage;
        int endIndex = Math.min(startIndex + playersPerPage, playerNames.size());
        
        StringBuilder playerList = new StringBuilder();
        for (int i = startIndex; i < endIndex; i++) {
            playerList.append("• ").append(playerNames.get(i)).append("\n");
        }
        
        MessageEmbed embed = new EmbedBuilder()
            .setTitle("Players Online (" + playerNames.size() + ")")
            .setColor(0x00FFFF)
            .setDescription(playerList.toString())
            .setFooter("Page " + page + "/" + totalPages + " | Use !players <page> to navigate", null)
            .build();
        
        event.getChannel().sendMessageEmbeds(embed).queue();
    }

    public boolean isConnected() {
        return jda != null && textChannel != null;
    }

    public void updatePlayerCount(int online, int max) {
        if (jda != null) {
            String status = max > 0 ? online + "/" + max + " players online" : online + " players online";
            jda.getPresence().setActivity(Activity.playing(status));
        }
    }
}
