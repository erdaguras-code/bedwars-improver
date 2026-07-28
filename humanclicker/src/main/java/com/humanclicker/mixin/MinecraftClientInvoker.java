package com.humanclicker.mixin;

import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/**
 * Vanilla'nin kendi saldiri metodunu disaridan cagirabilmek icin.
 *
 * Tiklamayi taklit etmenin en guvenli yolu bu: kendi saldiri/paket kodumuzu
 * yazmiyoruz, oyuncunun fiziksel tiklamasinin gectigi TAM AYNI koddan
 * geciyoruz. Boylece vurus mesafesi, sweep, cooldown, paket sirasi vs.
 * neyse o kaliyor.
 *
 * Imza: private boolean doAttack() -- Yarn 1.21.1'e karsi derlenerek dogrulandi.
 */
@Mixin(MinecraftClient.class)
public interface MinecraftClientInvoker {

    @Invoker("doAttack")
    boolean humanClicker$doAttack();
}
