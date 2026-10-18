package net.prospect.otherworlds.bending.command;

import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.prospect.otherworlds.OtherWorlds;
import net.prospect.otherworlds.bending.ModAbilities;
import net.prospect.otherworlds.bending.ability.Ability;
import net.prospect.otherworlds.bending.ability.AbilityManager;
import net.prospect.otherworlds.bending.ability.AbilityResult;
import net.prospect.otherworlds.bending.chi.ChiHandler;

@EventBusSubscriber(modid = OtherWorlds.MODID)
public class BendingCommands {

    @SubscribeEvent
    public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("bending")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("use")
                        .then(Commands.argument("ability", ResourceLocationArgument.id())
                                .suggests((c, b) -> SharedSuggestionProvider.suggestResource(
                                        ModAbilities.REGISTRY.keySet(), b))
                                .executes(BendingCommands::use)))
                .then(Commands.literal("chi")
                        .then(Commands.literal("get").executes(BendingCommands::chiGet))
                        .then(Commands.literal("set")
                                .then(Commands.argument("amount", DoubleArgumentType.doubleArg(0))
                                        .executes(BendingCommands::chiSet)))
                        .then(Commands.literal("level")
                                .then(Commands.argument("level", IntegerArgumentType.integer(0))
                                        .executes(BendingCommands::chiLevel)))));
    }

    private static int use(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        ResourceLocation id = ResourceLocationArgument.getId(ctx, "ability");
        Ability ability = ModAbilities.REGISTRY.get(id);
        if (ability == null) {
            ctx.getSource().sendFailure(Component.literal("Unknown ability: " + id));
            return 0;
        }

        AbilityResult result = AbilityManager.tryUse(player, ability, 1.0);
        if (result == AbilityResult.SUCCESS) {
            ctx.getSource().sendSuccess(() -> Component.literal("Used ").append(ability.getDisplayName()), false);
            return 1;
        }
        ctx.getSource().sendFailure(Component.literal("Could not use " + id + ": " + result));
        return 0;
    }

    private static int chiGet(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        ctx.getSource().sendSuccess(() -> Component.literal(
                        "Chi " + ChiHandler.round2(ChiHandler.getChi(player)) + " / " + ChiHandler.getMaxChi(player)
                                + "  (regen " + ChiHandler.getRegen(player) + "/s, level " + ChiHandler.getChiLevel(player) + ")"),
                false);
        return 1;
    }

    private static int chiSet(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        ChiHandler.setChi(player, DoubleArgumentType.getDouble(ctx, "amount"));
        return chiGet(ctx);
    }

    private static int chiLevel(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        ChiHandler.setChiLevel(player, IntegerArgumentType.getInteger(ctx, "level"));
        return chiGet(ctx);
    }
}