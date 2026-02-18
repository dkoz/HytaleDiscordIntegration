package com.kozejin;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;
import java.io.File;

public class DiscordConfigCommand extends AbstractPlayerCommand {

    public DiscordConfigCommand() {
        super("discord", "Manage Discord integration settings", false);
        this.setAllowsExtraArguments(true);
    }
    
    @Override
    protected boolean canGeneratePermission() {
        return true;
    }

    @Override
    protected void execute(
            @Nonnull CommandContext context,
            @Nonnull Store<EntityStore> store,
            @Nonnull Ref<EntityStore> ref,
            @Nonnull PlayerRef player,
            @Nonnull World world
    ) {
        String input = context.getInputString().trim();
        String[] args = input.split("\\s+", 3);
        
        if (args.length <= 1) {
            showConfigHelp(player);
            return;
        }

        String action = args[1].toLowerCase();
        
        if ("get".equals(action) && args.length >= 3) {
            getConfigValue(player, args[2]);
        } else if ("set".equals(action) && args.length >= 3) {
            String[] setArgs = args[2].split("\\s+", 2);
            if (setArgs.length >= 2) {
                setConfigValue(player, setArgs[0], setArgs[1]);
            } else {
                player.sendMessage(Message.raw("Usage: /discord set <field> <value>"));
            }
        } else if ("list".equals(action)) {
            listConfigValues(player);
        } else if ("reload".equals(action)) {
            reloadConfig(player);
        } else if ("restart".equals(action)) {
            restartBot(player);
        } else if ("status".equals(action)) {
            showStatus(player);
        } else if ("sync".equals(action)) {
            syncCommands(player);
        } else {
            showConfigHelp(player);
        }
    }

    private void showConfigHelp(PlayerRef player) {
        player.sendMessage(Message.raw("=== Discord Config Commands ==="));
        player.sendMessage(Message.raw("/discord get <field> - Get config value"));
        player.sendMessage(Message.raw("/discord set <field> <value> - Set config value"));
        player.sendMessage(Message.raw("/discord list - Show all config values"));
        player.sendMessage(Message.raw("/discord reload - Reload config from file"));
        player.sendMessage(Message.raw("/discord restart - Restart the Discord bot"));
        player.sendMessage(Message.raw("/discord status - Show bot connection status"));
        player.sendMessage(Message.raw("/discord sync - Sync slash commands with Discord"));
        player.sendMessage(Message.raw("=== Available Fields ==="));
        player.sendMessage(Message.raw("General: enabled, channelId, chatChannelId, commandChannelId, adminRoleId, linkedRoleId"));
        player.sendMessage(Message.raw("Chat: enableInGameChat, showChatTag, chatTagText, allowOtherBotMessages"));
        player.sendMessage(Message.raw("Messages: enableDeathMessages"));
        player.sendMessage(Message.raw("Webhooks: useWebhooks, webhookUrl, chatWebhookUrl, serverName, serverAvatarUrl, defaultPlayerAvatarUrl, avatarCacheMinutes"));
    }

    private void getConfigValue(PlayerRef player, String fieldName) {
        DiscordConfig config = DiscordIntegration.getInstance().config;
        
        try {
            Object value = getFieldValue(config, fieldName);
            player.sendMessage(Message.raw(fieldName + ": " + value));
        } catch (Exception e) {
            player.sendMessage(Message.raw("Error getting field '" + fieldName + "': " + e.getMessage()));
        }
    }

    private void setConfigValue(PlayerRef player, String fieldName, String value) {
        DiscordConfig config = DiscordIntegration.getInstance().config;
        
        try {
            setFieldValue(config, fieldName, value);
            saveConfig(config);
            player.sendMessage(Message.raw("Set " + fieldName + " to: " + value));
            System.out.println("[Discord Integration] Config updated in-game: " + fieldName + " = " + value);
        } catch (Exception e) {
            player.sendMessage(Message.raw("Error setting field '" + fieldName + "': " + e.getMessage()));
        }
    }

