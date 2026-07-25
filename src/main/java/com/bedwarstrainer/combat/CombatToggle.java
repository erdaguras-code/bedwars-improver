package com.bedwarstrainer.combat;

/**
 * 1.8 combat anahtari. Bu mod bir antrenman dunyasi oldugundan 1.8 combat
 * global olarak aciktir (cooldown yok + 1.8 knockback). Istersen komutla
 * kapatilabilir hale getiririz.
 */
public final class CombatToggle {
    private CombatToggle() {}
    public static volatile boolean OLD_COMBAT = true;
    public static boolean is18() { return OLD_COMBAT; }
}
