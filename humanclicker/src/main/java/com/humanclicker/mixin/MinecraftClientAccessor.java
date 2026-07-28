package com.humanclicker.mixin;

import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * attackCooldown alanina erisim.
 *
 * Bu alan (Mojang adiyla "missTime") bosluga vurunca 10 tick'e set edilir ve
 * doldugu sure boyunca blok kirma baslamaz. Bizim clickAir modumuz bosluga
 * tiklayabildigi icin, nisan bir bloga dondugu an KENDI biraktigi bu artigi
 * temizlemek zorunda; yoksa kullanici blogu kirmaya baslarken yarim saniye
 * gecikme hisseder.
 *
 * ⚠️ CI-DOGRULA: Yarn 1.21.1'de alan adi 'attackCooldown' (int) olmali.
 */
@Mixin(MinecraftClient.class)
public interface MinecraftClientAccessor {

    @Accessor("attackCooldown")
    int humanClicker$getAttackCooldown();

    @Accessor("attackCooldown")
    void humanClicker$setAttackCooldown(int value);
}