    private void listConfigValues(PlayerRef player) {
        DiscordConfig config = DiscordIntegration.getInstance().config;
        
        player.sendMessage(Message.raw("=== General ==="));
        player.sendMessage(Message.raw("enabled: " + config.isEnabled()));
        player.sendMessage(Message.raw("channelId: " + config.getChannelId()));
        player.sendMessage(Message.raw("chatChannelId: " + config.getChatChannelId()));
        player.sendMessage(Message.raw("commandChannelId: " + config.getCommandChannelId()));
        player.sendMessage(Message.raw("adminRoleId: " + config.getAdminRoleId()));
        player.sendMessage(Message.raw("linkedRoleId: " + config.getLinkedRoleId()));
        player.sendMessage(Message.raw("=== Chat ==="));
        player.sendMessage(Message.raw("enableInGameChat: " + config.isEnableInGameChat()));
        player.sendMessage(Message.raw("showChatTag: " + config.isShowChatTag()));
        player.sendMessage(Message.raw("chatTagText: " + config.getChatTagText()));
        player.sendMessage(Message.raw("allowOtherBotMessages: " + config.isAllowOtherBotMessages()));
        player.sendMessage(Message.raw("=== Messages ==="));
        player.sendMessage(Message.raw("enableDeathMessages: " + config.isEnableDeathMessages()));
        player.sendMessage(Message.raw("=== Webhooks ==="));
        player.sendMessage(Message.raw("useWebhooks: " + config.isUseWebhooks()));
        player.sendMessage(Message.raw("webhookUrl: " + config.getWebhookUrl()));
        player.sendMessage(Message.raw("chatWebhookUrl: " + config.getChatWebhookUrl()));
        player.sendMessage(Message.raw("serverName: " + config.getServerName()));
        player.sendMessage(Message.raw("serverAvatarUrl: " + config.getServerAvatarUrl()));
        player.sendMessage(Message.raw("defaultPlayerAvatarUrl: " + config.getDefaultPlayerAvatarUrl()));
        player.sendMessage(Message.raw("avatarCacheMinutes: " + config.getAvatarCacheMinutes()));
    }

    private void reloadConfig(PlayerRef player) {
        try {
            DiscordIntegration.getInstance().loadConfig();
            player.sendMessage(Message.raw("Config reloaded successfully!"));
        } catch (Exception e) {
            player.sendMessage(Message.raw("Error reloading config: " + e.getMessage()));
        }
    }
    
    private void restartBot(PlayerRef player) {
        player.sendMessage(Message.raw("Restarting Discord bot..."));
        try {
            DiscordIntegration.getInstance().restartBot();
            player.sendMessage(Message.raw("Bot restart initiated. Check console for status."));
        } catch (Exception e) {
            player.sendMessage(Message.raw("Error restarting bot: " + e.getMessage()));
        }
    }
    
    private void syncCommands(PlayerRef player) {
        DiscordIntegration plugin = DiscordIntegration.getInstance();
        if (plugin.isBotConnected()) {
            plugin.discordBot.syncCommands();
            player.sendMessage(Message.raw("Slash commands synced with Discord!"));
        } else {
            player.sendMessage(Message.raw("Bot is not connected. Cannot sync commands."));
        }
    }
    
    private void showStatus(PlayerRef player) {
        boolean connected = DiscordIntegration.getInstance().isBotConnected();
        DiscordConfig config = DiscordIntegration.getInstance().config;
        
        player.sendMessage(Message.raw("=== Discord Bot Status ==="));
        player.sendMessage(Message.raw("Bot Enabled: " + config.isEnabled()));
        player.sendMessage(Message.raw("Bot Connected: " + connected));
        player.sendMessage(Message.raw("Channel ID: " + config.getChannelId()));
        player.sendMessage(Message.raw("Chat Channel ID: " + config.getEffectiveChatChannelId()));
        player.sendMessage(Message.raw("Command Channel ID: " + config.getCommandChannelId()));
        player.sendMessage(Message.raw("Webhooks: " + (config.isUseWebhooks() ? "Enabled" : "Disabled")));
        player.sendMessage(Message.raw("In-Game Chat: " + (config.isEnableInGameChat() ? "Enabled" : "Disabled")));
        player.sendMessage(Message.raw("Death Messages: " + (config.isEnableDeathMessages() ? "Enabled" : "Disabled")));
    }

