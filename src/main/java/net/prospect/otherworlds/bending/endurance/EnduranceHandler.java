package net.prospect.otherworlds.bending.endurance;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.common.NeoForge;
import net.prospect.otherworlds.Config;
import net.prospect.otherworlds.OtherWorlds;
import net.prospect.otherworlds.bending.damage.ModDamageTypes;
import net.prospect.otherworlds.registry.ModAttachments;
import net.prospect.otherworlds.registry.ModAttributes;

/**
 * A second health pool that takes combat damage before hearts. Works for players and any NPC
 * that implements EnduranceUser. Server side only.
 *
 * Max Endurance is always a whole number. Current Endurance and damage work in exact decimals,
 * the same way vanilla health does.
 */
public final class EnduranceHandler {
    private EnduranceHandler() {}

    /** The player-model UI will have this many damage stages (0 = untouched), plus one for depleted. */
    public static final int STAGES = 5;

    private static final ResourceLocation MAX_NATURAL_ID = OtherWorlds.id("endurance_natural");
    private static final ResourceLocation REGEN_NATURAL_ID = OtherWorlds.id("endurance_regen_natural");

    // ---------- reading ----------

    public static boolean hasEndurance(LivingEntity entity) {
        return entity instanceof Player || entity instanceof EnduranceUser;
    }

    /** Always a whole number, whatever modifiers are on the attribute. */
    public static double getMax(LivingEntity entity) {
        AttributeInstance attribute = entity.getAttribute(ModAttributes.MAX_ENDURANCE);
        if (attribute != null) return Math.round(attribute.getValue());
        if (entity instanceof EnduranceUser user) return Math.round(user.enduranceProfile().maxEndurance());
        return 0.0;
    }

    /** Points per second. */
    public static double getRegen(LivingEntity entity) {
        AttributeInstance attribute = entity.getAttribute(ModAttributes.ENDURANCE_REGEN);
        if (attribute != null) return round2(attribute.getValue());
        if (entity instanceof EnduranceUser user) return round2(user.enduranceProfile().regenPerSecond());
        return 0.0;
    }

    public static double getCurrent(LivingEntity entity) {
        return entity.getData(ModAttachments.ENDURANCE).current();
    }

    /** 0 = untouched up to STAGES - 1, then STAGES when fully depleted. This is what the UI will show. */
    public static int getStage(LivingEntity entity) {
        double max = getMax(entity);
        if (max <= 0.0) return 0;
        double current = getCurrent(entity);
        if (current <= 0.0) return STAGES;
        double fraction = Mth.clamp(current / max, 0.0, 1.0);
        return Math.min(STAGES - 1, (int) Math.floor((1.0 - fraction) * STAGES));
    }

    public static boolean isSparring(LivingEntity entity) {
        return entity.getData(ModAttachments.ENDURANCE_STATE).sparring;
    }

    // ---------- changing (server side only) ----------

    public static void setCurrent(LivingEntity entity, double value) {
        double clamped = Mth.clamp(value, 0.0, getMax(entity));
        EnduranceData data = entity.getData(ModAttachments.ENDURANCE);
        if (clamped != data.current()) {
            entity.setData(ModAttachments.ENDURANCE, data.withCurrent(clamped));
        }
    }

    public static void setLevel(LivingEntity entity, int level) {
        EnduranceData data = entity.getData(ModAttachments.ENDURANCE);
        entity.setData(ModAttachments.ENDURANCE, data.withLevel(level));
        refreshAttributes(entity);
        setCurrent(entity, getCurrent(entity));
    }

    public static void setSparring(LivingEntity entity, boolean sparring) {
        entity.getData(ModAttachments.ENDURANCE_STATE).sparring = sparring;
    }

    /** Makes regeneration wait again, as if the entity had just been hit. */
    public static void markHit(LivingEntity entity) {
        entity.getData(ModAttachments.ENDURANCE_STATE).lastHitTick = entity.level().getGameTime();
    }

    /**
     * Adds to the trained bonus on max Endurance. Gains shrink as the bonus nears its cap,
     * so it cannot be farmed forever.
     */
    public static void train(LivingEntity entity, double amount) {
        if (amount <= 0.0) return;
        EnduranceData data = entity.getData(ModAttachments.ENDURANCE);
        double cap = Config.ENDURANCE_TRAINED_CAP.get();
        double gain = amount * Math.max(0.0, 1.0 - data.trained() / cap);
        if (gain <= 0.0) return;
        entity.setData(ModAttachments.ENDURANCE, data.withTrained(Math.min(cap, data.trained() + gain)));
        refreshAttributes(entity);
    }

    // ---------- lifecycle ----------

