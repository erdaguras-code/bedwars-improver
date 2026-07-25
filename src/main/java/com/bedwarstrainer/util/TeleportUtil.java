package com.bedwarstrainer.util;

import net.minecraft.server.network.ServerPlayerEntity;

/**
 * Isinlanma cagrisini tek bir yerde topluyoruz ki, Minecraft surumleri
 * arasinda imza degisirse sadece burayi duzeltmen yeterli olsun.
 *
 * networkHandler.requestTeleport(...) ayni dunya icinde isinlanma icin
 * surumler arasi en stabil yontemdir. Tum modullerimiz ayni overworld
 * dunyasinda oldugu icin bu yeterli.
 */
public final class TeleportUtil {
    private TeleportUtil() {}

    public static void teleport(ServerPlayerEntity player, double x, double y, double z, float yaw, float pitch) {
        player.networkHandler.requestTeleport(x, y, z, yaw, pitch);
    }
}
