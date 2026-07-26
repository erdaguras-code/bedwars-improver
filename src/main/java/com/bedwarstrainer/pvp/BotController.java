package com.bedwarstrainer.pvp;

import net.minecraft.entity.MovementType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.Vec3d;

import java.util.UUID;

/**
 * Sahte oyuncu botunu her tick suren taktiksel 1.8 PvP beyni.
 *
 * Davranis katmanlari (seviye yukseldikce acilir):
 *  - Sprint ile yaklasma (sprint = 1.8 knockback bonusu)
 *  - Mesafe yonetimi: cok yaklasinca geri cekilir, uzaksa kapatir
 *  - Strafe (yan yan hareket) - bodoslama gitmez
 *  - W-tap / sprint-reset: vurustan hemen once sprint kes-ac -> ekstra knockback
 *  - Vur-kac: vurduktan sonra birkac tick geri ceker (sprint resetler, misilleme yer)
 *  - Zipla-kritik: sprint birakip ziplar, DUSERKEN vurur -> kritik hasar
 *    (vanilla kurali: sprint aciksa kritik olmaz, o yuzden once sprint kapanir)
 *  - Dusuk seviyede nisan titremesi - insan gibi, robot gibi degil
 *
 * ⚠️ PLAYTEST: hareket fizigi (move + gravity + jump) elle surulur; sayilar
 *   oyun icinde denenerek ayarlanmali.
 */
public class BotController {
    private final ServerPlayerEntity bot;
    private final BotLevel level;
    /** Botun hedefi: oturumu baslatan oyuncu. */
    private final UUID ownerUuid;

    private int attackTick;
    private int strafeDir = 1;
    private int strafeSwitchTick;
    private double motionY;

    /** Vurduktan sonra geri cekilme sayaci. */
    private int backpedalTick;
    /** Zipla-kritik icin: ziplandi, dususe gecince vur. */
    private int critPending;

    private static final double REACH = 3.0;
    /** Bu mesafeden yakinsa bot geri ceker (dip dibe girip sikismaz). */
    private static final double TOO_CLOSE = 1.6;

    public BotController(ServerPlayerEntity bot, BotLevel level, UUID ownerUuid) {
        this.bot = bot;
        this.level = level;
        this.ownerUuid = ownerUuid;
    }

    public ServerPlayerEntity getBot() { return bot; }
    public boolean isDead() { return bot == null || bot.isRemoved() || !bot.isAlive(); }

    /**
     * Hedefi ADIYLA bulur, "en yakin oyuncu" ile DEGIL.
     *
     * Sahte oyuncu gercek bir ServerPlayerEntity oldugu icin dunyanin oyuncu
     * listesinde yer aliyor; getClosestPlayer(bot, ...) botun kendisini (mesafe 0)
     * dondurup botu kendi kendine vurdurmaya calisiyordu. Sahibi UUID ile
     * hedefleyince hem bu tuzak hem de botlarin birbirini hedeflemesi biter.
     */
    private ServerPlayerEntity findTarget() {
        MinecraftServer server = bot.getServer();
        if (server == null) return null;

        ServerPlayerEntity owner = server.getPlayerManager().getPlayer(ownerUuid);
        if (owner == null || !owner.isAlive() || owner.isCreative() || owner.isSpectator()) return null;
        if (owner.getWorld() != bot.getWorld()) return null;
        if (bot.squaredDistanceTo(owner) > level.followRange * level.followRange) return null;

        return owner;
    }

    public void tick() {
        if (isDead()) return;
        PlayerEntity target = findTarget();
        if (target == null) {
            idle();
            return;
        }

        faceTarget(target);

        double dist = Math.sqrt(bot.squaredDistanceTo(target));
        Vec3d toTarget = target.getPos().subtract(bot.getPos());
        Vec3d dir = new Vec3d(toTarget.x, 0, toTarget.z);
        double horiz = Math.sqrt(dir.x * dir.x + dir.z * dir.z);
        if (horiz > 1.0e-4) dir = dir.multiply(1.0 / horiz);

        Vec3d move = decideMovement(dir, dist);

        // Strafe: yakin mesafede yan yan sal
        if (level.strafe && dist < 4.0 && backpedalTick <= 0) {
            if (--strafeSwitchTick <= 0) {
                strafeSwitchTick = 15 + bot.getRandom().nextInt(20);
                strafeDir = -strafeDir;
            }
            Vec3d side = new Vec3d(-dir.z, 0, dir.x).multiply(strafeDir * level.moveSpeed * 0.6);
            move = move.add(side);
        }

        applyMovement(move);
        handleAttack(target, dist);
    }

