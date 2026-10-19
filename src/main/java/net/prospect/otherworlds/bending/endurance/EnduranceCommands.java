package net.prospect.otherworlds.bending.endurance;

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.prospect.otherworlds.Config;
import net.prospect.otherworlds.OtherWorlds;
import net.prospect.otherworlds.registry.ModAttachments;

/** Debug commands for testing Endurance. The final UI will never show these numbers to players. */
@EventBusSubscriber(modid = OtherWorlds.MODID)
public class EnduranceCommands {

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("bending")
                .then(Commands.literal("endurance")
                        .requires(source -> source.hasPermission(2))
                        .executes(ctx -> status(ctx.getSource()))
                        .then(Commands.literal("set")
                                .then(Commands.argument("value", DoubleArgumentType.doubleArg(0.0))
                                        .executes(ctx -> {
                                            ServerPlayer player = ctx.getSource().getPlayerOrException();
                                            EnduranceHandler.setCurrent(player, DoubleArgumentType.getDouble(ctx, "value"));
                                            return status(ctx.getSource());
                                        })))
                        .then(Commands.literal("drain")
                                .then(Commands.argument("amount", DoubleArgumentType.doubleArg(0.0))
                                        .executes(ctx -> {
                                            ServerPlayer player = ctx.getSource().getPlayerOrException();
                                            EnduranceHandler.setCurrent(player, EnduranceHandler.getCurrent(player)
                                                    - DoubleArgumentType.getDouble(ctx, "amount"));
                                            EnduranceHandler.markHit(player);   // regen waits, like after a real hit
                                            return status(ctx.getSource());
                                        })))
                        .then(Commands.literal("refill")
                                .executes(ctx -> {
                                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                                    EnduranceHandler.setCurrent(player, EnduranceHandler.getMax(player));
                                    return status(ctx.getSource());
                                }))
                        .then(Commands.literal("level")
                                .then(Commands.argument("level", IntegerArgumentType.integer(0))
                                        .executes(ctx -> {
                                            ServerPlayer player = ctx.getSource().getPlayerOrException();
                                            EnduranceHandler.setLevel(player, IntegerArgumentType.getInteger(ctx, "level"));
                                            return status(ctx.getSource());
                                        })))
                        .then(Commands.literal("train")
                                .then(Commands.argument("amount", DoubleArgumentType.doubleArg(0.0))
                                        .executes(ctx -> {
                                            ServerPlayer player = ctx.getSource().getPlayerOrException();
                                            EnduranceHandler.train(player, DoubleArgumentType.getDouble(ctx, "amount"));
                                            return status(ctx.getSource());
                                        })))
                        .then(Commands.literal("spar")
                                .then(Commands.argument("on", BoolArgumentType.bool())
                                        .executes(ctx -> {
                                            ServerPlayer player = ctx.getSource().getPlayerOrException();
                                            EnduranceHandler.setSparring(player, BoolArgumentType.getBool(ctx, "on"));
                                            return status(ctx.getSource());
                                        })))));
    }

    private static int status(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        EnduranceData data = player.getData(ModAttachments.ENDURANCE);
        EnduranceState state = player.getData(ModAttachments.ENDURANCE_STATE);

        double max = EnduranceHandler.getMax(player);
        double current = EnduranceHandler.getCurrent(player);
        double delayTicks = Config.ENDURANCE_REGEN_DELAY_SECONDS.get() * 20.0;
        long sinceHit = player.level().getGameTime() - state.lastHitTick;

        String regen;
        if (state.sparring) regen = "paused (spar)";
        else if (current >= max) regen = "full";
        else if (sinceHit < delayTicks) regen = String.format("waiting %.1fs", (delayTicks - sinceHit) / 20.0);
        else regen = "regenerating";

        send(source, String.format("Endurance: %.2f / %.2f  (damage stage %d of %d)",
                current, max, EnduranceHandler.getStage(player), EnduranceHandler.STAGES));
        send(source, String.format("Level %d, trained +%.2f (cap %.2f), rolls: max %+.0f%% regen %+.0f%%",
                data.level(), data.trained(), Config.ENDURANCE_TRAINED_CAP.get(),
                data.maxRoll() * Config.ENDURANCE_VARIANCE_PERCENT.get(),
                data.regenRoll() * Config.ENDURANCE_REGEN_VARIANCE_PERCENT.get()));
        send(source, String.format("Regen: %.2f/s, %s", EnduranceHandler.getRegen(player), regen));
        send(source, String.format("Last hit: %.2f after armor, Endurance took %.2f",
                state.lastIncoming, state.lastAbsorbed));
        send(source, "Spar mode: " + (state.sparring ? "ON (hearts untouchable)" : "off"));
        return 1;
    }

    private static void send(CommandSourceStack source, String text) {
        source.sendSuccess(() -> Component.literal(text), false);
    }
}