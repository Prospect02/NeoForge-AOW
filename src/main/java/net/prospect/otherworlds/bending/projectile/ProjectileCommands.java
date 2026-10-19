package net.prospect.otherworlds.bending.projectile;

import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.prospect.otherworlds.OtherWorlds;

/** Test command: fires a bending projectile from your eyes. Remove once real moves use it. */
@EventBusSubscriber(modid = OtherWorlds.MODID)
public class ProjectileCommands {

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("bending")
                .then(Commands.literal("shoot")
                        .requires(source -> source.hasPermission(2))
                        .executes(ctx -> shoot(ctx.getSource(), BendingProjectile.DEFAULT_DAMAGE))
                        .then(Commands.argument("damage", DoubleArgumentType.doubleArg(0.0))
                                .executes(ctx -> shoot(ctx.getSource(),
                                        (float) DoubleArgumentType.getDouble(ctx, "damage"))))));
    }

    private static int shoot(CommandSourceStack source, float damage) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        BendingProjectile.shoot(player, player.serverLevel(), damage, 1.2, 24.0);
        return 1;
    }
}