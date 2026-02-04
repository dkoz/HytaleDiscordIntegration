package com.kozejin;

import com.hypixel.hytale.component.Archetype;
import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.EntityEventSystem;
import com.hypixel.hytale.server.core.event.events.ecs.PlaceBlockEvent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class BlockPlaceSystem extends EntityEventSystem<EntityStore, PlaceBlockEvent> {
    public BlockPlaceSystem() {
        super(PlaceBlockEvent.class);
    }

    @Override
    public void handle(int i, @Nonnull ArchetypeChunk<EntityStore> archetypeChunk, @Nonnull Store<EntityStore> store, @Nonnull CommandBuffer<EntityStore> commandBuffer, @Nonnull PlaceBlockEvent event) {
        PlayerRef player = archetypeChunk.getComponent(i, PlayerRef.getComponentType());
        if (player == null) return;

        DiscordIntegration plugin = DiscordIntegration.getInstance();
        if (plugin != null) {
            PlayerData data = plugin.getPlayerDataStorage().getPlayerData(player.getUuid());
            if (data != null) {
                data.incrementBlocksPlaced();
            }
        }
    }

    @Nullable
    @Override
    public Query<EntityStore> getQuery() {
        return Archetype.empty();
    }
}
