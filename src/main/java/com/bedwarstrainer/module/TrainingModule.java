package com.bedwarstrainer.module;

import net.minecraft.util.math.BlockPos;

/**
 * Egitim modulleri. Her modulun lobideki NPC konumu, isinlanma hedefi
 * ve dil anahtari burada tanimli. Yeni modul eklemek icin buraya bir
 * enum girdisi eklemen yeterli.
 */
public enum TrainingModule {
    BRIDGING(
            "bridging",
            "bedwarstrainer.module.bridging",
            new BlockPos(-4, 100, 6),        // lobideki NPC konumu
            new BlockPos(100, 100, 0),       // arena baslangic platformu
            0.0f                             // isinlanma yonu (yaw) - +Z'ye bakar (bosluga dogru)
    ),
    PVP(
            "pvp",
            "bedwarstrainer.module.pvp",
            new BlockPos(0, 100, 6),
            new BlockPos(0, 100, 100),
            180.0f
    ),
    RESOURCES(
            "resources",
            "bedwarstrainer.module.resources",
            new BlockPos(4, 100, 6),
            new BlockPos(-100, 100, 0),
            90.0f
    );

    public final String id;
    public final String translationKey;
    public final BlockPos npcPos;
    public final BlockPos arenaSpawn;
    public final float arenaYaw;

    TrainingModule(String id, String translationKey, BlockPos npcPos, BlockPos arenaSpawn, float arenaYaw) {
        this.id = id;
        this.translationKey = translationKey;
        this.npcPos = npcPos;
        this.arenaSpawn = arenaSpawn;
        this.arenaYaw = arenaYaw;
    }

    /** NPC'ye eklenen etiket (command tag), tiklaninca hangi modul oldugunu anlamak icin. */
    public String tag() {
        return "bwt_module:" + id;
    }

    public static TrainingModule fromId(String id) {
        for (TrainingModule m : values()) {
            if (m.id.equalsIgnoreCase(id)) return m;
        }
        return null;
    }

    public static TrainingModule fromTag(String tag) {
        if (tag == null || !tag.startsWith("bwt_module:")) return null;
        return fromId(tag.substring("bwt_module:".length()));
    }
}
