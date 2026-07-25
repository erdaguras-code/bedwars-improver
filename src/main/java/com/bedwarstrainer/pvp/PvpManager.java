package com.bedwarstrainer.pvp;

import com.bedwarstrainer.module.TrainingModule;
import com.bedwarstrainer.pvp.fake.FakePlayerBot;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * PvP oturumlarini yonetir: oyuncu basina tek SAHTE OYUNCU botu.
 * Sen botu yendikce seviye artar; her tick BotController ile surulur.
 */
public final class PvpManager {
    private PvpManager() {}

    private static final Map<UUID, Session> SESSIONS = new HashMap<>();

    private static class Session {
        int level;
        BotController controller;
        Session(int level) { this.level = level; }
    }

    public static void init() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            for (Map.Entry<UUID, Session> e : SESSIONS.entrySet()) {
                Session s = e.getValue();
                if (s.controller == null) continue;

                // Bot oldu mu? -> seviye atlat, yenisini dogur
                if (s.controller.isDead()) {
                    ServerPlayerEntity player = server.getPlayerManager().getPlayer(e.getKey());
                    s.controller = null;
                    s.level++;
                    if (player != null) {
                        player.sendMessage(Text.literal("Bot yenildi! Yeni seviye: " + s.level), false);
                        spawnFor(player, s);
                    }
                } else {
                    // Botu sur
                    s.controller.tick();
                }
            }
        });
    }

    public static void start(ServerPlayerEntity player, int startLevel) {
        Session s = SESSIONS.computeIfAbsent(player.getUuid(), k -> new Session(startLevel));
        s.level = startLevel;
        spawnFor(player, s);
        player.sendMessage(Text.literal("PvP basladi. Seviye " + s.level + ". Botu yen, seviye artsin!"), false);
    }

    public static void stop(ServerPlayerEntity player) {
        Session s = SESSIONS.remove(player.getUuid());
        if (s != null && s.controller != null) {
            FakePlayerBot.remove(s.controller.getBot());
        }
        player.sendMessage(Text.literal("PvP durduruldu."), false);
    }

    private static void spawnFor(ServerPlayerEntity player, Session s) {
        ServerWorld world = player.getServerWorld();
        BlockPos arena = TrainingModule.PVP.arenaSpawn;
        BlockPos p = arena.add(0, 0, -8); // botu oyuncunun birkac blok karsisina koy
        ServerPlayerEntity bot = FakePlayerBot.spawn(
                world.getServer(), world,
                p.getX() + 0.5, p.getY(), p.getZ() + 0.5, 0.0f,
                "TrainerBot_Lv" + s.level);
        s.controller = new BotController(bot, new BotLevel(s.level));
    }
}
