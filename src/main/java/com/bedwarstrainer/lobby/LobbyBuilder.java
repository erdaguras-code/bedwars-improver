package com.bedwarstrainer.lobby;

import com.bedwarstrainer.module.TrainingModule;
import com.bedwarstrainer.util.TeleportUtil;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

/**
 * Lobiyi ve (simdilik iskelet) modul arenalarini blok blok insa eder.
 * Tum yapilar y=99 zemininde durur; oyuncu y=100'de yurur.
 */
public final class LobbyBuilder {
    private LobbyBuilder() {}

    public static final BlockPos LOBBY_ORIGIN = new BlockPos(0, 99, 0);
    public static final BlockPos LOBBY_SPAWN  = new BlockPos(0, 100, 0);
    /** "Lobi kuruldu mu?" isaretcisi: origin'in 2 alti bir beacon. */
    private static final BlockPos BUILT_MARKER = new BlockPos(0, 97, 0);

    /** Zemin void mu (yani bizim antrenman dunyamiz mi)? Spawn cevresine bakar. */
    public static boolean looksLikeTrainerWorld(ServerWorld world) {
        BlockPos spawn = world.getSpawnPos();
        // Spawn'in altindaki 5 blok tamamen hava ise void dunya kabul ediyoruz.
        for (int dy = 0; dy < 5; dy++) {
            if (!world.getBlockState(spawn.down(dy)).isAir()) {
                return false;
            }
        }
        return true;
    }

    public static boolean isBuilt(ServerWorld world) {
        return world.getBlockState(BUILT_MARKER).isOf(Blocks.BEACON);
    }

    public static void buildLobby(ServerWorld world) {
        // 1) Lobi zemini: 17x17 quartz platform
        fillFloor(world, LOBBY_ORIGIN, 8, Blocks.QUARTZ_BLOCK);

        // 2) Kenar cizgisi (sea lantern) - gorsel cerceve
        drawBorder(world, LOBBY_ORIGIN, 8, Blocks.SEA_LANTERN);

        // 3) Isaretci beacon
        world.setBlockState(BUILT_MARKER, Blocks.BEACON.getDefaultState());

        // 4) Her modul icin NPC (armor stand) + altina renkli yun
        for (TrainingModule module : TrainingModule.values()) {
            spawnModuleNpc(world, module);
            // NPC'nin durdugu bloku belirginlestir
            world.setBlockState(module.npcPos.down(), Blocks.SMOOTH_QUARTZ.getDefaultState());
            // Iskelet arenayi kur
            buildModuleArenaStub(world, module);
        }
    }

    private static void spawnModuleNpc(ServerWorld world, TrainingModule module) {
        ArmorStandEntity stand = new ArmorStandEntity(EntityType.ARMOR_STAND, world);
        stand.refreshPositionAndAngles(
                module.npcPos.getX() + 0.5,
                module.npcPos.getY(),
                module.npcPos.getZ() + 0.5,
                180.0f, 0.0f);
        stand.setCustomName(Text.translatable(module.translationKey));
        stand.setCustomNameVisible(true);
        stand.setInvulnerable(true);
        stand.setNoGravity(true);
        stand.addCommandTag(module.tag());
        world.spawnEntity(stand);
    }

    /** Iskelet arena: simdilik sadece ustunde durabilecegin bir platform + tabela. */
    private static void buildModuleArenaStub(ServerWorld world, TrainingModule module) {
        BlockPos base = module.arenaSpawn.down(); // zemin blogu
        if (module == TrainingModule.BRIDGING) {
            // Basit bridging: baslangic platformu, bosluk, hedef platform
            fillFloor(world, base, 3, Blocks.SMOOTH_STONE);            // baslangic
            fillFloor(world, base.add(0, 0, 25), 3, Blocks.EMERALD_BLOCK); // hedef (25 blok ileride)
        } else if (module == TrainingModule.PVP) {
            fillFloor(world, base, 12, Blocks.RED_SANDSTONE);          // 25x25 arena
            drawBorder(world, base, 12, Blocks.SMOOTH_RED_SANDSTONE);
        } else {
            fillFloor(world, base, 4, Blocks.LIGHT_GRAY_CONCRETE);
        }
    }

    // ---- yardimci insa fonksiyonlari ----

    private static void fillFloor(ServerWorld world, BlockPos center, int radius, Block block) {
        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                world.setBlockState(center.add(x, 0, z), block.getDefaultState());
            }
        }
    }

    private static void drawBorder(ServerWorld world, BlockPos center, int radius, Block block) {
        for (int i = -radius; i <= radius; i++) {
            world.setBlockState(center.add(i, 0, -radius), block.getDefaultState());
            world.setBlockState(center.add(i, 0, radius), block.getDefaultState());
            world.setBlockState(center.add(-radius, 0, i), block.getDefaultState());
            world.setBlockState(center.add(radius, 0, i), block.getDefaultState());
        }
    }

    // ---- isinlanma yardimcilari ----

    public static void sendToLobby(ServerPlayerEntity player) {
        TeleportUtil.teleport(player,
                LOBBY_SPAWN.getX() + 0.5, LOBBY_SPAWN.getY(), LOBBY_SPAWN.getZ() + 0.5,
                0.0f, 0.0f);
        player.sendMessage(Text.translatable("bedwarstrainer.lobby.welcome"), false);
    }

    public static void sendToModule(ServerPlayerEntity player, TrainingModule module) {
        TeleportUtil.teleport(player,
                module.arenaSpawn.getX() + 0.5, module.arenaSpawn.getY(), module.arenaSpawn.getZ() + 0.5,
                module.arenaYaw, 0.0f);
        player.sendMessage(Text.translatable(module.translationKey), true); // action bar
    }
}
