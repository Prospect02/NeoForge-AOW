package net.prospect.otherworlds.bending.effect;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.prospect.otherworlds.OtherWorlds;
import net.prospect.otherworlds.bending.chi.ChiHandler;
import net.prospect.otherworlds.bending.effect.ActiveEffects.State;
import net.prospect.otherworlds.bending.util.Motion;
import net.prospect.otherworlds.registry.ModAttachments;

import java.util.ArrayList;

/** Applies, ticks and removes timed effects. Server side only. */
public final class EffectManager {
    private EffectManager() {}

    public static final ResourceLocation WIND_BUFFER = OtherWorlds.id("wind_buffer");
    public static final ResourceLocation WIND_RUN = OtherWorlds.id("wind_run");
    public static final ResourceLocation CAST_SLOW = OtherWorlds.id("cast_slow");

    // Wind Buffer tuning. These move into Config later.
    private static final double BUFFER_ENGAGE_HEIGHT = 3.0;     // blocks above the ground
    private static final double BUFFER_SIDEWAYS_DAMPING = 0.9;  // sideways speed kept per tick

    // Wind Run tuning. state.strength is the top speed bonus (0.15 = +15% speed).
    private static final ResourceLocation WIND_RUN_MODIFIER = OtherWorlds.id("wind_run_speed");
    private static final int WIND_RUN_RAMP_TICKS = 20;          // time to reach top speed
    private static final double WIND_RUN_CHI_PER_TICK = 0.3;
    private static final int FOOD_NEEDED_TO_SPRINT = 6;         // vanilla stops sprinting at 6 or less

    // Cast slow. state.strength is the share of walking speed lost (0.25 = 25 percent slower).
    private static final ResourceLocation CAST_SLOW_MODIFIER = OtherWorlds.id("cast_slow_speed");
    private static final int CAST_SLOW_TICKS = 3;               // renewed every tick while casting, so it ends soon after

    public static void apply(LivingEntity entity, ResourceLocation id, int ticks, double strength) {
        entity.getData(ModAttachments.EFFECTS).add(id, ticks, strength);
    }

    public static boolean has(LivingEntity entity, ResourceLocation id) {
        return entity.hasData(ModAttachments.EFFECTS) && entity.getData(ModAttachments.EFFECTS).has(id);
    }

    public static void remove(LivingEntity entity, ResourceLocation id) {
        if (!entity.hasData(ModAttachments.EFFECTS)) return;
        ActiveEffects effects = entity.getData(ModAttachments.EFFECTS);
        if (effects.has(id)) {
            effects.remove(id);
            onRemoved(entity, id);
        }
    }

    /**
     * Slows the entity while it casts. Call this every tick of the cast (for example from tickCasting and
     * tickCharge): it runs out by itself a few ticks after the last call. fraction 0.25 is 25 percent slower.
     */
    public static void castSlow(LivingEntity entity, double fraction) {
        apply(entity, CAST_SLOW, CAST_SLOW_TICKS, fraction);
    }

    /** Ends the cast slow right away, for when the move fires or the cast fails. */
    public static void clearCastSlow(LivingEntity entity) {
        remove(entity, CAST_SLOW);
    }

    public static void tick(LivingEntity entity) {
        if (!(entity.level() instanceof ServerLevel)) return;
        if (!entity.hasData(ModAttachments.EFFECTS)) return;
        ActiveEffects effects = entity.getData(ModAttachments.EFFECTS);
        if (effects.isEmpty()) return;

        for (ResourceLocation id : new ArrayList<>(effects.ids())) {
            State state = effects.get(id);
            if (state == null) continue;

            Vec3 pos = entity.position();
            state.velocity = state.lastPos == null ? Vec3.ZERO : pos.subtract(state.lastPos);
            state.lastPos = pos;
            state.age++;

            boolean keep = true;
            if (id.equals(WIND_BUFFER)) keep = tickWindBuffer(entity, state);
            else if (id.equals(WIND_RUN)) keep = tickWindRun(entity, state);
            else if (id.equals(CAST_SLOW)) keep = tickCastSlow(entity, state);

            if (!keep || --state.ticksLeft <= 0) {
                effects.remove(id);
                onRemoved(entity, id);
            }
        }
    }

