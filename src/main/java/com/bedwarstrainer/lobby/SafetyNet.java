package com.bedwarstrainer.lobby;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.network.ServerPlayerEntity;

/**
 * Void dunyada genel emniyet agi: bosluga dusen oyuncuyu olmeden lobiye alir.
 *
 * Bu KABA bir agdir (y < 40). Modullerin kendi reset mantigi bundan cok daha
 * once devreye girmelidir - ornegin bridging arenasinda oyuncu y=95'in altina
 * dustugunde modul onu baslangica isinlamali. Bu ag sadece "hicbir modul
 * sahiplenmediyse en azindan olme" garantisi verir.
 */
public final class SafetyNet {
    private SafetyNet() {}

    /** Bu yukseklige dusen oyuncu lobiye alinir. */
    private static final double VOID_Y = 40.0;

    /** Her tick tum oyuncularin y'sine bakmaya gerek yok. */
    private static final int CHECK_INTERVAL_TICKS = 10;

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (server.getTicks() % CHECK_INTERVAL_TICKS != 0) return;

            for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
                if (player.getY() >= VOID_Y) continue;
                if (player.isSpectator() || player.isCreative()) continue;
                if (!LobbyBuilder.isBuilt(player.getServerWorld())) continue;

                player.fallDistance = 0.0f;
                player.setVelocity(0.0, 0.0, 0.0);
                LobbyBuilder.sendToLobby(player);
            }
        });
    }
}
