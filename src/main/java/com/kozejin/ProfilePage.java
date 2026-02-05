package com.kozejin;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;

public class ProfilePage extends InteractiveCustomUIPage<ProfilePage.Data> {
    private final UUID targetUuid;

    public ProfilePage(@Nonnull PlayerRef playerRef, UUID targetUuid) {
        super(playerRef, CustomPageLifetime.CanDismiss, Data.CODEC);
        this.targetUuid = targetUuid;
    }

    @Override
    public void build(@Nonnull Ref<EntityStore> ref, @Nonnull UICommandBuilder cb, @Nonnull UIEventBuilder event, @Nonnull Store<EntityStore> store) {
        cb.append("profile/base_page.ui");

        DiscordIntegration plugin = DiscordIntegration.getInstance();
        if (plugin == null) return;

        PlayerData data = plugin.getPlayerDataStorage().getPlayerData(targetUuid);
        if (data == null) return;

        cb.set("#Title.Text", "Player Profile: " + data.getUsername());

        SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy");
        String firstLoginDate = dateFormat.format(new Date(data.getFirstLoginTime()));
        String discordStatus = data.getDiscordId() != null ? "Linked" : "Not linked";

        addSection(cb, "Account");
        addStat(cb, "Username:", data.getUsername());
        addStat(cb, "First Login:", firstLoginDate);
        addStat(cb, "Total Playtime:", data.getFormattedPlayTime());
        addStat(cb, "Discord:", discordStatus);

        addSection(cb, "Combat");
        addStat(cb, "Player Kills:", String.valueOf(data.getPlayerKills()));
        addStat(cb, "Mob Kills:", String.valueOf(data.getMobKills()));
        addStat(cb, "Deaths:", String.valueOf(data.getTotalDeaths()));

        addSection(cb, "Building");
        addStat(cb, "Blocks Placed:", String.valueOf(data.getBlocksPlaced()));
        addStat(cb, "Blocks Broken:", String.valueOf(data.getBlocksBroken()));
    }

    @Override
    public void handleDataEvent(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store, @Nonnull Data data) {
        super.handleDataEvent(ref, store, data);
    }

    private void addStat(UICommandBuilder cb, String name, String value) {
        StringBuilder sb = new StringBuilder();
        sb.append("Group { ")
                .append("LayoutMode: Left; ")
                .append("Anchor: (Height: 32); ")
                .append("Padding: (Left: 12, Right: 30); ")
                .append("Label { ")
                .append("Text: \"").append(name).append("\"; ")
                .append("Style: (FontSize: 16, TextColor: #9aa7b4); ")
                .append("Anchor: (Width: 300, Right: 10); ")
                .append("} ")
                .append("Label { ")
                .append("Text: \"").append(value).append("\"; ")
                .append("Style: (FontSize: 16, TextColor: #e6edf5, HorizontalAlignment: End); ")
                .append("Anchor: (Width: 300); ")
                .append("} ")
                .append("}");
        cb.appendInline("#BodyRow", sb.toString());
    }

    private void addSection(UICommandBuilder cb, String title) {
        cb.appendInline("#BodyRow",
                "Label { " +
                        "Text: \"" + title + "\"; " +
                        "Style: (FontSize: 22, TextColor: #7fb2ff); " +
                        "Padding: (Bottom: 8, Left: 6, Top: 12); " +
                        "}"
        );
    }

    public static class Data {
        public static final BuilderCodec<ProfilePage.Data> CODEC = BuilderCodec.builder(ProfilePage.Data.class, ProfilePage.Data::new)
                .append(new KeyedCodec<>("Button", Codec.STRING), (data, s) -> data.value = s, data -> data.value).add()
                .build();
        public String value;
    }
}
