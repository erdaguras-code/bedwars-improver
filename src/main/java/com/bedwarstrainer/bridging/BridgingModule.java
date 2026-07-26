package com.bedwarstrainer.bridging;

import com.bedwarstrainer.lobby.LobbyBuilder;
import com.bedwarstrainer.module.ModuleEntry;
import com.bedwarstrainer.module.TrainingModule;
import com.bedwarstrainer.util.TeleportUtil;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.block.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/**
 * Kopru atma (bridging) antrenmani.
 *
 * Akis:
 *   1) Arenaya girince oyuncuya yun verilir, baslangic platformuna konur.
 *   2) Baslangic platformundan cikinca kronometre baslar.
 *   3) Hedef platforma ayak basinca sure, blok sayisi ve blok/saniye yazilir.
 *   4) Bosluga dusunce OLMEDEN baslangica geri alinir, deneme sayaci artar.
 *
 * Antrenmanin ise yaramasi icin kritik olan sey hizli tekrar dongusudur:
 * dusunce oyuncu beklemez, aninda yeniden dener.
 */
public final class BridgingModule {
    private BridgingModule() {}

    /** Kopru malzemesi. */
    private static final Item BLOCK_ITEM = Items.WHITE_WOOL;
    /** Oyuncuya verilen yun (2 deste). */
    private static final int GIVEN_BLOCKS = 128;
    /** Bu yuksekligin altina dusen oyuncu basa alinir (arena zemini y=99). */
    private static final double FALL_Y = 94.0;
    /** Oyuncu arenadan bu kadar uzaklasirsa oturum kapanir. */
    private static final double ABANDON_DISTANCE = 60.0;
    /** Tur basinda temizlenen bolge (bosluk boyunca): yatayda ve dikeyde kac blok. */
    private static final int CLEAR_RADIUS_X = 10;
    private static final int CLEAR_DOWN = 8;
    private static final int CLEAR_UP = 8;

    private static final Map<UUID, Session> SESSIONS = new HashMap<>();

    private enum Phase { WAITING, RUNNING }

    private static class Session {
        Phase phase = Phase.WAITING;
        long startTick;
        int attempts;
        int completions;
        long bestTicks = Long.MAX_VALUE;
        int blocksAtStart;
    }

