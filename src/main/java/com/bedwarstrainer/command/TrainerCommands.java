package com.bedwarstrainer.command;

import com.bedwarstrainer.lobby.LobbyBuilder;
import com.bedwarstrainer.module.TrainingModule;
import com.bedwarstrainer.pvp.PvpManager;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;

/**
 * Test / kolaylik komutlari:
 *   /trainer lobby            -> lobiyi (yoksa) kurar ve seni oraya isinlar
 *   /trainer goto <module>    -> bir modul arenasina isinlar
 *   /trainer build            -> lobiyi zorla yeniden kurar
 */
public final class TrainerCommands {
    private TrainerCommands() {}

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
            dispatcher.register(CommandManager.literal("trainer")
                .then(CommandManager.literal("lobby").executes(ctx -> {
                    ServerCommandSource src = ctx.getSource();
                    ServerPlayerEntity player = src.getPlayerOrThrow();
                    ServerWorld world = player.getServerWorld();
                    if (!LobbyBuilder.isBuilt(world)) {
                        LobbyBuilder.buildLobby(world);
                    }
                    LobbyBuilder.sendToLobby(player);
                    return 1;
                }))
                .then(CommandManager.literal("build").executes(ctx -> {
                    ServerCommandSource src = ctx.getSource();
                    ServerPlayerEntity player = src.getPlayerOrThrow();
                    LobbyBuilder.buildLobby(player.getServerWorld());
                    src.sendFeedback(() -> Text.literal("Lobi yeniden kuruldu."), false);
                    return 1;
                }))
                .then(CommandManager.literal("goto")
                    .then(CommandManager.argument("module", StringArgumentType.word())
                        .executes(ctx -> {
                            ServerCommandSource src = ctx.getSource();
                            ServerPlayerEntity player = src.getPlayerOrThrow();
                            String id = StringArgumentType.getString(ctx, "module");
                            TrainingModule module = TrainingModule.fromId(id);
                            if (module == null) {
                                src.sendError(Text.literal("Bilinmeyen modul: " + id
                                        + " (bridging | pvp | resources)"));
                                return 0;
                            }
                            LobbyBuilder.sendToModule(player, module);
                            return 1;
                        })
                    )
                )
                .then(CommandManager.literal("pvp")
                    .then(CommandManager.literal("start")
                        .executes(ctx -> {
                            ServerPlayerEntity player = ctx.getSource().getPlayerOrThrow();
                            com.bedwarstrainer.lobby.LobbyBuilder.sendToModule(player, TrainingModule.PVP);
                            PvpManager.start(player, 1);
                            return 1;
                        })
                        .then(CommandManager.argument("level", IntegerArgumentType.integer(1, 50))
                            .executes(ctx -> {
                                ServerPlayerEntity player = ctx.getSource().getPlayerOrThrow();
                                int lvl = IntegerArgumentType.getInteger(ctx, "level");
                                com.bedwarstrainer.lobby.LobbyBuilder.sendToModule(player, TrainingModule.PVP);
                                PvpManager.start(player, lvl);
                                return 1;
                            })
                        )
                    )
                    .then(CommandManager.literal("stop").executes(ctx -> {
                        PvpManager.stop(ctx.getSource().getPlayerOrThrow());
                        return 1;
                    }))
                )
            )
        );
    }
}