    /** Cleanup when an effect ends for any reason. */
    private static void onRemoved(LivingEntity entity, ResourceLocation id) {
        if (id.equals(WIND_RUN)) {
            AttributeInstance speed = entity.getAttribute(Attributes.MOVEMENT_SPEED);
            if (speed != null) speed.removeModifier(WIND_RUN_MODIFIER);
        } else if (id.equals(CAST_SLOW)) {
            AttributeInstance speed = entity.getAttribute(Attributes.MOVEMENT_SPEED);
            if (speed != null) speed.removeModifier(CAST_SLOW_MODIFIER);
        }
    }

    // ---------- Wind Buffer ----------

    /** Returns false when the effect is spent. */
    private static boolean tickWindBuffer(LivingEntity entity, State state) {
        if (entity.onGround()) {
            // Landed: a buffer that saw the air is spent. One cast while standing keeps waiting.
            return !state.airborne;
        }
        state.airborne = true;

        Vec3 v = state.velocity;
        if (v.y > -0.1) return true;   // not falling yet

        double ground = Motion.groundDistance(entity, BUFFER_ENGAGE_HEIGHT + 1.0);
        if (ground > BUFFER_ENGAGE_HEIGHT) return true;   // still too high: stay armed

        state.engaged = true;
        double slowestFall = -(0.15 + 0.12 * ground);   // gentler the closer the ground gets
        double newY = Math.max(v.y, slowestFall);
        Motion.setVelocity(entity, new Vec3(
                v.x * BUFFER_SIDEWAYS_DAMPING, newY, v.z * BUFFER_SIDEWAYS_DAMPING));
        entity.fallDistance = 0;

        // The visible sign that the buffer has engaged
        if (entity.level() instanceof ServerLevel level) {
            level.sendParticles(ParticleTypes.CLOUD, entity.getX(), entity.getY() + 0.1, entity.getZ(),
                    3, 0.3, 0.05, 0.3, 0.02);
        }
        return true;
    }

    // ---------- Wind Run ----------

    /** Returns false when the run is over: sprint released, too hungry, or out of chi. */
    private static boolean tickWindRun(LivingEntity entity, State state) {
        if (!entity.isSprinting()) return false;
        if (entity instanceof Player player
                && player.getFoodData().getFoodLevel() <= FOOD_NEEDED_TO_SPRINT
                && !player.getAbilities().mayfly) {
            return false;
        }
        if (entity instanceof ServerPlayer serverPlayer
                && !ChiHandler.trySpend(serverPlayer, WIND_RUN_CHI_PER_TICK)) {
            return false;
        }

        // Ramp up to the top speed. The modifier is only touched while ramping,
        // so it isn't resent to the client every tick afterwards.
        if (state.age <= WIND_RUN_RAMP_TICKS) {
            double ramp = Math.min(1.0, state.age / (double) WIND_RUN_RAMP_TICKS);
            AttributeInstance speed = entity.getAttribute(Attributes.MOVEMENT_SPEED);
            if (speed != null) {
                speed.addOrUpdateTransientModifier(new AttributeModifier(
                        WIND_RUN_MODIFIER, state.strength * ramp,
                        AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
            }
        }

        if (entity.tickCount % 2 == 0 && entity.level() instanceof ServerLevel level) {
            level.sendParticles(ParticleTypes.CLOUD, entity.getX(), entity.getY() + 0.1, entity.getZ(),
                    1, 0.2, 0.05, 0.2, 0.01);
        }
        return true;
    }

    // ---------- Cast slow ----------

    /** Keeps the slow on. The modifier is only changed when the amount changes, so it isn't resent every tick. */
    private static boolean tickCastSlow(LivingEntity entity, State state) {
        AttributeInstance speed = entity.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed == null) return true;

        double wanted = -state.strength;
        AttributeModifier current = speed.getModifier(CAST_SLOW_MODIFIER);
        if (current == null || Math.abs(current.amount() - wanted) > 1.0e-9) {
            speed.addOrUpdateTransientModifier(new AttributeModifier(
                    CAST_SLOW_MODIFIER, wanted, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        }
        return true;
    }
}