    public static void register() {
        // Arenaya her giris (NPC'ye tiklama, /trainer goto bridging) oturumu baslatir.
        ModuleEntry.register(TrainingModule.BRIDGING, BridgingModule::begin);

        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (SESSIONS.isEmpty()) return;
            // Iterator ile geziyoruz: tickSession oturumu sonlandirabilir,
            // dogrudan SESSIONS.remove cagirmak ConcurrentModificationException atardi.
            Iterator<Map.Entry<UUID, Session>> it = SESSIONS.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<UUID, Session> e = it.next();
                ServerPlayerEntity player = server.getPlayerManager().getPlayer(e.getKey());
                if (player == null) {
                    it.remove(); // oyuncu cikmis
                    continue;
                }
                if (!tickSession(player, e.getValue(), server.getTicks())) {
                    it.remove();
                }
            }
        });

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
            // Brigadier ayni isimli literal'i mevcut /trainer agacina birlestirir,
            // bu yuzden TrainerCommands dosyasina dokunmaya gerek yok.
            dispatcher.register(CommandManager.literal("trainer")
                .then(CommandManager.literal("bridging")
                    .then(CommandManager.literal("start").executes(ctx -> {
                        ServerPlayerEntity player = ctx.getSource().getPlayerOrThrow();
                        LobbyBuilder.sendToModule(player, TrainingModule.BRIDGING);
                        return 1;
                    }))
                    .then(CommandManager.literal("stop").executes(ctx -> {
                        ServerPlayerEntity player = ctx.getSource().getPlayerOrThrow();
                        SESSIONS.remove(player.getUuid());
                        player.sendMessage(Text.literal("Kopru antrenmani kapatildi."), false);
                        return 1;
                    }))
                    .then(CommandManager.literal("stats").executes(ctx -> {
                        ServerPlayerEntity player = ctx.getSource().getPlayerOrThrow();
                        sendStats(player, SESSIONS.get(player.getUuid()));
                        return 1;
                    }))
                )
            )
        );
    }

    /** Arenaya girildi: malzemeyi ver, kronometreyi sifirla, basa al. */
    private static void begin(ServerPlayerEntity player) {
        Session s = SESSIONS.computeIfAbsent(player.getUuid(), k -> new Session());
        resetToStart(player, s, false);

        player.sendMessage(Text.literal("Kopru antrenmani").formatted(Formatting.AQUA, Formatting.BOLD), false);
        player.sendMessage(Text.literal("Hedef: karsidaki yesil platforma kopru at. "
                + "Dusersen otomatik basa donersin, sure sifirlanir."), false);
        player.sendMessage(Text.literal("Kronometre platformdan cikinca baslar. "
                + "Cikmak icin: /trainer bridging stop").formatted(Formatting.GRAY), false);
    }

    /** @return oturum devam ediyorsa true; false ise cagiran kaldirir. */
    private static boolean tickSession(ServerPlayerEntity player, Session s, int serverTicks) {
        BlockPos start = TrainingModule.BRIDGING.arenaSpawn;

        // Arenadan uzaklastiysa (lobiye donduyse) oturumu kapat
        if (player.squaredDistanceTo(start.getX() + 0.5, start.getY(), start.getZ() + 0.5)
                > ABANDON_DISTANCE * ABANDON_DISTANCE) {
            return false;
        }

        // Dustu mu? -> olmeden basa al
        if (player.getY() < FALL_Y) {
            s.attempts++;
            resetToStart(player, s, true);
            return true;
        }

        double z = player.getZ();
        double startEdgeZ = start.getZ() + LobbyBuilder.BRIDGE_PLATFORM_RADIUS + 0.5;

        if (s.phase == Phase.WAITING) {
            // Baslangic platformunun kenarini gecince kronometre baslar
            if (z > startEdgeZ) {
                s.phase = Phase.RUNNING;
                s.startTick = serverTicks;
                s.blocksAtStart = countBlocks(player);
            }
            return true;
        }

        // RUNNING: hedefe vardi mi?
        if (isOnTarget(player, start)) {
            finish(player, s, serverTicks);
            return true;
        }

        // Canli sure gostergesi (action bar)
        long elapsed = serverTicks - s.startTick;
        player.sendMessage(Text.literal(String.format("%.1f sn", elapsed / 20.0))
                .formatted(Formatting.YELLOW), true);
        return true;
    }

    private static boolean isOnTarget(ServerPlayerEntity player, BlockPos start) {
        int r = LobbyBuilder.BRIDGE_PLATFORM_RADIUS;
        double targetZ = start.getZ() + LobbyBuilder.BRIDGE_GAP;
        return Math.abs(player.getX() - (start.getX() + 0.5)) <= r + 0.5
                && Math.abs(player.getZ() - (targetZ + 0.5)) <= r + 0.5
                && player.getY() >= start.getY() - 1;
    }

    private static void finish(ServerPlayerEntity player, Session s, int serverTicks) {
        long ticks = serverTicks - s.startTick;
        double seconds = ticks / 20.0;
        int blocksUsed = Math.max(0, s.blocksAtStart - countBlocks(player));
        double perSecond = seconds > 0.01 ? blocksUsed / seconds : 0.0;

        s.completions++;
        s.attempts++;

        boolean record = ticks < s.bestTicks;
        if (record) s.bestTicks = ticks;

        player.sendMessage(Text.literal(String.format(
                "Tamamlandi! %.2f sn | %d blok | %.1f blok/sn",
                seconds, blocksUsed, perSecond)).formatted(Formatting.GREEN), false);

        if (record) {
            player.sendMessage(Text.literal("Yeni rekor!").formatted(Formatting.GOLD, Formatting.BOLD), false);
        } else {
            player.sendMessage(Text.literal(String.format("En iyi derecen: %.2f sn", s.bestTicks / 20.0))
                    .formatted(Formatting.GRAY), false);
        }

        // Hemen yeni tur
        resetToStart(player, s, false);
    }

    /** Oyuncuyu baslangica al, malzemeyi tazele, kronometreyi sifirla. */
    private static void resetToStart(ServerPlayerEntity player, Session s, boolean announceFall) {
        BlockPos start = TrainingModule.BRIDGING.arenaSpawn;

        s.phase = Phase.WAITING;
        player.fallDistance = 0.0f;
        player.setVelocity(0.0, 0.0, 0.0);
        player.velocityModified = true;

        // Onceki turda atilan kopruyu sil; yoksa ikinci deneme hazir kopruden
        // yurumeye donusur ve antrenman anlamini kaybeder.
        clearBridgeArea(player.getServerWorld(), start);

        TeleportUtil.teleport(player,
                start.getX() + 0.5, start.getY(), start.getZ() + 0.5,
                TrainingModule.BRIDGING.arenaYaw, 0.0f);

        giveBlocks(player);

        if (announceFall) {
            player.sendMessage(Text.literal("Dustun. Deneme " + (s.attempts + 1) + " - tekrar!")
                    .formatted(Formatting.RED), true);
        }
    }

    /**
     * Iki platform arasindaki bosluktaki her seyi havaya cevirir.
     * Platformlarin kendisine dokunmaz (z araligi kenarlarin icinde kalir).
     */
    private static void clearBridgeArea(ServerWorld world, BlockPos start) {
        int r = LobbyBuilder.BRIDGE_PLATFORM_RADIUS;
        int zFrom = start.getZ() + r + 1;
        int zTo = start.getZ() + LobbyBuilder.BRIDGE_GAP - r - 1;

        BlockPos.Mutable pos = new BlockPos.Mutable();
        for (int x = start.getX() - CLEAR_RADIUS_X; x <= start.getX() + CLEAR_RADIUS_X; x++) {
            for (int z = zFrom; z <= zTo; z++) {
                for (int y = start.getY() - CLEAR_DOWN; y <= start.getY() + CLEAR_UP; y++) {
                    pos.set(x, y, z);
                    if (!world.getBlockState(pos).isAir()) {
                        world.setBlockState(pos, Blocks.AIR.getDefaultState());
                    }
                }
            }
        }
    }

    /** Envanteri temizleyip taze yun verir; boylece blok sayimi hep dogru olur. */
    private static void giveBlocks(ServerPlayerEntity player) {
        player.getInventory().clear();
        int remaining = GIVEN_BLOCKS;
        while (remaining > 0) {
            int n = Math.min(64, remaining);
            player.getInventory().insertStack(new ItemStack(BLOCK_ITEM, n));
            remaining -= n;
        }
        player.getInventory().selectedSlot = 0;
        player.currentScreenHandler.sendContentUpdates();
    }

    private static int countBlocks(ServerPlayerEntity player) {
        int total = 0;
        for (ItemStack stack : player.getInventory().main) {
            if (stack.isOf(BLOCK_ITEM)) total += stack.getCount();
        }
        return total;
    }

    private static void sendStats(ServerPlayerEntity player, Session s) {
        if (s == null) {
            player.sendMessage(Text.literal("Henuz kopru denemen yok."), false);
            return;
        }
        player.sendMessage(Text.literal(String.format(
                "Deneme: %d | Tamamlanan: %d | En iyi: %s",
                s.attempts, s.completions,
                s.bestTicks == Long.MAX_VALUE ? "-" : String.format("%.2f sn", s.bestTicks / 20.0)
        )).formatted(Formatting.AQUA), false);
    }
}
