package com.bedwarstrainer.module;

import net.minecraft.server.network.ServerPlayerEntity;

import java.util.EnumMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Modul arenasina GIRIS kancasi (extension point).
 *
 * Oyuncu bir modul arenasina hangi yoldan girerse girsin (NPC'ye tiklama,
 * /trainer goto, /trainer pvp start ...) LobbyBuilder.sendToModule() sonunda
 * buradaki kanca calisir. Boylece her modul kendi baslangic mantigini
 * KENDI dosyasinda kurar; InteractionHandler veya TrainerCommands'a
 * dokunmasi gerekmez.
 *
 * Kullanim (modulun kendi register metodunda):
 *   ModuleEntry.register(TrainingModule.BRIDGING, player -> BridgingSession.begin(player));
 */
public final class ModuleEntry {
    private ModuleEntry() {}

    private static final Map<TrainingModule, Consumer<ServerPlayerEntity>> HANDLERS =
            new EnumMap<>(TrainingModule.class);

    /** Bir modul icin giris kancasi kaydeder. Ayni modul icin son kayit gecerlidir. */
    public static void register(TrainingModule module, Consumer<ServerPlayerEntity> handler) {
        HANDLERS.put(module, handler);
    }

    /** LobbyBuilder.sendToModule() tarafindan cagrilir; kanca yoksa sessizce gecer. */
    public static void onEnter(TrainingModule module, ServerPlayerEntity player) {
        Consumer<ServerPlayerEntity> handler = HANDLERS.get(module);
        if (handler != null) {
            handler.accept(player);
        }
    }
}
