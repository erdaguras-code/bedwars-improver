package com.bedwarstrainer.event;

import com.bedwarstrainer.lobby.LobbyBuilder;
import com.bedwarstrainer.module.TrainingModule;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.ActionResult;

/**
 * Lobideki NPC'lere (armor stand) sag tiklaninca ilgili modul arenasina isinlar.
 */
public final class InteractionHandler {
    private InteractionHandler() {}

    public static void register() {
        UseEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (world.isClient()) return ActionResult.PASS;
            if (!(entity instanceof ArmorStandEntity)) return ActionResult.PASS;
            if (!(player instanceof ServerPlayerEntity serverPlayer)) return ActionResult.PASS;

            for (String tag : entity.getCommandTags()) {
                TrainingModule module = TrainingModule.fromTag(tag);
                if (module != null) {
                    LobbyBuilder.sendToModule(serverPlayer, module);
                    return ActionResult.SUCCESS;
                }
            }
            return ActionResult.PASS;
        });
    }
}