    /** Mesafeye gore yaklas / tut / geri cek. */
    private Vec3d decideMovement(Vec3d dir, double dist) {
        if (backpedalTick > 0) {
            backpedalTick--;
            bot.setSprinting(false); // geri cekilirken sprint resetlenir
            return dir.multiply(-level.moveSpeed * 0.8);
        }

        if (critPending > 0) {
            // Kritik icin havadayken sprint kapali kalmali
            bot.setSprinting(false);
            return dir.multiply(level.moveSpeed * 0.5);
        }

        if (dist < TOO_CLOSE) {
            bot.setSprinting(false);
            return dir.multiply(-level.moveSpeed * 0.5);
        }

        bot.setSprinting(true);
        if (dist > REACH * 0.9) {
            return dir.multiply(level.moveSpeed);          // mesafeyi kapat
        }
        return dir.multiply(level.moveSpeed * 0.35);       // menzilde tut
    }

    private void handleAttack(PlayerEntity target, double dist) {
        // Zipla-kritik bekliyorsa: dususe gecince vur
        if (critPending > 0) {
            critPending--;
            if (motionY < 0.0 && dist <= REACH) {
                strike(target);
                critPending = 0;
            }
            return;
        }

        if (dist > REACH || --attackTick > 0) return;

        attackTick = level.attackIntervalTicks;

        // Seviye 6+: bazen zipla-kritik dene
        if (level.level >= 6 && bot.isOnGround() && bot.getRandom().nextFloat() < 0.35f) {
            bot.setSprinting(false);   // sprint acikken kritik olmaz
            motionY = 0.42;            // zipla
            critPending = 6;           // birkac tick icinde dususte vur
            return;
        }

        if (level.wTap) {
            // W-tap / sprint-reset: sprint'i kapat-ac -> ekstra knockback
            bot.setSprinting(false);
            bot.setSprinting(true);
        }
        strike(target);
    }

    private void strike(PlayerEntity target) {
        bot.attack(target);
        bot.swingHand(bot.getActiveHand());
        // Vur-kac: vurduktan sonra kisa geri cekilme (sprint resetler, karsi vurustan kacar)
        if (level.level >= 4) {
            backpedalTick = 4;
        }
    }

    private void faceTarget(PlayerEntity target) {
        Vec3d d = target.getEyePos().subtract(bot.getEyePos());
        double yaw = Math.toDegrees(Math.atan2(-d.x, d.z));
        double pitch = Math.toDegrees(-Math.atan2(d.y, Math.sqrt(d.x * d.x + d.z * d.z)));

        // Dusuk seviyede nisan tam isabetli olmasin - insan gibi dursun
        if (level.level < 4) {
            double jitter = (4 - level.level) * 1.5;
            yaw += (bot.getRandom().nextDouble() - 0.5) * jitter;
            pitch += (bot.getRandom().nextDouble() - 0.5) * jitter * 0.5;
        }

        bot.setYaw((float) yaw);
        bot.setHeadYaw((float) yaw);
        bot.setPitch((float) pitch);
    }

    private void applyMovement(Vec3d horizontalMove) {
        // basit yercekimi
        if (bot.isOnGround() && motionY <= 0.0) {
            motionY = 0;
            // engel varsa zipla
            if (bot.horizontalCollision) motionY = 0.42;
        } else {
            motionY -= 0.08;
            if (motionY < -3.0) motionY = -3.0;
        }
        Vec3d vel = new Vec3d(horizontalMove.x, motionY, horizontalMove.z);
        bot.move(MovementType.SELF, vel);
        bot.setVelocity(vel);
    }

    private void idle() {
        bot.setSprinting(false);
        applyMovement(Vec3d.ZERO);
    }
}
