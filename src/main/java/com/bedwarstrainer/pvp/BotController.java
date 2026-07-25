package com.bedwarstrainer.pvp;

import net.minecraft.entity.MovementType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.Vec3d;

/**
 * Sahte oyuncu botunu her tick suren taktiksel 1.8 PvP beyni.
 *  - Sprint atarak hedefe yaklasir (sprint = 1.8 knockback bonusu)
 *  - Menzile girince spam vurur (cooldown mixin sayesinde tam hasar)
 *  - Yakinda strafe atar (yan yan hareket) - insan gibi, bodoslama degil
 *  - Vurustan hemen once sprint-reset (W-tap) yapar -> ekstra knockback
 *
 * ⚠️ CI/PLAYTEST: hareket fizigi (move + gravity + jump) elle surulur;
 *   deger/akis oynatarak ince ayar gerekir. Netcode derlenince test edip
 *   birlikte tunelariz.
 */
public class BotController {
    private final ServerPlayerEntity bot;
    private final BotLevel level;

    private int attackTick;
    private int strafeDir = 1;
    private int strafeSwitchTick;
    private double motionY;

    private static final double REACH = 3.0;

    public BotController(ServerPlayerEntity bot, BotLevel level) {
        this.bot = bot;
        this.level = level;
    }

    public ServerPlayerEntity getBot() { return bot; }
    public boolean isDead() { return bot == null || bot.isRemoved() || !bot.isAlive(); }

    public void tick() {
        if (isDead()) return;
        PlayerEntity target = bot.getWorld().getClosestPlayer(bot, level.followRange);
        if (target == null || !target.isAlive() || target.isCreative() || target.isSpectator()) {
            idle();
            return;
        }

        // Hedefe bak
        faceTarget(target);

        double distSq = bot.squaredDistanceTo(target);
        Vec3d toTarget = target.getPos().subtract(bot.getPos());
        Vec3d dir = new Vec3d(toTarget.x, 0, toTarget.z);
        double horiz = Math.sqrt(dir.x * dir.x + dir.z * dir.z);
        if (horiz > 1.0e-4) dir = dir.multiply(1.0 / horiz);

        // Sprint (1.8 knockback bonusu icin kritik)
        bot.setSprinting(true);

        // Hareket vektoru: hedefe dogru
        Vec3d move = dir.multiply(level.moveSpeed);

        // Yakinda strafe: bodoslama yerine yan yan
        if (level.strafe && distSq < 16.0) {
            if (--strafeSwitchTick <= 0) {
                strafeSwitchTick = 15 + bot.getRandom().nextInt(20);
                strafeDir = -strafeDir;
            }
            Vec3d side = new Vec3d(-dir.z, 0, dir.x).multiply(strafeDir * level.moveSpeed * 0.6);
            move = move.add(side);
        }

        applyMovement(move);

        // Menzildeyse vur
        if (distSq <= REACH * REACH) {
            if (--attackTick <= 0) {
                attackTick = level.attackIntervalTicks;
                // W-tap / sprint-reset: sprint'i kapat-ac -> ekstra knockback
                if (level.wTap) {
                    bot.setSprinting(false);
                    bot.setSprinting(true);
                }
                bot.attack(target); // GERCEK oyuncu saldirisi -> otantik 1.8 KB
                bot.swingHand(bot.getActiveHand());
            }
        }
    }

    private void faceTarget(PlayerEntity target) {
        Vec3d d = target.getEyePos().subtract(bot.getEyePos());
        double yaw = Math.toDegrees(Math.atan2(-d.x, d.z));
        double pitch = Math.toDegrees(-Math.atan2(d.y, Math.sqrt(d.x * d.x + d.z * d.z)));
        bot.setYaw((float) yaw);
        bot.setHeadYaw((float) yaw);
        bot.setPitch((float) pitch);
    }

    private void applyMovement(Vec3d horizontalMove) {
        // basit yercekimi
        if (bot.isOnGround()) {
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
