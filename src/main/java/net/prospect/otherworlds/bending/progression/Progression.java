package net.prospect.otherworlds.bending.progression;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.prospect.otherworlds.Config;
import net.prospect.otherworlds.bending.ability.Ability;
import net.prospect.otherworlds.bending.ability.Learning;
import net.prospect.otherworlds.bending.chi.ChiHandler;
import net.prospect.otherworlds.bending.data.BendingData;
import net.prospect.otherworlds.bending.endurance.EnduranceData;
import net.prospect.otherworlds.bending.endurance.EnduranceHandler;
import net.prospect.otherworlds.bending.endurance.EnduranceUser;
import net.prospect.otherworlds.bending.style.BendingStyle;
import net.prospect.otherworlds.registry.ModAttachments;

import java.util.Optional;
import java.util.function.IntToDoubleFunction;

/**
 * The one place progression rules live. Server side for everything that changes data.
 *
 *  - Player level (earned by defeating opponents) raises chi level and Endurance level.
 *  - Element level (earned by casting, and by training under a master) only gates learning.
 *  - Move level (casting, training, duplicate scrolls) scales the move itself through Mastery.of.
 */
public final class Progression {
    private Progression() {}

    public enum LearnSource { SCROLL, MASTER, FORCE }

    public enum LearnResult { LEARNED, GAINED_XP, ALREADY_KNOWN, TOO_LOW, MASTER_ONLY }

    // ---------- curves ----------

    public static double playerXpForNext(int level) {
        return Config.PLAYER_XP_BASE.get() * Math.pow(level, Config.PLAYER_XP_EXPONENT.get());
    }

    public static double elementXpForNext(int level) {
        return Config.ELEMENT_XP_BASE.get() * Math.pow(level, Config.ELEMENT_XP_EXPONENT.get());
    }

    public static double moveXpForNext(int level) {
        double needed = Config.MOVE_XP_BASE.get() * Math.pow(level, Config.MOVE_XP_EXPONENT.get());
        if (level >= Config.MOVE_SOFT_CAP.get()) needed *= Config.MOVE_OVERFLOW_MULTIPLIER.get();
        return needed;
    }

    // ---------- reading ----------

    public static ProgressionData data(Player player) {
        return player.getData(ModAttachments.PROGRESSION);
    }

    public static int getPlayerLevel(Player player) {
        return data(player).playerLevel();
    }

    /** Sub-styles share their parent element's level. */
    public static BendingStyle rootStyle(BendingStyle style) {
        BendingStyle current = style;
        while (current.isSubStyle()) {
            Optional<BendingStyle> parent = current.getParent();
            if (parent.isEmpty()) break;
            current = parent.get();
        }
        return current;
    }

    public static int getElementLevel(Player player, BendingStyle style) {
        return data(player).elementTrack(rootStyle(style).getId()).level();
    }

    public static boolean hasLearned(Player player, Ability ability) {
        return data(player).hasLearned(ability.getId());
    }

    public static int getMoveLevel(Player player, Ability ability) {
        return data(player).moveTrack(ability.getId()).level();
    }

    /**
     * 0.0 at move level 1 up to 1.0 at the soft cap (level 50). This is what Mastery.of returns,
     * so every move's numbers scale from it.
     */
    public static double moveMastery(Player player, Ability ability) {
        int cap = Config.MOVE_SOFT_CAP.get();
        if (cap <= 1) return 1.0;
        return Mth.clamp((getMoveLevel(player, ability) - 1) / (double) (cap - 1), 0.0, 1.0);
    }

    /** Element level needed to learn this move from the given source. */
    public static int requiredLevel(Ability ability, LearnSource source) {
        int required = ability.getRequiredElementLevel();
        if (source == LearnSource.MASTER) required = Math.max(1, required - Config.MASTER_LEVEL_DISCOUNT.get());
        return required;
    }

    /** XP multiplier while training or sparring. The sparring flag is the hook until master training exists. */
    public static double accelerator(LivingEntity entity) {
        return EnduranceHandler.isSparring(entity) ? Config.TRAINING_XP_MULTIPLIER.get() : 1.0;
    }

    // ---------- learning ----------

