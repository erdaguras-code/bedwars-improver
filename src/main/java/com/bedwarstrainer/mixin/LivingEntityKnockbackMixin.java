package com.bedwarstrainer.mixin;

import com.bedwarstrainer.combat.CombatToggle;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 1.8 knockback formulunu uygular (buyuk sunuculardaki OldCombatMechanics
 * mantigi). Vanilla knockback'i iptal edip 1.8 degerleriyle yeniden hesaplar.
 * Bu ELLE per-hit hack degil; combat'in sistemsel kurali (tum vuruslar icin).
 *
 * ⚠️ CI-DOGRULA: takeKnockback(double, double, double) imzasi ve
 *   'velocityModified' alan adi Yarn 1.21.1'e gore. Hata cikarsa duzelt.
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityKnockbackMixin {

    @Inject(method = "takeKnockback", at = @At("HEAD"), cancellable = true)
    private void bwt$oldKnockback(double strength, double x, double z, CallbackInfo ci) {
        if (!CombatToggle.is18()) return;
        LivingEntity self = (LivingEntity) (Object) this;

        strength *= 1.0 - self.getAttributeValue(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE);
        if (strength <= 0.0) { ci.cancel(); return; }

        double dist = Math.sqrt(x * x + z * z);
        if (dist < 1.0e-4) { ci.cancel(); return; }

        Vec3d v = self.getVelocity();
        double nx = x / dist * strength;
        double nz = z / dist * strength;
        double newY = self.isOnGround() ? Math.min(0.4, v.y / 2.0 + strength) : v.y;

        self.setVelocity(v.x / 2.0 - nx, newY, v.z / 2.0 - nz);
        self.velocityModified = true;
        ci.cancel(); // vanilla knockback'i devre disi birak
    }
}
