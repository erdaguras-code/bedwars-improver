package com.bedwarstrainer;

import com.bedwarstrainer.command.TrainerCommands;
import com.bedwarstrainer.event.InteractionHandler;
import com.bedwarstrainer.lobby.LobbyBuilder;
import com.bedwarstrainer.pvp.PvpManager;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Bedwars Trainer - ana giris noktasi.
 *
 * Akis:
 *  1) Oyuncu "Bedwars Antrenman" dunya tipini secip dunya olusturur (bkz. datapack world_preset).
 *     -> Bos (void) bir dunya olusur.
 *  2) Sunucu baslarken void dunya tespit edilir ve lobi insa edilir (LobbyBuilder).
 *  3) Oyuncu katildiginda lobiye isinlanir.
 *  4) Lobideki NPC'lere sag tiklayarak egitim modullerine gecer (InteractionHandler).
 *
 * Test icin: herhangi bir dunyada "/trainer lobby" komutu lobiyi kurup seni oraya isinlar.
 */
public class BedwarsTrainerMod implements ModInitializer {
    public static final String MOD_ID = "bedwarstrainer";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        LOGGER.info("[Bedwars Trainer] baslatiliyor...");

        // Komutlari kaydet
        TrainerCommands.register();

        // NPC'lere tiklama olaylarini dinle
        InteractionHandler.register();

        // PvP bot oturum yoneticisi (Modul 2)
        PvpManager.init();

        // Sunucu basladiginda: void dunya ise lobiyi kur
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            ServerWorld overworld = server.getOverworld();
            if (LobbyBuilder.looksLikeTrainerWorld(overworld) && !LobbyBuilder.isBuilt(overworld)) {
                LOGGER.info("[Bedwars Trainer] Void dunya tespit edildi, lobi insa ediliyor.");
                LobbyBuilder.buildLobby(overworld);
            }
        });

        // Oyuncu katildiginda: lobi kuruluysa oraya isinla
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayerEntity player = handler.player;
            ServerWorld world = player.getServerWorld();
            if (LobbyBuilder.isBuilt(world)) {
                LobbyBuilder.sendToLobby(player);
            }
        });

        LOGGER.info("[Bedwars Trainer] hazir.");
    }
}
