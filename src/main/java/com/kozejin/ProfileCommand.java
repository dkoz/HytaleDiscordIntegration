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
import java.text.SimpleDateFormat;
import java.util.Date;

public class ProfileCommand extends AbstractPlayerCommand {

    public ProfileCommand() {
        super("profile", "View your or another player's profile", false);
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
        String[] parts = input.split("\\s+");
        
        DiscordIntegration plugin = DiscordIntegration.getInstance();
        PlayerDataStorage storage = plugin.getPlayerDataStorage();
        PlayerData playerData;
        String targetUsername;
        
        if (parts.length > 1) {
            targetUsername = parts[1];
            playerData = null;
            
            for (PlayerData data : storage.getAllPlayers().values()) {
                if (data.getUsername().equalsIgnoreCase(targetUsername)) {
                    playerData = data;
                    break;
                }
            }
            
            if (playerData == null) {
                player.sendMessage(Message.raw("Player '" + targetUsername + "' not found!"));
                return;
            }
        } else {
            playerData = storage.getPlayerData(player.getUuid());
            targetUsername = player.getUsername();
            
            if (playerData == null) {
                player.sendMessage(Message.raw("Error: Your player data not found!"));
                return;
            }
        }
        
        SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy");
        String firstLoginDate = dateFormat.format(new Date(playerData.getFirstLoginTime()));
        String discordStatus = playerData.getDiscordId() != null ? "Linked" : "Not linked";
        
        player.sendMessage(Message.raw("Player Profile: " + targetUsername));
        player.sendMessage(Message.raw("Total Playtime: " + playerData.getFormattedPlayTime()));
        player.sendMessage(Message.raw("First Login: " + firstLoginDate));
        player.sendMessage(Message.raw("Discord: " + discordStatus));
        player.sendMessage(Message.raw("Deaths: " + playerData.getTotalDeaths()));
        player.sendMessage(Message.raw("Player Kills: " + playerData.getPlayerKills()));
        player.sendMessage(Message.raw("Blocks Placed: " + playerData.getBlocksPlaced()));
        player.sendMessage(Message.raw("Blocks Broken: " + playerData.getBlocksBroken()));
    }
}
