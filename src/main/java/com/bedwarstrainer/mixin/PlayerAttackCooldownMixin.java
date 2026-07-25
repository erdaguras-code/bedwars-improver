package com.bedwarstrainer.mixin;

import com.bedwarstrainer.combat.CombatToggle;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 1.8 combat: saldiri sarj gostergesini her zaman dolu (1.0) yapar.
 * Boylece 1.9+ cooldown cezasi kalkar; hizli tikla = tam hasar.
 *
 * ⚠️ CI-DOGRULA: method imzasi Yarn 1.21.1'de
 *   getAttackCooldownProgress(F)F  olmali. Hata cikarsa imzayi IDE/CI ile duzelt.
 */
@Mixin(PlayerEntity.class)
public class PlayerAttackCooldownMixin {
    @Inject(method = "getAttackCooldownProgress(F)F", at = @At("HEAD"), cancellable = true)
    private void bwt$fullCharge(float baseTime, CallbackInfoReturnable<Float> cir) {
        if (CombatToggle.is18()) {
            cir.setReturnValue(1.0f);
        }
    }
}