    /**
     * Scrolls and masters both come through here. A scroll for a move you already know adds move XP.
     * If the element level is too low the scroll is unreadable: the caller shows the gibberish.
     */
    public static LearnResult learn(ServerPlayer player, Ability ability, LearnSource source) {
        if (source == LearnSource.SCROLL && ability.getLearning() == Learning.MASTER_ONLY) {
            return LearnResult.MASTER_ONLY;
        }
        if (source != LearnSource.FORCE) {
            int have = getElementLevel(player, ability.getStyle());
            if (have < requiredLevel(ability, source)) return LearnResult.TOO_LOW;
        }

        ProgressionData data = data(player);
        if (data.hasLearned(ability.getId())) {
            if (source == LearnSource.SCROLL) {
                addMoveXp(player, ability, Config.MOVE_XP_PER_SCROLL.get());
                return LearnResult.GAINED_XP;
            }
            return LearnResult.ALREADY_KNOWN;
        }

        player.setData(ModAttachments.PROGRESSION, data.withMove(ability.getId(), ProgressionData.Track.START));
        return LearnResult.LEARNED;
    }

    public static void forget(ServerPlayer player, Ability ability) {
        player.setData(ModAttachments.PROGRESSION, data(player).withoutMove(ability.getId()));
    }

    // ---------- earning XP ----------

    /** A learned move was cast successfully. Gives move XP and element XP, reduced if the move is being spammed. */
    public static void onCast(ServerPlayer player, Ability ability) {
        if (!hasLearned(player, ability)) return;
        double multiplier = repeatMultiplier(player, ability.getId()) * accelerator(player);
        addMoveXp(player, ability, Config.MOVE_XP_PER_CAST.get() * multiplier);
        addElementXp(player, ability.getStyle(), Config.ELEMENT_XP_PER_CAST.get() * multiplier);
    }

    /**
     * A learned move hit a real target (a mob, a player, and later a training dummy). Call it once per hit
     * target from the move's hit code. Gives the extra XP on top of the cast XP.
     */
    public static void onMoveHit(ServerPlayer player, Ability ability, LivingEntity target) {
        if (target == player || !target.isAlive() || !hasLearned(player, ability)) return;
        double extra = Config.HIT_XP_MULTIPLIER.get() - 1.0;
        if (extra <= 0.0) return;
        double multiplier = repeatMultiplier(player, ability.getId().withSuffix("/hit")) * accelerator(player);
        addMoveXp(player, ability, Config.MOVE_XP_PER_CAST.get() * extra * multiplier);
        addElementXp(player, ability.getStyle(), Config.ELEMENT_XP_PER_CAST.get() * extra * multiplier);
    }

    /** The player defeated something. Mob levels will scale this later. */
    public static void onKill(ServerPlayer killer, LivingEntity victim) {
        if (victim == killer) return;
        if (!(victim instanceof Enemy || victim instanceof Player || victim instanceof EnduranceUser)) return;
        addPlayerXp(killer, victim.getMaxHealth() * Config.KILL_XP_PER_HEALTH.get() * mobLevelFactor(victim));
    }

    private static double mobLevelFactor(LivingEntity victim) {
        return 1.0;   // placeholder until mobs and NPCs have their own levels
    }

    public static void addPlayerXp(ServerPlayer player, double amount) {
        if (amount <= 0.0) return;
        ProgressionData data = data(player);
        Result result = advance(new ProgressionData.Track(data.playerLevel(), data.playerXp()),
                amount, Config.PLAYER_MAX_LEVEL.get(), Progression::playerXpForNext);
        player.setData(ModAttachments.PROGRESSION, data.withPlayer(result.track().level(), result.track().xp()));
        if (result.gained() > 0) {
            applyPlayerLevel(player, result.track().level());
            player.sendSystemMessage(Component.literal("Level up! You are now player level "
                    + result.track().level() + ".").withStyle(ChatFormatting.GOLD));
            levelUpSound(player);
        }
    }

    public static void addElementXp(ServerPlayer player, BendingStyle style, double amount) {
        if (amount <= 0.0) return;
        BendingStyle root = rootStyle(style);
        ResourceLocation id = root.getId();
        ProgressionData data = data(player);
        ProgressionData.Track before = data.elementTrack(id);
        Result result = advance(before, amount, Config.ELEMENT_MAX_LEVEL.get(), Progression::elementXpForNext);
        player.setData(ModAttachments.PROGRESSION, data.withElement(id, result.track()));
        if (result.gained() > 0) {
            ElementTitle oldTitle = ElementTitle.forLevel(before.level());
            ElementTitle newTitle = ElementTitle.forLevel(result.track().level());
            Component message = Component.empty()
                    .append(root.getDisplayName())
                    .append(" level " + result.track().level() + " (" + newTitle.getDisplayName() + ")");
            if (newTitle != oldTitle) {
                player.sendSystemMessage(Component.literal("Your understanding has deepened: ")
                        .append(message).withStyle(ChatFormatting.AQUA));
                levelUpSound(player);
            } else {
                player.displayClientMessage(message.copy().withStyle(ChatFormatting.AQUA), true);
            }
        }
    }

