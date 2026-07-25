package com.bedwarstrainer.pvp.fake;

import com.mojang.authlib.GameProfile;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.NetworkSide;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ConnectedClientData;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Uuids;
import net.minecraft.world.GameMode;

import java.util.UUID;

/**
 * Bir "sahte oyuncu" botu olusturur: GERCEK bir ServerPlayerEntity.
 * Zombiden farkli olarak sprint atabilir, oyuncu skini gorunur ve
 * saldirilari gercek oyuncu saldiri kodundan gecer (otantik 1.8 knockback).
 *
 * Carpet'in EntityPlayerMPFake.createFake desenini Yarn 1.21.1'e uyarladik.
 *
 * ⚠️ CI-DOGRULA (en olasi hata noktalari):
 *   - ServerPlayerEntity kurucu imzasi: (server, world, profile, SyncedClientOptions)
 *   - SyncedClientOptions.createDefault()  (ClientInformation karsiligi)
 *   - PlayerManager.onPlayerConnect(ClientConnection, ServerPlayerEntity, ConnectedClientData)
 *   - ConnectedClientData kurucu imzasi
 *   Bunlarin adlari surumle degisebilir; CI ciktisina gore duzeltilecek.
 */
public final class FakePlayerBot {
    private FakePlayerBot() {}

    public static ServerPlayerEntity spawn(MinecraftServer server, ServerWorld world,
                                           double x, double y, double z, float yaw,
                                           String name) {
        GameProfile profile = new GameProfile(Uuids.getOfflinePlayerUuid(name), name);

        // Varsayilan istemci ayarlarini (skin katmanlari vb.) buradan aliyoruz;
        // boylece SyncedClientOptions sinifini adiyla anmamiza gerek kalmiyor.
        ConnectedClientData clientData = ConnectedClientData.createDefault(profile, false);

        ServerPlayerEntity bot = new ServerPlayerEntity(server, world, profile, clientData.syncedOptions());

        FakeClientConnection connection = new FakeClientConnection(NetworkSide.SERVERBOUND);
        server.getPlayerManager().onPlayerConnect(connection, bot, clientData);

        bot.networkHandler.requestTeleport(x, y, z, yaw, 0.0f);
        bot.setHealth(20.0f);
        bot.changeGameMode(GameMode.SURVIVAL);

        // Gorunum + silah
        bot.equipStack(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_SWORD));

        return bot;
    }

    public static void remove(ServerPlayerEntity bot) {
        if (bot != null) {
            bot.networkHandler.onDisconnected(
                    new net.minecraft.network.DisconnectionInfo(
                            net.minecraft.text.Text.literal("bot kaldirildi")));
        }
    }
}
