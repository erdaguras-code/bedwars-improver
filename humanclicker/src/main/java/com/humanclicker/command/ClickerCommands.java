package com.humanclicker.command;

import com.humanclicker.ClickerConfig;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.text.Text;

import java.util.Locale;

/**
 * Istemci tarafi komutlar (sunucuya gitmez):
 *
 *   /humanclicker                     -> durum
 *   /humanclicker on|off
 *   /humanclicker cps <min> <max>     -> hiz bandi
 *   /humanclicker jitter <0..0.5>
 *   /humanclicker air <true|false>
 */
public final class ClickerCommands {

    private ClickerCommands() {}

    public static void register() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->
            dispatcher.register(ClientCommandManager.literal("humanclicker")
                .executes(ctx -> status(ctx.getSource()))

                .then(ClientCommandManager.literal("on")
                    .executes(ctx -> setEnabled(ctx.getSource(), true)))

                .then(ClientCommandManager.literal("off")
                    .executes(ctx -> setEnabled(ctx.getSource(), false)))

                .then(ClientCommandManager.literal("cps")
                    .then(ClientCommandManager.argument("min",
                            DoubleArgumentType.doubleArg(ClickerConfig.MIN_CPS, ClickerConfig.MAX_CPS))
                        .then(ClientCommandManager.argument("max",
                                DoubleArgumentType.doubleArg(ClickerConfig.MIN_CPS, ClickerConfig.MAX_CPS))
                            .executes(ctx -> {
                                ClickerConfig cfg = ClickerConfig.get();
                                cfg.cpsMin = DoubleArgumentType.getDouble(ctx, "min");
                                cfg.cpsMax = DoubleArgumentType.getDouble(ctx, "max");
                                cfg.normalizeBand();
                                ClickerConfig.save();
                                return status(ctx.getSource());
                            }))))

                .then(ClickerCommands.jitterNode())

                .then(ClientCommandManager.literal("air")
                    .then(ClientCommandManager.argument("value", BoolArgumentType.bool())
                        .executes(ctx -> {
                            ClickerConfig cfg = ClickerConfig.get();
                            cfg.clickAir = BoolArgumentType.getBool(ctx, "value");
                            ClickerConfig.save();
                            return status(ctx.getSource());
                        })))
            )
        );
    }

    private static com.mojang.brigadier.builder.LiteralArgumentBuilder<FabricClientCommandSource> jitterNode() {
        return ClientCommandManager.literal("jitter")
            .then(ClientCommandManager.argument("value",
                    DoubleArgumentType.doubleArg(0.0, ClickerConfig.MAX_JITTER))
                .executes(ctx -> {
                    ClickerConfig cfg = ClickerConfig.get();
                    cfg.jitter = ClickerConfig.clampJitter(DoubleArgumentType.getDouble(ctx, "value"));
                    ClickerConfig.save();
                    return status(ctx.getSource());
                }));
    }

    private static int setEnabled(FabricClientCommandSource source, boolean value) {
        ClickerConfig cfg = ClickerConfig.get();
        cfg.enabled = value;
        ClickerConfig.save();
        return status(source);
    }

    private static int status(FabricClientCommandSource source) {
        ClickerConfig cfg = ClickerConfig.get();
        source.sendFeedback(Text.literal(String.format(Locale.ROOT,
                "[HumanClicker] %s | hiz %.1f-%.1f CPS | sapma %.2f | bosluga tikla: %s",
                cfg.enabled ? "acik" : "kapali",
                cfg.cpsMin, cfg.cpsMax, cfg.jitter,
                cfg.clickAir ? "evet" : "hayir")));
        return 1;
    }
}