    /** Call on login (players) or on spawn (NPCs). Rolls on first join, applies the attributes, fills the pool. */
    public static void initialize(LivingEntity entity) {
        if (!hasEndurance(entity)) return;
        EnduranceData data = entity.getData(ModAttachments.ENDURANCE);
        boolean first = !data.rolled();

        if (first) {
            if (entity instanceof Player) {
                RandomSource random = entity.getRandom();
                data = data.withRolls(rollVariance(random), rollVariance(random));
            } else {
                data = data.withRolls(0.0f, 0.0f);   // NPCs use their fixed profile
            }
            entity.setData(ModAttachments.ENDURANCE, data);
        }

        refreshAttributes(entity);

        if (first) {
            setCurrent(entity, getMax(entity));   // new characters start full
        } else {
            setCurrent(entity, getCurrent(entity));   // re-clamp in case max went down
        }
    }

    /** Call after respawn. Refills the pool and ends any spar. */
    public static void onRespawn(LivingEntity entity) {
        if (!hasEndurance(entity)) return;
        setSparring(entity, false);
        refreshAttributes(entity);
        setCurrent(entity, getMax(entity));
    }

    /**
     * Recalculates max Endurance and regen from level, rolls and training.
     * The ONE place the scaling formulas live. Safe to call any number of times.
     */
    public static void refreshAttributes(LivingEntity entity) {
        EnduranceData data = entity.getData(ModAttachments.ENDURANCE);

        // Max: (base + growth) * (1 + roll * variance) + trained, rounded to a whole number
        AttributeInstance max = entity.getAttribute(ModAttributes.MAX_ENDURANCE);
        if (max != null) {
            double base = max.getBaseValue();
            double growth = data.level() * Config.ENDURANCE_PER_LEVEL.get();
            double variance = 1.0 + data.maxRoll() * Config.ENDURANCE_VARIANCE_PERCENT.get() / 100.0;
            double total = Math.round((base + growth) * variance + data.trained());   // whole points only
            setNaturalModifier(max, MAX_NATURAL_ID, total - base);
        }

        // Regen: (base + growth) * (1 + roll * variance)
        AttributeInstance regen = entity.getAttribute(ModAttributes.ENDURANCE_REGEN);
        if (regen != null) {
            double base = regen.getBaseValue();
            double growth = data.level() * Config.ENDURANCE_REGEN_PER_LEVEL.get();
            double variance = 1.0 + data.regenRoll() * Config.ENDURANCE_REGEN_VARIANCE_PERCENT.get() / 100.0;
            double total = round2((base + growth) * variance);
            setNaturalModifier(regen, REGEN_NATURAL_ID, total - base);
        }
    }

    /** Every server tick. Regenerates after a quiet spell, never during a spar. */
    public static void tick(LivingEntity entity) {
        if (!hasEndurance(entity)) return;
        EnduranceState state = entity.getData(ModAttachments.ENDURANCE_STATE);
        if (state.sparring) return;

        double max = getMax(entity);
        double current = getCurrent(entity);
        if (current >= max) return;

        double delayTicks = Config.ENDURANCE_REGEN_DELAY_SECONDS.get() * 20.0;
        if (entity.level().getGameTime() - state.lastHitTick < delayTicks) return;

        setCurrent(entity, current + getRegen(entity) / 20.0);   // 20 ticks per second
    }

    // ---------- damage ----------

    /**
     * Takes some or all of a hit. incoming is the damage after armor and enchantments.
     * Works in exact decimals like vanilla health. Returns how much should still reach hearts.
     */
    public static float absorb(LivingEntity entity, DamageSource source, float incoming) {
        if (incoming <= 0.0f || !hasEndurance(entity)) return incoming;
        // /kill and the void always work
        if (source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) return incoming;

        EnduranceState state = entity.getData(ModAttachments.ENDURANCE_STATE);
        boolean sparring = state.sparring;

        // Not combat damage: skips Endurance. In a spar, hearts are untouchable, so it does nothing.
        if (!source.is(ModDamageTypes.DRAINS_ENDURANCE)) return sparring ? 0.0f : incoming;

        EnduranceData data = entity.getData(ModAttachments.ENDURANCE);
        double absorbed = Math.min(data.current(), incoming);
        double remaining = data.current() - absorbed;
        entity.setData(ModAttachments.ENDURANCE, data.withCurrent(remaining));

        state.lastHitTick = entity.level().getGameTime();
        state.lastIncoming = incoming;
        state.lastAbsorbed = absorbed;

        train(entity, absorbed * Config.ENDURANCE_TRAIN_PER_DAMAGE.get());

        if (absorbed > 0.0 && remaining <= 0.0) {
            NeoForge.EVENT_BUS.post(new EnduranceDepletedEvent(entity, source));
        }

        return sparring ? 0.0f : (float) (incoming - absorbed);
    }

    // ---------- helpers ----------

    public static double round2(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    /** Most players land near the middle of the range, and only a few reach the edges. */
    private static float rollVariance(RandomSource random) {
        return Mth.clamp((float) random.nextGaussian() * 0.4f, -1.0f, 1.0f);
    }

    private static void setNaturalModifier(AttributeInstance instance, ResourceLocation id, double bonus) {
        instance.addOrReplacePermanentModifier(
                new AttributeModifier(id, bonus, AttributeModifier.Operation.ADD_VALUE));
    }
}