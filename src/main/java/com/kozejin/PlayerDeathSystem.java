package com.kozejin;

import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.entity.damage.DeathComponent;
import com.hypixel.hytale.server.core.modules.entity.damage.DeathSystems;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;
import java.util.UUID;

public class PlayerDeathSystem extends DeathSystems.OnDeathSystem {
    
    @Nonnull
    @Override
    public Query<EntityStore> getQuery() {
        return Query.and(Player.getComponentType());
    }
    
    @Override
    public void onComponentAdded(
            @Nonnull Ref ref,
            @Nonnull DeathComponent component,
            @Nonnull Store store,
            @Nonnull CommandBuffer commandBuffer) {
        
        Player playerComponent = (Player) store.getComponent(ref, Player.getComponentType());
        
        if (playerComponent != null) {
            String playerName = playerComponent.getDisplayName();
            System.out.println("[Discord Integration] Player died: " + playerName);
            
            String cause = "";
            try {
                Message deathMessage = component.getDeathMessage();
                if (deathMessage != null) {
                    String fullMessage = deathMessage.getAnsiMessage();
                    System.out.println("[Discord Integration] Full death message: " + fullMessage);
                    
                    if (fullMessage.contains("You were killed by")) {
                        cause = fullMessage.replace("You were killed by ", "").trim();
                    } else if (fullMessage.contains("You were")) {
                        cause = fullMessage.replace("You were ", "").trim();
                    } else if (fullMessage.contains("You died")) {
                        cause = fullMessage.replace("You died", "").trim();
                    } else {
                        cause = fullMessage.trim();
                    }
                    
                    cause = cause.replaceAll("\\u00a7[0-9a-fA-Fk-oK-O]", "").trim();
                    
                    System.out.println("[Discord Integration] Extracted cause: " + cause);
                }
            } catch (Exception e) {
                System.out.println("[Discord Integration] Error extracting death message: " + e.getMessage());
            }
            
            DiscordIntegration plugin = DiscordIntegration.getInstance();
            if (plugin != null) {
                plugin.notifyPlayerDeath(playerName, cause);
                
                UUID playerUuid = playerComponent.getUuid();
                if (playerUuid != null) {
                    PlayerData data = plugin.getPlayerDataStorage().getPlayerData(playerUuid);
                    if (data != null) {
                        data.incrementDeaths();
                        plugin.getPlayerDataStorage().saveAllPlayers();
                    }
                }
            }
        }
    }
}