    private Object getFieldValue(DiscordConfig config, String fieldName) throws Exception {
        switch (fieldName.toLowerCase()) {
            case "enabled":
                return config.isEnabled();
            case "showchattag":
                return config.isShowChatTag();
            case "enableingamechat":
                return config.isEnableInGameChat();
            case "chattagtext":
                return config.getChatTagText();
            case "channelid":
                return config.getChannelId();
            case "chatchannelid":
                return config.getChatChannelId();
            case "commandchannelid":
                return config.getCommandChannelId();
            case "adminroleid":
                return config.getAdminRoleId();
            case "linkedroleid":
                return config.getLinkedRoleId();
            case "allowotherbotmessages":
                return config.isAllowOtherBotMessages();
            case "enabledeathmessages":
                return config.isEnableDeathMessages();
            case "usewebhooks":
                return config.isUseWebhooks();
            case "webhookurl":
                return config.getWebhookUrl();
            case "chatwebhookurl":
                return config.getChatWebhookUrl();
            case "servername":
                return config.getServerName();
            case "serveravatarurl":
                return config.getServerAvatarUrl();
            case "defaultplayeravatarurl":
                return config.getDefaultPlayerAvatarUrl();
            case "avatarcacheminutes":
                return config.getAvatarCacheMinutes();
            default:
                throw new Exception("Unknown field: " + fieldName);
        }
    }

    private void setFieldValue(DiscordConfig config, String fieldName, String value) throws Exception {
        switch (fieldName.toLowerCase()) {
            case "enabled":
                config.setEnabled(Boolean.parseBoolean(value));
                break;
            case "showchattag":
                config.setShowChatTag(Boolean.parseBoolean(value));
                break;
            case "enableingamechat":
                config.setEnableInGameChat(Boolean.parseBoolean(value));
                break;
            case "chattagtext":
                config.setChatTagText(value.replace("\"", ""));
                break;
            case "channelid":
                config.setChannelId(value.replace("\"", ""));
                break;
            case "chatchannelid":
                config.setChatChannelId(value.replace("\"", ""));
                break;
            case "commandchannelid":
                config.setCommandChannelId(value.replace("\"", ""));
                break;
            case "adminroleid":
                config.setAdminRoleId(value.replace("\"", ""));
                break;
            case "linkedroleid":
                config.setLinkedRoleId(value.replace("\"", ""));
                break;
            case "allowotherbotmessages":
                config.setAllowOtherBotMessages(Boolean.parseBoolean(value));
                break;
            case "enabledeathmessages":
                config.setEnableDeathMessages(Boolean.parseBoolean(value));
                break;
            case "usewebhooks":
                config.setUseWebhooks(Boolean.parseBoolean(value));
                break;
            case "webhookurl":
                config.setWebhookUrl(value.replace("\"", ""));
                break;
            case "chatwebhookurl":
                config.setChatWebhookUrl(value.replace("\"", ""));
                break;
            case "servername":
                config.setServerName(value.replace("\"", ""));
                break;
            case "serveravatarurl":
                config.setServerAvatarUrl(value.replace("\"", ""));
                break;
            case "defaultplayeravatarurl":
                config.setDefaultPlayerAvatarUrl(value.replace("\"", ""));
                break;
            case "avatarcacheminutes":
                config.setAvatarCacheMinutes(Integer.parseInt(value));
                break;
            default:
                throw new Exception("Unknown field: " + fieldName);
        }
    }

    private void saveConfig(DiscordConfig config) {
        File configFile = new File("mods/DiscordIntegration/config.json");
        DiscordIntegration.getInstance().saveConfig(configFile);
    }
}
