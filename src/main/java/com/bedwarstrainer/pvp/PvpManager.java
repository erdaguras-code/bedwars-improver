package com.bedwarstrainer.pvp;

import com.bedwarstrainer.BedwarsTrainerMod;
import com.bedwarstrainer.module.ModuleEntry;
import com.bedwarstrainer.module.TrainingModule;
import com.bedwarstrainer.pvp.fake.FakePlayerBot;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * PvP oturumlarini yonetir: oyuncu basina tek SAHTE OYUNCU botu.
 * Sen botu yendikce seviye artar; her tick BotController ile surulur.
 *
 * Oturum su yollarla baslar:
 *   - PvP arenasina girmek (NPC'ye tiklama / /trainer goto pvp)  -> ensureStarted
 *   - /trainer pvp start [seviye]                                -> start (zorla yeniden baslatir)
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
        // Arenaya giren oyuncu icin oturumu otomatik baslat.
        ModuleEntry.register(TrainingModule.PVP, PvpManager::ensureStarted);

        ServerTickEvents.END_SERVER_TICK.register(server -> {
            for (Map.Entry<UUID, Session> e : SESSIONS.entrySet()) {
                Session s = e.getValue();
                if (s.controller == null) continue;

                // Bot oldu mu? -> seviye atlat, yenisini dogur
                if (s.controller.isDead()) {
                    ServerPlayerEntity player = server.getPlayerManager().getPlayer(e.getKey());
                    // Olen botun cesedi dunyada kaliyordu; yeni bot onun icine
                    // doguyor ve vuruslar cesede gidiyordu. Once temizle.
                    FakePlayerBot.remove(s.controller.getBot());
                    s.controller = null;
                    s.level++;
                    if (player != null) {
                        player.sendMessage(Text.literal("Bot yenildi! Yeni seviye: " + s.level)
                                .formatted(Formatting.GREEN), false);
                        spawnFor(player, s);
                    }
                } else {
                    // Botu sur
                    s.controller.tick();
                }
            }
        });
    }

    /** Arenaya girildiginde: canli bot yoksa oturumu baslat, varsa dokunma. */
    public static void ensureStarted(ServerPlayerEntity player) {
        Session s = SESSIONS.get(player.getUuid());
        if (s != null && s.controller != null && !s.controller.isDead()) {
            return; // zaten devam eden bir dovus var
        }
        start(player, s == null ? 1 : s.level);
    }

    public static void start(ServerPlayerEntity player, int startLevel) {
        Session s = SESSIONS.computeIfAbsent(player.getUuid(), k -> new Session(startLevel));
        s.level = startLevel;
        despawnBot(s);
        giveKit(player);
        spawnFor(player, s);
        player.sendMessage(Text.literal("PvP basladi. Seviye " + s.level + ". Botu yen, seviye artsin!"), false);
    }

    public static void stop(ServerPlayerEntity player) {
        Session s = SESSIONS.remove(player.getUuid());
        if (s != null) {
            despawnBot(s);
        }
        player.sendMessage(Text.literal("PvP durduruldu."), false);
    }

    private static void despawnBot(Session s) {
        if (s.controller != null) {
            FakePlayerBot.remove(s.controller.getBot());
            s.controller = null;
        }
    }

    /** Dovuse girmeden once oyuncuya standart ekipman ver. */
    private static void giveKit(ServerPlayerEntity player) {
        player.getInventory().clear();
        player.equipStack(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_SWORD));
        player.equipStack(EquipmentSlot.HEAD, new ItemStack(Items.LEATHER_HELMET));
        player.equipStack(EquipmentSlot.CHEST, new ItemStack(Items.LEATHER_CHESTPLATE));
        player.equipStack(EquipmentSlot.LEGS, new ItemStack(Items.LEATHER_LEGGINGS));
        player.equipStack(EquipmentSlot.FEET, new ItemStack(Items.LEATHER_BOOTS));
        player.setHealth(player.getMaxHealth());
        player.getHungerManager().setFoodLevel(20);
        player.extinguish();
    }

    /**
     * Botun kilici ve zirhi seviyeyle guclenir. Bot da senin gibi ekipmanli
     * dovussun ki knockback ve hasar hesabi gercekci olsun.
     */
    private static void equipBot(ServerPlayerEntity bot, BotLevel level) {
        bot.equipStack(EquipmentSlot.MAINHAND, new ItemStack(
                level.level >= 6 ? Items.DIAMOND_SWORD
                        : level.level >= 3 ? Items.IRON_SWORD
                        : Items.STONE_SWORD));

        if (level.level >= 6) {
            bot.equipStack(EquipmentSlot.HEAD, new ItemStack(Items.IRON_HELMET));
            bot.equipStack(EquipmentSlot.CHEST, new ItemStack(Items.IRON_CHESTPLATE));
            bot.equipStack(EquipmentSlot.LEGS, new ItemStack(Items.IRON_LEGGINGS));
            bot.equipStack(EquipmentSlot.FEET, new ItemStack(Items.IRON_BOOTS));
        } else if (level.level >= 4) {
            bot.equipStack(EquipmentSlot.HEAD, new ItemStack(Items.CHAINMAIL_HELMET));
            bot.equipStack(EquipmentSlot.CHEST, new ItemStack(Items.CHAINMAIL_CHESTPLATE));
            bot.equipStack(EquipmentSlot.LEGS, new ItemStack(Items.CHAINMAIL_LEGGINGS));
            bot.equipStack(EquipmentSlot.FEET, new ItemStack(Items.CHAINMAIL_BOOTS));
        } else if (level.level >= 2) {
            bot.equipStack(EquipmentSlot.HEAD, new ItemStack(Items.LEATHER_HELMET));
            bot.equipStack(EquipmentSlot.CHEST, new ItemStack(Items.LEATHER_CHESTPLATE));
            bot.equipStack(EquipmentSlot.LEGS, new ItemStack(Items.LEATHER_LEGGINGS));
            bot.equipStack(EquipmentSlot.FEET, new ItemStack(Items.LEATHER_BOOTS));
        }
    }

    private static void spawnFor(ServerPlayerEntity player, Session s) {
        ServerWorld world = player.getServerWorld();
        BlockPos arena = TrainingModule.PVP.arenaSpawn;
        BlockPos p = arena.add(0, 0, -8); // botu oyuncunun birkac blok karsisina koy
        try {
            ServerPlayerEntity bot = FakePlayerBot.spawn(
                    world.getServer(), world,
                    p.getX() + 0.5, p.getY(), p.getZ() + 0.5, 0.0f,
                    "TrainerBot_Lv" + s.level);
            BotLevel botLevel = new BotLevel(s.level);
            equipBot(bot, botLevel);
            s.controller = new BotController(bot, botLevel, player.getUuid());
        } catch (Throwable t) {
            // Sahte oyuncu olusturmak surume duyarli bir is; sessizce yutma,
            // hatayi hem log'a hem oyuncunun ekranina yaz.
            s.controller = null;
            BedwarsTrainerMod.LOGGER.error("[Bedwars Trainer] Bot olusturulamadi", t);
            player.sendMessage(Text.literal("Bot olusturulamadi: " + t)
                    .formatted(Formatting.RED), false);
        }
    }
}
