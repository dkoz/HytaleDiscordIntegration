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
        return false;
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
        player.sendMessage(Message.raw("Fields: enabled, showChatTag, enableInGameChat, chatTagText, channelId, commandChannelId, adminRoleId"));
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
        
        player.sendMessage(Message.raw("=== Discord Config Values ==="));
        player.sendMessage(Message.raw("enabled: " + config.isEnabled()));
        player.sendMessage(Message.raw("showChatTag: " + config.isShowChatTag()));
        player.sendMessage(Message.raw("enableInGameChat: " + config.isEnableInGameChat()));
        player.sendMessage(Message.raw("chatTagText: " + config.getChatTagText()));
        player.sendMessage(Message.raw("channelId: " + config.getChannelId()));
        player.sendMessage(Message.raw("commandChannelId: " + config.getCommandChannelId()));
        player.sendMessage(Message.raw("adminRoleId: " + config.getAdminRoleId()));
    }

    private void reloadConfig(PlayerRef player) {
        try {
            DiscordIntegration.getInstance().loadConfig();
            player.sendMessage(Message.raw("Config reloaded successfully!"));
        } catch (Exception e) {
            player.sendMessage(Message.raw("Error reloading config: " + e.getMessage()));
        }
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
            case "commandchannelid":
                return config.getCommandChannelId();
            case "adminroleid":
                return config.getAdminRoleId();
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
            case "commandchannelid":
                config.setCommandChannelId(value.replace("\"", ""));
                break;
            case "adminroleid":
                config.setAdminRoleId(value.replace("\"", ""));
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
