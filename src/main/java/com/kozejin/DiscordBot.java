package com.kozejin;

import com.google.gson.JsonObject;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.requests.GatewayIntent;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;

public class DiscordBot extends ListenerAdapter {
    private static final HttpClient HTTP_CLIENT = HttpClient.newHttpClient();
    
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
            
            syncCommands();
            
            updatePlayerCount(0, 0);
            future.complete(true);

        } catch (Exception e) {
            System.out.println("[Discord] Failed to start bot: " + e.getMessage());
            e.printStackTrace();
            future.complete(false);
        }

        return future;
    }

    public void syncCommands() {
        if (jda == null) return;
        
        jda.updateCommands().addCommands(
            Commands.slash("link", "Get a link code to connect your Discord account to Hytale"),
            Commands.slash("profile", "View player profile and stats")
                .addOption(OptionType.STRING, "username", "Player username (optional)", false),
            Commands.slash("players", "List online players")
                .addOption(OptionType.INTEGER, "page", "Page number (optional)", false),
            Commands.slash("me", "View your linked Hytale profile"),
            Commands.slash("sync", "Sync slash commands with Discord")
        ).queue(
            success -> System.out.println("[Discord] Slash commands synced successfully"),
            error -> System.err.println("[Discord] Failed to sync slash commands: " + error.getMessage())
        );
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
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        switch (event.getName()) {
            case "link":
                handleLinkSlashCommand(event);
                break;
            case "profile":
                handleProfileSlashCommand(event);
                break;
            case "players":
                handlePlayersSlashCommand(event);
                break;
            case "me":
                handleMeSlashCommand(event);
                break;
            case "sync":
                handleSyncSlashCommand(event);
                break;
        }
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        if (event.getAuthor().isBot() && !config.isAllowOtherBotMessages()) return;
        if (event.getAuthor().getId().equals(jda.getSelfUser().getId())) return;
        
        String channelId = event.getChannel().getId();
        String username = event.getAuthor().getName();
        String message = event.getMessage().getContentDisplay();

        if (message.equalsIgnoreCase("!sync") && hasAdminRole(event.getMember())) {
            syncCommands();
            event.getChannel().sendMessage("Slash commands synced with Discord!").queue();
            return;
        }

        if (channelId.equals(config.getChannelId())) {
            onDiscordMessage.accept(username, message);
        }
    }

    private void handleLinkSlashCommand(SlashCommandInteractionEvent event) {
        String discordId = event.getUser().getId();
        String discordUsername = event.getUser().getName();

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

        event.getUser().openPrivateChannel().queue(privateChannel -> {
            privateChannel.sendMessageEmbeds(embed).queue(
                success -> {
                    event.reply("Check your DMs for your link code!").setEphemeral(true).queue();
                    System.out.println("[Discord Integration] Generated link code for " + discordUsername + ": " + code);
                },
                error -> {
                    event.reply("Could not send you a DM. Please enable DMs from server members.").setEphemeral(true).queue();
                    System.out.println("[Discord Integration] Failed to DM link code to " + discordUsername);
                }
            );
        });
    }
    
    private void handleProfileSlashCommand(SlashCommandInteractionEvent event) {
        String username = event.getOption("username") != null ? event.getOption("username").getAsString() : null;
        String discordId = event.getUser().getId();
        
        PlayerDataStorage storage = DiscordIntegration.getInstance().getPlayerDataStorage();
        PlayerData playerData = null;
        String targetUsername = null;
        
        if (username != null) {
            for (PlayerData data : storage.getAllPlayers().values()) {
                if (data.getUsername().equalsIgnoreCase(username)) {
                    playerData = data;
                    break;
                }
            }
            
            if (playerData == null) {
                event.reply("Player `" + username + "` not found!").setEphemeral(true).queue();
                return;
            }
            targetUsername = username;
        } else {
            playerData = storage.getPlayerByDiscordId(discordId);
            
            if (playerData == null) {
                event.reply("Your Discord account is not linked! Use `/link` to get a link code.").setEphemeral(true).queue();
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
            .addField("Player Kills", String.valueOf(playerData.getPlayerKills()), true)
            .addField("Mob Kills", String.valueOf(playerData.getMobKills()), true)
            .addField("Deaths", String.valueOf(playerData.getTotalDeaths()), true)
            .addField("Blocks Placed", String.valueOf(playerData.getBlocksPlaced()), true)
            .addField("Blocks Broken", String.valueOf(playerData.getBlocksBroken()), true)
            .setFooter("Discord Integration", null)
            .build();
        
        event.replyEmbeds(embed).queue();
    }
    
    private void handlePlayersSlashCommand(SlashCommandInteractionEvent event) {
        if (!hasAdminRole(event.getMember())) {
            event.reply("You need admin permissions to use this command.").setEphemeral(true).queue();
            return;
        }
        
        com.hypixel.hytale.server.core.universe.Universe universe = 
            com.hypixel.hytale.server.core.universe.Universe.get();
        java.util.Collection<com.hypixel.hytale.server.core.universe.PlayerRef> onlinePlayers = universe.getPlayers();
        int playerCount = onlinePlayers.size();
        
        if (playerCount == 0) {
            event.reply("No players are currently online.").queue();
            return;
        }
        
        java.util.List<String> playerNames = new java.util.ArrayList<>();
        for (com.hypixel.hytale.server.core.universe.PlayerRef player : onlinePlayers) {
            playerNames.add(player.getUsername());
        }
        
        int page = event.getOption("page") != null ? event.getOption("page").getAsInt() : 1;
        if (page < 1) page = 1;
        
        int playersPerPage = 10;
        int totalPages = (int) Math.ceil((double) playerNames.size() / playersPerPage);
        
        if (page > totalPages) page = totalPages;
        
        int startIndex = (page - 1) * playersPerPage;
        int endIndex = Math.min(startIndex + playersPerPage, playerNames.size());
        
        StringBuilder playerList = new StringBuilder();
        for (int i = startIndex; i < endIndex; i++) {
            playerList.append((i + 1)).append(". ").append(playerNames.get(i)).append("\n");
        }
        
        MessageEmbed embed = new EmbedBuilder()
            .setTitle("Players Online (" + playerNames.size() + ")")
            .setColor(0x00FFFF)
            .setDescription(playerList.toString())
            .setFooter("Page " + page + "/" + totalPages, null)
            .build();
        
        event.replyEmbeds(embed).queue();
    }
    
    private void handleMeSlashCommand(SlashCommandInteractionEvent event) {
        String discordId = event.getUser().getId();
        
        PlayerDataStorage storage = DiscordIntegration.getInstance().getPlayerDataStorage();
        PlayerData playerData = storage.getPlayerByDiscordId(discordId);
        
        if (playerData == null) {
            event.reply("Your Discord account is not linked! Use `/link` to get a link code.").setEphemeral(true).queue();
            return;
        }
        
        String discordTag = "<@" + playerData.getDiscordId() + ">";
        long firstLogin = playerData.getFirstLoginTime();
        String firstLoginDate = new java.text.SimpleDateFormat("MMM dd, yyyy").format(new java.util.Date(firstLogin));
        
        MessageEmbed embed = new EmbedBuilder()
            .setTitle("Your Profile: " + playerData.getUsername())
            .setColor(0x5865F2)
            .addField("Total Playtime", playerData.getFormattedPlayTime(), true)
            .addField("First Login", firstLoginDate, true)
            .addField("Discord", discordTag, false)
            .addField("Player Kills", String.valueOf(playerData.getPlayerKills()), true)
            .addField("Mob Kills", String.valueOf(playerData.getMobKills()), true)
            .addField("Deaths", String.valueOf(playerData.getTotalDeaths()), true)
            .addField("Blocks Placed", String.valueOf(playerData.getBlocksPlaced()), true)
            .addField("Blocks Broken", String.valueOf(playerData.getBlocksBroken()), true)
            .setFooter("Discord Integration", null)
            .build();
        
        event.replyEmbeds(embed).setEphemeral(true).queue();
    }
    
    private void handleSyncSlashCommand(SlashCommandInteractionEvent event) {
        if (!hasAdminRole(event.getMember())) {
            event.reply("You need admin permissions to use this command.").setEphemeral(true).queue();
            return;
        }
        
        syncCommands();
        event.reply("Slash commands synced with Discord!").setEphemeral(true).queue();
    }
    
    private boolean hasAdminRole(net.dv8tion.jda.api.entities.Member member) {
        if (member == null) return false;
        String adminRoleId = config.getAdminRoleId();
        if (adminRoleId == null || adminRoleId.isEmpty()) return true;
        return member.getRoles().stream().anyMatch(role -> role.getId().equals(adminRoleId));
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

    public void assignLinkedRole(String discordId) {
        if (jda == null || config.getLinkedRoleId() == null || config.getLinkedRoleId().isEmpty()) {
            return;
        }

        jda.getGuilds().forEach(guild -> {
            guild.retrieveMemberById(discordId).queue(
                member -> {
                    Role role = guild.getRoleById(config.getLinkedRoleId());
                    if (role != null) {
                        guild.addRoleToMember(member, role).queue(
                            success -> System.out.println("[Discord] Assigned linked role to " + member.getUser().getName()),
                            error -> System.err.println("[Discord] Failed to assign role: " + error.getMessage())
                        );
                    }
                },
                error -> System.err.println("[Discord] Failed to retrieve member: " + error.getMessage())
            );
        });
    }
    
    public void sendLinkConfirmationDM(String discordId, String hytaleUsername) {
        if (jda == null) return;
        
        jda.retrieveUserById(discordId).queue(
            user -> {
                user.openPrivateChannel().queue(
                    channel -> {
                        channel.sendMessage("Your Discord account has been successfully linked to your Hytale account **" + hytaleUsername + "**!").queue(
                            success -> System.out.println("[Discord] Sent link confirmation DM to " + user.getName()),
                            error -> System.err.println("[Discord] Failed to send DM: " + error.getMessage())
                        );
                    },
                    error -> System.err.println("[Discord] Failed to open DM channel: " + error.getMessage())
                );
            },
            error -> System.err.println("[Discord] Failed to retrieve user: " + error.getMessage())
        );
    }

    public void sendWebhookMessage(String username, String content, String avatarUrl) {
        if (!config.isUseWebhooks() || config.getWebhookUrl() == null || config.getWebhookUrl().isEmpty()) {
            sendMessage("**" + username + "**: " + content);
            return;
        }

        JsonObject json = new JsonObject();
        json.addProperty("username", username);
        json.addProperty("content", content);
        if (avatarUrl != null && !avatarUrl.isEmpty()) {
            json.addProperty("avatar_url", avatarUrl);
        }

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(config.getWebhookUrl()))
                .header("Content-Type", "application/json")
                .header("User-Agent", "HytaleDiscordIntegration/1.0")
                .POST(HttpRequest.BodyPublishers.ofString(json.toString()))
                .build();

        HTTP_CLIENT.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenAccept(response -> {
                    if (response.statusCode() >= 400) {
                        System.err.println("[Discord] Webhook Error: " + response.statusCode() + " - " + response.body());
                    }
                })
                .exceptionally(ex -> {
                    System.err.println("[Discord] Webhook Network Error: " + ex.getMessage());
                    return null;
                });
    }

    public void dispatchToDiscord(UUID playerUuid, String name, String content, String fallbackAvatar) {
        if (!config.isUseWebhooks()) {
            if (!name.equals(config.getServerName())) {
                sendMessage("**" + name + "**: " + content);
            } else {
                sendMessage(content);
            }
            return;
        }

        AvatarCache.setExpireTime(config.getAvatarCacheMinutes());

        try {
            if (playerUuid == null) {
                sendWebhookMessage(name, content, fallbackAvatar);
                return;
            }

            String cachedAvatar = AvatarCache.get(playerUuid.toString());
            if (cachedAvatar != null) {
                sendWebhookMessage(name, content, cachedAvatar);
                return;
            }

            String hytaleAvatarUrl = "https://hyvatar.io/render/" + name + "?size=128";
            AvatarCache.put(playerUuid.toString(), hytaleAvatarUrl);
            sendWebhookMessage(name, content, hytaleAvatarUrl);
        } catch (Exception e) {
            System.err.println("[Discord] Dispatch error: " + e.getMessage());
            e.printStackTrace();
            sendWebhookMessage(name, content, fallbackAvatar);
        }
    }
}
