package com.bedwarstrainer.mixin;

import com.bedwarstrainer.combat.CombatToggle;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Box;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Collections;
import java.util.List;

/**
 * 1.9'da gelen SUPURME SALDIRISINI (sweeping attack) kapatir.
 *
 * Neden gerekli: cooldown'i her zaman dolu gosteren mixin'imiz, vanilla'nin
 * "tam sarjda supur" kosulunu surekli saglanir hale getiriyor. Yani 1.8 combat
 * isterken sweep'i istemeden kalicilastirmis oluyoruz.
 *
 * Nasil: PlayerEntity.attack() icinde sweep kurbanlarini toplayan
 * World.getNonSpectatingEntities cagrisini bos listeye yonlendiriyoruz.
 * Vuruslarin geri kalani (hasar, knockback, kritik) aynen calisir.
 *
 * require = 0: imza tutmazsa mixin sessizce uygulanmaz. Boyle bir durumda
 * oyun COKMEZ, sadece sweep acik kalir - oyunu kirmaktansa bu yeglenir.
 * (Yerelde derleyemedigimiz icin bu ihtiyat bilincli bir tercih.)
 */
@Mixin(PlayerEntity.class)
public class PlayerSweepAttackMixin {

    @SuppressWarnings("rawtypes")
    @Redirect(
            method = "attack",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/World;getNonSpectatingEntities(Ljava/lang/Class;Lnet/minecraft/util/math/Box;)Ljava/util/List;"
            ),
            require = 0
    )
    private List bwt$noSweepTargets(World world, Class entityClass, Box box) {
        if (CombatToggle.is18()) {
            return Collections.emptyList(); // supurulecek kimse yok
        }
        return world.getNonSpectatingEntities(entityClass, box);
    }

    /** Hasar gitse de gitmese de supurme efektini gostermeyelim. */
    @Inject(method = "spawnSweepAttackParticles", at = @At("HEAD"), cancellable = true, require = 0)
    private void bwt$noSweepParticles(CallbackInfo ci) {
        if (CombatToggle.is18()) {
            ci.cancel();
        }
    }
}
