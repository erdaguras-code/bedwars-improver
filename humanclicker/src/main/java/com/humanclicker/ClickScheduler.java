package com.humanclicker;

import java.util.Random;

/**
 * Tiklama zamanlayicisi.
 *
 * Gercek zamana (nanoTime) gore calisir, tick sayisina degil; boylece FPS
 * dususu veya lag hedef hizi bozmaz.
 *
 * Hiz iki katmanli uretiliyor:
 *   1) Efektif CPS, cpsMin..cpsMax bandi icinden secilir ve birkac tiklamada
 *      bir yeniden secilir (tempo bir yerde sabit kalmaz).
 *   2) Her aralik ayrica gaussian bir carpanla oynatilir.
 *
 * Onemli: zamanlayici sol tus basili oldugu SURECE calisir. Nisan blokta
 * oldugu icin tiklama gonderilmedigi anlarda bile saat ilerler; boylece
 * hedefe geri donuldugunde birikmis bir tiklama patlamasi olusmaz.
 */
public final class ClickScheduler {

    /** Tek bir tick icinde gonderilebilecek en fazla tiklama (lag sigortasi). */
    private static final int MAX_CLICKS_PER_TICK = 3;

    /** Sapma sonrasi araligin taban degerine gore alt/ust siniri. */
    private static final double MIN_FACTOR = 0.55;
    private static final double MAX_FACTOR = 1.75;

    /** Efektif CPS'in bant icinde yeniden secilme sikligi (tiklama sayisi). */
    private static final int RESAMPLE_MIN_CLICKS = 6;
    private static final int RESAMPLE_MAX_CLICKS = 18;

    private final Random random = new Random();

    private boolean running = false;
    private long nextClickNanos = 0L;

    /** Su an gecerli olan efektif hiz ve kac tiklama sonra yenilenecegi. */
    private double currentCps = 0.0;
    private int clicksUntilResample = 0;

    /** Tus birakildiginda / durum bozuldugunda cagrilir. */
    public void stop() {
        running = false;
        nextClickNanos = 0L;
        clicksUntilResample = 0;
    }

    /**
     * O ana kadar zamani gelmis tiklama sayisini dondurur ve saati ilerletir.
     *
     * Ilk cagride 0 doner: sol tusa fiziksel basisin kendisi zaten vanilla
     * tarafindan bir saldiri olarak islenmistir, onun ustune bir tane daha
     * eklemek ilk vurusu ciftlerdi.
     */
    public int pollDueClicks(long nowNanos, ClickerConfig cfg) {
        if (!running) {
            running = true;
            resampleCps(cfg);
            nextClickNanos = nowNanos + nextIntervalNanos(cfg);
            return 0;
        }

        int due = 0;
        while (nowNanos >= nextClickNanos && due < MAX_CLICKS_PER_TICK) {
            due++;
            nextClickNanos += nextIntervalNanos(cfg);
        }

        // Uzun bir donma sonrasi saat cok geride kaldiysa birikmis borcu silip
        // simdiden yeniden basla; yoksa tekrar tekrar MAX_CLICKS_PER_TICK atar.
        if (nowNanos - nextClickNanos > 1_000_000_000L) {
            nextClickNanos = nowNanos + nextIntervalNanos(cfg);
        }

        return due;
    }

    private long nextIntervalNanos(ClickerConfig cfg) {
        if (clicksUntilResample <= 0) {
            resampleCps(cfg);
        }
        clicksUntilResample--;

        double base = 1_000_000_000.0 / currentCps;
        double factor = 1.0 + random.nextGaussian() * ClickerConfig.clampJitter(cfg.jitter);
        factor = Math.max(MIN_FACTOR, Math.min(MAX_FACTOR, factor));
        return (long) (base * factor);
    }

    /** Efektif hizi cpsMin..cpsMax bandindan yeniden secer. */
    private void resampleCps(ClickerConfig cfg) {
        cfg.normalizeBand();
        double lo = cfg.cpsMin;
        double hi = cfg.cpsMax;
        currentCps = (hi > lo) ? lo + random.nextDouble() * (hi - lo) : lo;
        clicksUntilResample = RESAMPLE_MIN_CLICKS
                + random.nextInt(RESAMPLE_MAX_CLICKS - RESAMPLE_MIN_CLICKS + 1);
    }
}
