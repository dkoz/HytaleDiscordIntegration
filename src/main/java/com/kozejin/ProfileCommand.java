package com.kozejin;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;
import java.util.UUID;

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
        UUID targetUuid;
        
        if (parts.length > 1) {
            String targetUsername = parts[1];
            playerData = null;
            targetUuid = null;
            
            for (PlayerData data : storage.getAllPlayers().values()) {
                if (data.getUsername().equalsIgnoreCase(targetUsername)) {
                    playerData = data;
                    targetUuid = data.getUuid();
                    break;
                }
            }
            
            if (playerData == null) {
                player.sendMessage(Message.raw("Player '" + targetUsername + "' not found!"));
                return;
            }
        } else {
            playerData = storage.getPlayerData(player.getUuid());
            targetUuid = player.getUuid();
            
            if (playerData == null) {
                player.sendMessage(Message.raw("Error: Your player data not found!"));
                return;
            }
        }
        
        Player playerEntity = context.senderAs(Player.class);
        playerEntity.getPageManager().openCustomPage(ref, store, new ProfilePage(player, targetUuid));
    }
}
