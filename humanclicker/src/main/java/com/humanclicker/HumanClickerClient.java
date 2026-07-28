package com.humanclicker;

import com.humanclicker.command.ClickerCommands;
import com.humanclicker.mixin.MinecraftClientAccessor;
import com.humanclicker.mixin.MinecraftClientInvoker;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import net.minecraft.util.hit.HitResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Human Clicker - sol tus basili tutuldugunda saldiri tiklamasini tekrarlar.
 *
 * BLOK KIRMA NEDEN BOZULMAZ
 * -------------------------
 * Minecraft'ta iki ayri yol var:
 *
 *   - Blok kirma : MinecraftClient.handleBlockBreaking(...), tusun FIZIKSEL
 *                  basili olmasina (attackKey.isPressed()) bakar. Basili
 *                  tuttugun surece ilerler; birakinca ilerleme sifirlanir.
 *   - Saldiri    : doAttack(), tusun BASILMA SAYACINA (wasPressed()) bakar.
 *
 * Bu mod fiziksel tus durumuna hic dokunmuyor; sadece doAttack()'i fazladan
 * cagiriyor. Ustune bir de nisan bir BLOGUN uzerindeyken hicbir sey
 * gondermiyor. Yani kirma sirasinda mod tamamen devre disi kaliyor ve vanilla
 * davranisi aynen calisiyor.
 */
public class HumanClickerClient implements ClientModInitializer {

    public static final String MOD_ID = "humanclicker";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private final ClickScheduler scheduler = new ClickScheduler();

    /**
     * clickAir aciksa bosluga vurmak vanilla'da attackCooldown'i 10 tick'e
     * cekebilir ve bu da blok kirmanin baslamasini geciktirir. Bu bayrak
     * "o gecikmeyi biz olusturduk mu" sorusunu tutar ki nisan bloga
     * dondugunde sadece kendi artigimizi temizleyelim.
     */
    private boolean causedMissCooldown = false;

    @Override
    public void onInitializeClient() {
        ClickerConfig.load();
        HumanClickerKeys.register();
        ClickerCommands.register();
        ClientTickEvents.END_CLIENT_TICK.register(this::onClientTick);
        LOGGER.info("[HumanClicker] hazir.");
    }

    private void onClientTick(MinecraftClient client) {
        handleToggleKey(client);

        ClickerConfig cfg = ClickerConfig.get();

        if (!cfg.enabled || client.player == null || client.world == null) {
            scheduler.stop();
            return;
        }

        // GUI acikken veya imlec serbestken tiklama uretme.
        if (client.currentScreen != null || !client.mouse.isCursorLocked()) {
            scheduler.stop();
            return;
        }

        // Tetikleyici: sol tusu FIZIKSEL olarak basili tutmak.
        if (!client.options.attackKey.isPressed()) {
            scheduler.stop();
            return;
        }

        HitResult target = client.crosshairTarget;
        if (target == null) {
            scheduler.stop();
            return;
        }

        // Saat, tus basili oldugu surece HER DURUMDA ilerler. Gonderilmeyecek
        // tiklamalar da burada uretilip atilir; boylece nisan hedefe geri
        // dondugunde birikmis bir tiklama patlamasi olusmaz.
        int due = scheduler.pollDueClicks(System.nanoTime(), cfg);

        // --- BLOK KORUMASI ---
        // Nisan bir blokta: burasi kirma bolgesi. Uretilen tiklamalar atilir,
        // mod hicbir sey gondermez, vanilla kirma davranisi aynen calisir.
        if (target.getType() == HitResult.Type.BLOCK) {
            clearOwnMissCooldown(client);
            return;
        }

        boolean onEntity = target.getType() == HitResult.Type.ENTITY;

        if (!onEntity && !cfg.clickAir) {
            return;
        }
        if (due <= 0) {
            return;
        }

        MinecraftClientInvoker invoker = (MinecraftClientInvoker) client;
        for (int i = 0; i < due; i++) {
            invoker.humanClicker$doAttack();
        }

        if (!onEntity) {
            causedMissCooldown = true;
        }
    }

    private void handleToggleKey(MinecraftClient client) {
        while (HumanClickerKeys.toggle.wasPressed()) {
            ClickerConfig cfg = ClickerConfig.get();
            cfg.enabled = !cfg.enabled;
            ClickerConfig.save();
            scheduler.stop();
            if (client.player != null) {
                client.player.sendMessage(Text.literal(cfg.enabled
                        ? "[HumanClicker] acik (" + fmt(cfg.cpsMin) + "-" + fmt(cfg.cpsMax) + " CPS)"
                        : "[HumanClicker] kapali"), true);
            }
        }
    }

    /**
     * clickAir yuzunden biz bir "bosa vurus" gecikmesi yarattiysak, nisan
     * bloga donunce onu temizler. Biz yaratmadiysak vanilla'nin degerine
     * dokunmayiz.
     */
    private void clearOwnMissCooldown(MinecraftClient client) {
        if (!causedMissCooldown) {
            return;
        }
        causedMissCooldown = false;
        MinecraftClientAccessor accessor = (MinecraftClientAccessor) client;
        if (accessor.humanClicker$getAttackCooldown() > 0) {
            accessor.humanClicker$setAttackCooldown(0);
        }
    }

    static String fmt(double value) {
        return String.format(java.util.Locale.ROOT, "%.1f", value);
    }
}
