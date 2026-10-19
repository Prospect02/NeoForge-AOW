package net.prospect.otherworlds.bending.progression;

import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
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
import net.prospect.otherworlds.bending.style.BendingStyle;
import net.prospect.otherworlds.bending.style.ModBendingStyles;

import java.util.Map;

/**
 * Debug and stand-in commands for progression. /bending learn scroll|master|force runs the same check
 * that real scrolls and masters will use later.
 */
@EventBusSubscriber(modid = OtherWorlds.MODID)
public class ProgressionCommands {
    private static final SuggestionProvider<CommandSourceStack> ABILITIES =
            (ctx, builder) -> SharedSuggestionProvider.suggestResource(ModAbilities.REGISTRY.keySet(), builder);
    private static final SuggestionProvider<CommandSourceStack> STYLES =
            (ctx, builder) -> SharedSuggestionProvider.suggestResource(ModBendingStyles.REGISTRY.keySet(), builder);

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("bending")
                .then(Commands.literal("progress")
                        .requires(source -> source.hasPermission(2))
                        .executes(ctx -> status(ctx.getSource()))
                        .then(Commands.literal("addxp")
                                .then(Commands.literal("player")
                                        .then(Commands.argument("amount", DoubleArgumentType.doubleArg(0.0))
                                                .executes(ctx -> {
                                                    ServerPlayer p = ctx.getSource().getPlayerOrException();
                                                    Progression.addPlayerXp(p, DoubleArgumentType.getDouble(ctx, "amount"));
                                                    return status(ctx.getSource());
                                                })))
                                .then(Commands.literal("element")
                                        .then(Commands.argument("style", ResourceLocationArgument.id()).suggests(STYLES)
                                                .then(Commands.argument("amount", DoubleArgumentType.doubleArg(0.0))
                                                        .executes(ctx -> {
                                                            ServerPlayer p = ctx.getSource().getPlayerOrException();
                                                            Progression.addElementXp(p, style(ctx), DoubleArgumentType.getDouble(ctx, "amount"));
                                                            return status(ctx.getSource());
                                                        }))))
                                .then(Commands.literal("move")
                                        .then(Commands.argument("ability", ResourceLocationArgument.id()).suggests(ABILITIES)
                                                .then(Commands.argument("amount", DoubleArgumentType.doubleArg(0.0))
                                                        .executes(ctx -> {
                                                            ServerPlayer p = ctx.getSource().getPlayerOrException();
                                                            Progression.addMoveXp(p, ability(ctx), DoubleArgumentType.getDouble(ctx, "amount"));
                                                            return status(ctx.getSource());
                                                        })))))
                        .then(Commands.literal("setlevel")
                                .then(Commands.literal("player")
                                        .then(Commands.argument("level", IntegerArgumentType.integer(1))
                                                .executes(ctx -> {
                                                    ServerPlayer p = ctx.getSource().getPlayerOrException();
                                                    Progression.setPlayerLevel(p, IntegerArgumentType.getInteger(ctx, "level"));
                                                    return status(ctx.getSource());
                                                })))
                                .then(Commands.literal("element")
                                        .then(Commands.argument("style", ResourceLocationArgument.id()).suggests(STYLES)
                                                .then(Commands.argument("level", IntegerArgumentType.integer(1))
                                                        .executes(ctx -> {
                                                            ServerPlayer p = ctx.getSource().getPlayerOrException();
                                                            Progression.setElementLevel(p, style(ctx), IntegerArgumentType.getInteger(ctx, "level"));
                                                            return status(ctx.getSource());
                                                        }))))
                                .then(Commands.literal("move")
                                        .then(Commands.argument("ability", ResourceLocationArgument.id()).suggests(ABILITIES)
                                                .then(Commands.argument("level", IntegerArgumentType.integer(1))
                                                        .executes(ctx -> {
                                                            ServerPlayer p = ctx.getSource().getPlayerOrException();
                                                            Progression.setMoveLevel(p, ability(ctx), IntegerArgumentType.getInteger(ctx, "level"));
                                                            return status(ctx.getSource());
                                                        })))))
                        .then(Commands.literal("forget")
                                .then(Commands.argument("ability", ResourceLocationArgument.id()).suggests(ABILITIES)
                                        .executes(ctx -> {
                                            ServerPlayer p = ctx.getSource().getPlayerOrException();
                                            Progression.forget(p, ability(ctx));
                                            return status(ctx.getSource());
                                        }))))
                .then(Commands.literal("learn")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.literal("scroll")
                                .then(Commands.argument("ability", ResourceLocationArgument.id()).suggests(ABILITIES)
                                        .executes(ctx -> learn(ctx, Progression.LearnSource.SCROLL))))
                        .then(Commands.literal("master")
                                .then(Commands.argument("ability", ResourceLocationArgument.id()).suggests(ABILITIES)
                                        .executes(ctx -> learn(ctx, Progression.LearnSource.MASTER))))
                        .then(Commands.literal("force")
                                .then(Commands.argument("ability", ResourceLocationArgument.id()).suggests(ABILITIES)
                                        .executes(ctx -> learn(ctx, Progression.LearnSource.FORCE))))));
    }

    private static int learn(CommandContext<CommandSourceStack> ctx, Progression.LearnSource source) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        Ability ability = ability(ctx);
        Progression.LearnResult result = Progression.learn(player, ability, source);
        int need = Progression.requiredLevel(ability, source);
        int have = Progression.getElementLevel(player, ability.getStyle());

        String text = switch (result) {
            case LEARNED -> "Learned " + ability.getId() + ".";
            case GAINED_XP -> "Already known. The scroll added move XP.";
            case ALREADY_KNOWN -> "Already known.";
            case TOO_LOW -> "The writing is unreadable gibberish. (Needs element level " + need + ", you have " + have + ".)";
            case MASTER_ONLY -> "This move cannot be learned from a scroll. Only a master can teach it.";
        };
        send(ctx.getSource(), text);
        return 1;
    }

    private static int status(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        ProgressionData data = Progression.data(player);

        send(source, String.format("Player level %d, xp %.1f / %.1f", data.playerLevel(), data.playerXp(),
                Progression.playerXpForNext(data.playerLevel())));
        for (BendingStyle style : ModBendingStyles.REGISTRY) {
            if (style.isSubStyle()) continue;
            ProgressionData.Track track = data.elementTrack(style.getId());
            send(source, String.format("  %s: level %d (%s), xp %.1f / %.1f", style.getId().getPath(), track.level(),
                    ElementTitle.forLevel(track.level()).getDisplayName(), track.xp(),
                    Progression.elementXpForNext(track.level())));
        }
        if (data.moves().isEmpty()) {
            send(source, "No moves learned.");
        }
        for (Map.Entry<ResourceLocation, ProgressionData.Track> entry : data.moves().entrySet()) {
            ProgressionData.Track track = entry.getValue();
            send(source, String.format("  move %s: level %d, xp %.1f / %.1f", entry.getKey().getPath(), track.level(),
                    track.xp(), Progression.moveXpForNext(track.level())));
        }
        return 1;
    }

    private static Ability ability(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ResourceLocation id = ResourceLocationArgument.getId(ctx, "ability");
        if (!ModAbilities.REGISTRY.containsKey(id)) {
            throw new SimpleCommandExceptionType(Component.literal("Unknown ability: " + id)).create();
        }
        return ModAbilities.REGISTRY.get(id);
    }

    private static BendingStyle style(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ResourceLocation id = ResourceLocationArgument.getId(ctx, "style");
        if (!ModBendingStyles.REGISTRY.containsKey(id)) {
            throw new SimpleCommandExceptionType(Component.literal("Unknown style: " + id)).create();
        }
        return ModBendingStyles.REGISTRY.get(id);
    }

    private static void send(CommandSourceStack source, String text) {
        source.sendSuccess(() -> Component.literal(text), false);
    }
}