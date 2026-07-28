package com.humanclicker;

import java.security.SecureRandom;
import java.util.SplittableRandom;

/**
 * Tiklama zamanlayicisi.
 *
 * <p>Uretilen her aralik, ayarlardaki [cpsMin, cpsMax] bandinin icinde kalir:
 * en kisa aralik 1s/cpsMax, en uzun aralik 1s/cpsMin. Varsayilan 13-18 CPS
 * icin bu 55.6 ms - 76.9 ms demektir; hicbir tiklama bu araligin disina cikmaz.
 *
 * <p>Bant icinde ise her aralik bagimsiz olarak ve olabildigince rastgele
 * secilir. Sabit bir CPS degeri tutulmaz, bir onceki aralik bir sonrakini
 * belirlemez. Konum secimi dort kaynagin karisimidir; her tiklamada hangisinin
 * kullanilacagi da rastgeledir:
 * <ul>
 *   <li>duz dagilim - bandin tamamina esit olasilikla yayilir (en yuksek entropi),</li>
 *   <li>suruklenme - bir onceki konumdan genis adimli rastgele yuruyus,</li>
 *   <li>tempo - saniyeler icinde yavasca gezinen bir merkez etrafinda dagilim,</li>
 *   <li>kenar - bandin en hizli veya en yavas ucuna yakin degerler.</li>
 * </ul>
 * Bant disina tasan degerler kirpilmaz, geri yansitilir; boylece bant uclarinda
 * yigilma (kirpmanin birakacagi belirgin iz) olusmaz.
 *
 * <p>Tempo merkezi yavas bir rastgele yuruyus yaptigi icin saniyelik ortalama
 * hiz da bandin icinde gezinir, sabit bir degere kilitlenmez. Butun araliklar
 * bant icinde oldugundan hangi pencere alinirsa alinsin ortalama hiz da
 * kacinilmaz olarak [cpsMin, cpsMax] araliginda kalir.
 */
public final class ClickScheduler {
    /** Tek bir tickte gonderilebilecek en fazla tiklama (gecikme telafisi icin). */
    private static final int MAX_CLICKS_PER_TICK = 3;
    /** Bu kadar geride kalindiysa zamanlama sifirdan kurulur. */
    private static final long RESYNC_LAG_NANOS = 1_000_000_000L;
    private static final double NANOS_PER_SECOND = 1.0E9;

    /** Karisim agirliklari; toplami 1.0 olmalidir. */
    private static final double WEIGHT_UNIFORM = 0.40;
    private static final double WEIGHT_DRIFT = 0.25;
    private static final double WEIGHT_TEMPO = 0.30;
    private static final double WEIGHT_EDGE = 0.05;

    /** Suruklenme adiminin standart sapmasi (bant genisligine oranla). */
    private static final double DRIFT_STEP = 0.32;
    /** Tempo merkezi etrafindaki dagilimin standart sapmasi. */
    private static final double TEMPO_SPREAD = 0.18;
    /** Tempo merkezinin her tiklamada attigi adim; hizin saniyeler icinde gezinmesini saglar. */
    private static final double TEMPO_STEP = 0.05;
    /** Kenar modunda uclara olan azami uzaklik. */
    private static final double EDGE_MARGIN = 0.18;

    private final SplittableRandom random = newRandom();

    private boolean running = false;
    private long nextClickNanos = 0L;
    /** Son araligin bant icindeki konumu, 0.0 = en hizli, 1.0 = en yavas. */
    private double lastPosition;
    /** Yavas gezinen tempo merkezi. */
    private double tempoCenter;

    public ClickScheduler() {
        this.lastPosition = this.random.nextDouble();
        this.tempoCenter = this.random.nextDouble();
    }

    public void stop() {
        this.running = false;
        this.nextClickNanos = 0L;
    }

    /**
     * Verilen ana kadar zamani gelmis tiklama sayisini dondurur.
     */
    public int pollDueClicks(long nowNanos, ClickerConfig cfg) {
        if (!this.running) {
            this.running = true;
            this.lastPosition = this.random.nextDouble();
            this.tempoCenter = this.random.nextDouble();
            this.nextClickNanos = nowNanos + this.nextIntervalNanos(cfg);
            return 0;
        }

        int due = 0;
        while (nowNanos >= this.nextClickNanos && due < MAX_CLICKS_PER_TICK) {
            due++;
            this.nextClickNanos += this.nextIntervalNanos(cfg);
        }
        if (nowNanos - this.nextClickNanos > RESYNC_LAG_NANOS) {
            this.nextClickNanos = nowNanos + this.nextIntervalNanos(cfg);
        }
        return due;
    }

    /**
     * Bir sonraki araligi nanosaniye olarak uretir. Sonuc her zaman
     * [1s/cpsMax, 1s/cpsMin] araligindadir.
     */
    private long nextIntervalNanos(ClickerConfig cfg) {
        cfg.normalizeBand();
        double slowCps = ClickerConfig.clampCps(cfg.cpsMin);
        double fastCps = Math.max(slowCps, ClickerConfig.clampCps(cfg.cpsMax));

        double shortest = NANOS_PER_SECOND / fastCps;
        double longest = NANOS_PER_SECOND / slowCps;

        double interval = shortest + this.nextPosition(cfg) * (longest - shortest);
        return (long) Math.max(1.0, interval);
    }

    /**
     * Aralik icin bant konumunu (0.0 - 1.0) secer.
     */
    private double nextPosition(ClickerConfig cfg) {
        // Tempo merkezi her tiklamada bir adim atar: hiz saniyeler icinde
        // yavasca yukselip duser, hicbir yerde sabitlenmez.
        this.tempoCenter = fold(this.tempoCenter + this.random.nextGaussian() * TEMPO_STEP);

        double pick = this.random.nextDouble();
        double position;

        if ((pick -= WEIGHT_UNIFORM) < 0.0) {
            position = this.random.nextDouble();
        } else if ((pick -= WEIGHT_DRIFT) < 0.0) {
            position = this.lastPosition + this.random.nextGaussian() * DRIFT_STEP;
        } else if ((pick -= WEIGHT_TEMPO) < 0.0) {
            position = this.tempoCenter + this.random.nextGaussian() * TEMPO_SPREAD;
        } else {
            double edge = this.random.nextDouble() * EDGE_MARGIN;
            position = this.random.nextBoolean() ? edge : 1.0 - edge;
        }

        // Ince gurultu: iki aralik birbirinin ayni olmasin, adim genisligi de degissin.
        double micro = ClickerConfig.clampJitter(cfg.jitter) * 0.10;
        position += this.random.nextGaussian() * micro;

        position = fold(position);
        this.lastPosition = position;
        return position;
    }

    /**
     * [0, 1] disina cikan degeri kirpmak yerine sinirlardan geri yansitir.
     */
    private static double fold(double value) {
        if (!Double.isFinite(value)) {
            return 0.5;
        }
        double folded = Math.abs(value) % 2.0;
        return folded > 1.0 ? 2.0 - folded : folded;
    }

    private static SplittableRandom newRandom() {
        long seed;
        try {
            seed = new SecureRandom().nextLong();
        } catch (RuntimeException e) {
            seed = System.nanoTime();
        }
        return new SplittableRandom(seed ^ System.nanoTime() ^ (System.identityHashCode(new Object()) * 0x9E3779B97F4A7C15L));
    }
}
