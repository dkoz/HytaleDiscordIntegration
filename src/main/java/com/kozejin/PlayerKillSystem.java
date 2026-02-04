package com.kozejin;

import com.hypixel.hytale.component.Archetype;
import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.EntityEventSystem;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatValue;
import com.hypixel.hytale.server.core.modules.entitystats.asset.DefaultEntityStatTypes;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class PlayerKillSystem extends EntityEventSystem<EntityStore, Damage> {
    public PlayerKillSystem() {
        super(Damage.class);
    }

    @Override
    public void handle(int i, @Nonnull ArchetypeChunk<EntityStore> archetypeChunk, @Nonnull Store<EntityStore> store, @Nonnull CommandBuffer<EntityStore> commandBuffer, @Nonnull Damage damage) {
        EntityStatMap entityStatMapComponent = archetypeChunk.getComponent(i, EntityStatMap.getComponentType());
        if (entityStatMapComponent == null) return;
        
        EntityStatValue health = entityStatMapComponent.get(DefaultEntityStatTypes.getHealth());
        if (health == null) return;
        
        boolean died = health.get() <= 0;
        if (!died) return;

        Player victim = archetypeChunk.getComponent(i, Player.getComponentType());
        if (victim == null) return;

        if (damage.getSource() instanceof Damage.EntitySource entitySource) {
            Ref<EntityStore> sourceRef = entitySource.getRef();
            if (!sourceRef.isValid()) return;

            Player killer = store.getComponent(sourceRef, Player.getComponentType());
            if (killer == null) return;

            DiscordIntegration plugin = DiscordIntegration.getInstance();
            if (plugin != null) {
                PlayerRef killerRef = store.getComponent(sourceRef, PlayerRef.getComponentType());
                if (killerRef != null) {
                    PlayerData killerData = plugin.getPlayerDataStorage().getPlayerData(killerRef.getUuid());
                    if (killerData != null) {
                        killerData.incrementPlayerKills();
                    }
                }
            }
        }
    }

    @Nullable
    @Override
    public Query<EntityStore> getQuery() {
        return Archetype.empty();
    }
}
