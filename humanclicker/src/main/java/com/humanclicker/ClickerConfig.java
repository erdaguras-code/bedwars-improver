package com.humanclicker;

import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/**
 * Ayarlar. config/humanclicker.properties dosyasinda saklanir.
 *
 * Hiz tek bir sayi degil bir ARALIK olarak tutulur (cpsMin..cpsMax): efektif
 * hiz bu bandin icinde zaman zaman yeniden secilir, ustune de her aralikta
 * ayrica sapma uygulanir.
 *
 * Referans: iyi bir oyuncu normal tiklamayla 8-10 CPS, jitter/butterfly ile
 * 12-16 CPS yapar. Ust sinir MAX_CPS ile 20'de kilitli.
 */
public final class ClickerConfig {

    public static final double MIN_CPS = 1.0;
    public static final double MAX_CPS = 20.0;
    public static final double MAX_JITTER = 0.5;

    private static final String FILE_NAME = "humanclicker.properties";

    /** Autoclicker acik mi (tus ile degistirilir, diske yazilir). */
    public boolean enabled = false;

    /** Hiz bandinin alt ucu (saniyedeki tiklama). */
    public double cpsMin = 13.0;

    /** Hiz bandinin ust ucu. */
    public double cpsMax = 18.0;

    /**
     * Tiklamalar arasi sapma orani. 0 = metronom gibi sabit (robotik),
     * 0.18 = araliklar daha dagilmis olur.
     */
    public double jitter = 0.18;

    /**
     * Crosshair bosluktayken (hicbir seye bakmiyorken) de tiklasin mi.
     * Varsayilan kapali: boylece autoclicker SADECE bir varliga nisan
     * alindiginda calisir ve blok kirma tarafina hic bulasmaz.
     */
    public boolean clickAir = false;

    // --- singleton ---

    private static ClickerConfig instance;

    private ClickerConfig() {}

    public static ClickerConfig get() {
        if (instance == null) {
            instance = new ClickerConfig();
        }
        return instance;
    }

    public static double clampCps(double value) {
        return Math.max(MIN_CPS, Math.min(MAX_CPS, value));
    }

    public static double clampJitter(double value) {
        return Math.max(0.0, Math.min(MAX_JITTER, value));
    }

    /** Bandi gecerli halde tutar: iki uc de sinirlar icinde ve min <= max. */
    public void normalizeBand() {
        cpsMin = clampCps(cpsMin);
        cpsMax = clampCps(cpsMax);
        if (cpsMin > cpsMax) {
            double tmp = cpsMin;
            cpsMin = cpsMax;
            cpsMax = tmp;
        }
    }

    private static Path path() {
        return FabricLoader.getInstance().getConfigDir().resolve(FILE_NAME);
    }

    public static void load() {
        ClickerConfig cfg = get();
        Path file = path();
        if (!Files.exists(file)) {
            save();
            return;
        }
        Properties props = new Properties();
        try (InputStream in = Files.newInputStream(file)) {
            props.load(in);
        } catch (IOException e) {
            HumanClickerClient.LOGGER.warn("[HumanClicker] Ayar dosyasi okunamadi, varsayilanlar kullanilacak.", e);
            return;
        }
        cfg.enabled = Boolean.parseBoolean(props.getProperty("enabled", String.valueOf(cfg.enabled)));
        cfg.cpsMin = parseDouble(props.getProperty("cpsMin"), cfg.cpsMin);
        cfg.cpsMax = parseDouble(props.getProperty("cpsMax"), cfg.cpsMax);
        cfg.jitter = clampJitter(parseDouble(props.getProperty("jitter"), cfg.jitter));
        cfg.clickAir = Boolean.parseBoolean(props.getProperty("clickAir", String.valueOf(cfg.clickAir)));
        cfg.normalizeBand();
    }

    public static void save() {
        ClickerConfig cfg = get();
        cfg.normalizeBand();
        Properties props = new Properties();
        props.setProperty("enabled", String.valueOf(cfg.enabled));
        props.setProperty("cpsMin", String.valueOf(cfg.cpsMin));
        props.setProperty("cpsMax", String.valueOf(cfg.cpsMax));
        props.setProperty("jitter", String.valueOf(cfg.jitter));
        props.setProperty("clickAir", String.valueOf(cfg.clickAir));
        try {
            Path file = path();
            Files.createDirectories(file.getParent());
            try (OutputStream out = Files.newOutputStream(file)) {
                props.store(out, "Human Clicker ayarlari");
            }
        } catch (IOException e) {
            HumanClickerClient.LOGGER.warn("[HumanClicker] Ayar dosyasi yazilamadi.", e);
        }
    }

    private static double parseDouble(String raw, double fallback) {
        if (raw == null) {
            return fallback;
        }
        try {
            return Double.parseDouble(raw.trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }
}
