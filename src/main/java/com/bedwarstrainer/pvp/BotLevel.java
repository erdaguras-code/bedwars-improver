package com.bedwarstrainer.pvp;

/**
 * Bot zorlugunu tek yerde toplayan ayar paketi. Seviye yukseldikce
 * daha hizli vurur, daha hizli kosar, strafe/combo acilir.
 */
public class BotLevel {
    public final int level;
    public final double moveSpeed;        // hareket hizi
    public final int attackIntervalTicks; // vuruslar arasi tick (dusuk = hizli spam)
    public final double knockback;        // 1.8 yatay knockback siddeti
    public final double followRange;      // oyuncuyu ne kadar uzaktan gorur
    public final double maxHealth;
    public final double attackDamage;
    public final boolean strafe;
    public final boolean wTap;

    public BotLevel(int level) {
        this.level = Math.max(1, level);
        // Kademeli zorlasma (dengeyi oynayarak ayarlayabilirsin)
        this.moveSpeed = Math.min(0.32, 0.23 + level * 0.008);
        this.attackIntervalTicks = Math.max(4, 12 - level);   // lvl 8'de ~4 tick (cok hizli)
        this.knockback = 0.4;                                 // 1.8 standardi
        this.followRange = 24.0;
        this.maxHealth = 20.0;
        this.attackDamage = Math.min(6.0, 2.0 + level * 0.4);
        this.strafe = level >= 3;   // 3. seviyeden itibaren strafe
        this.wTap  = level >= 5;    // 5. seviyeden itibaren W-tap/combo
    }
}