    public static void addMoveXp(ServerPlayer player, Ability ability, double amount) {
        if (amount <= 0.0 || !hasLearned(player, ability)) return;
        ProgressionData data = data(player);
        ProgressionData.Track before = data.moveTrack(ability.getId());
        Result result = advance(before, amount, Config.MOVE_HARD_CAP.get(), Progression::moveXpForNext);
        player.setData(ModAttachments.PROGRESSION, data.withMove(ability.getId(), result.track()));
        if (result.gained() > 0) {
            Component message = Component.empty()
                    .append(ability.getDisplayName())
                    .append(" reached level " + result.track().level());
            player.displayClientMessage(message.copy().withStyle(ChatFormatting.GREEN), true);
            if (before.level() < Config.MOVE_SOFT_CAP.get() && result.track().level() >= Config.MOVE_SOFT_CAP.get()) {
                player.sendSystemMessage(Component.empty().append(ability.getDisplayName())
                        .append(" is fully mastered.").withStyle(ChatFormatting.GREEN));
                levelUpSound(player);
            }
        }
    }

    // ---------- setting levels directly (commands and testing) ----------

    public static void setPlayerLevel(ServerPlayer player, int level) {
        int clamped = Mth.clamp(level, 1, Config.PLAYER_MAX_LEVEL.get());
        player.setData(ModAttachments.PROGRESSION, data(player).withPlayer(clamped, 0.0));
        applyPlayerLevel(player, clamped);
    }

    public static void setElementLevel(ServerPlayer player, BendingStyle style, int level) {
        int clamped = Mth.clamp(level, 1, Config.ELEMENT_MAX_LEVEL.get());
        player.setData(ModAttachments.PROGRESSION,
                data(player).withElement(rootStyle(style).getId(), new ProgressionData.Track(clamped, 0.0)));
    }

    /** Also marks the move as learned. */
    public static void setMoveLevel(ServerPlayer player, Ability ability, int level) {
        int clamped = Mth.clamp(level, 1, Config.MOVE_HARD_CAP.get());
        player.setData(ModAttachments.PROGRESSION,
                data(player).withMove(ability.getId(), new ProgressionData.Track(clamped, 0.0)));
    }

    // ---------- lifecycle ----------

    /** Hardcore mode: wipes all progression, and the chi and Endurance levels it gave. */
    public static void resetAll(ServerPlayer player) {
        player.setData(ModAttachments.PROGRESSION, ProgressionData.DEFAULT);
        BendingData bending = player.getData(ModAttachments.BENDING);
        player.setData(ModAttachments.BENDING, bending.withChiLevel(0));
        EnduranceData endurance = player.getData(ModAttachments.ENDURANCE);
        player.setData(ModAttachments.ENDURANCE, endurance.withLevel(0).withTrained(0.0));
    }

    /** Player level 1 is chi level 0 and Endurance level 0. */
    private static void applyPlayerLevel(ServerPlayer player, int playerLevel) {
        ChiHandler.setChiLevel(player, playerLevel - 1);
        EnduranceHandler.setLevel(player, playerLevel - 1);
    }

    // ---------- internals ----------

    private record Result(ProgressionData.Track track, int gained) {}

    private static Result advance(ProgressionData.Track start, double amount, int maxLevel, IntToDoubleFunction needed) {
        int level = start.level();
        double xp = start.xp() + amount;
        int gained = 0;
        while (level < maxLevel) {
            double need = needed.applyAsDouble(level);
            if (xp < need) break;
            xp -= need;
            level++;
            gained++;
        }
        if (level >= maxLevel) xp = 0.0;
        return new Result(new ProgressionData.Track(level, xp), gained);
    }

    /** Repeating the same move quickly builds fatigue, which cuts the XP. The fatigue fades with time. */
    private static double repeatMultiplier(ServerPlayer player, ResourceLocation key) {
        ProgressionState state = player.getData(ModAttachments.PROGRESSION_STATE);
        long now = player.level().getGameTime();
        ProgressionState.Heat heat = state.heat.computeIfAbsent(key, k -> new ProgressionState.Heat());
        double recoverTicks = Config.DIMINISH_RECOVER_SECONDS.get() * 20.0;
        heat.value = Math.max(0.0, heat.value - (now - heat.tick) / recoverTicks);
        heat.tick = now;
        double multiplier = Math.max(Config.DIMINISH_MIN.get(), 1.0 / (1.0 + heat.value * Config.DIMINISH_STRENGTH.get()));
        heat.value += 1.0;
        return multiplier;
    }

    private static void levelUpSound(ServerPlayer player) {
        player.level().playSound(null, player.blockPosition(), SoundEvents.PLAYER_LEVELUP,
                SoundSource.PLAYERS, 0.8f, 1.0f);
    }
}