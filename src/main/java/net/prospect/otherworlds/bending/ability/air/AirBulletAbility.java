package net.prospect.otherworlds.bending.ability.air;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.prospect.otherworlds.bending.ability.*;
import net.prospect.otherworlds.bending.effect.EffectManager;
import net.prospect.otherworlds.bending.projectile.BendingProjectile;
import net.prospect.otherworlds.bending.style.ModBendingStyles;

/**
 * A tiny, extremely fast shot of compressed air with a long range. Tap the combo for a 50 percent shot, or hold
 * the last click: 3 seconds is 100 percent, 6 seconds is 150 percent (and it fires by itself at that point).
 * Power changes damage and a little knockback. The bullet is always just as fast.
 * Chimes mark the 100 and 150 percent points.
 * Aiming costs mobility: 25 percent slower during the combo, 50 percent slower while holding the charge.
 */
public class AirBulletAbility extends Ability {
    private static final int MAX_CHARGE_TICKS = 120;      // 6 seconds to full charge
    private static final double POWER_MIN = 0.5;          // just tapping the last click
    private static final double POWER_MAX = 1.5;          // full hold. 100 percent lands at 3 seconds on the way.

    private static final float DAMAGE = 6.0f;             // at 100 percent power: 3 for a tap, 9 for a full hold
    private static final double SPEED = 8.0;              // blocks per tick (160 per second), a real bullet
    private static final double RANGE = 80.0;             // blocks, no mastery
    private static final double RANGE_MASTERED = 128.0;   // blocks, full mastery
    private static final float KNOCKBACK = 0.15f;         // at 100 percent power

    private static final double COMBO_SLOW = 0.25;        // share of walking speed lost while casting the combo
    private static final double CHARGE_SLOW = 0.50;       // share of walking speed lost while holding the charge

    public AirBulletAbility() {
        super(properties(ModBendingStyles.AIR)
                .type(MoveType.PROJECTILE)
                .difficulty(Difficulty.MEDIUM)
                .learning(Learning.SCROLL_OR_MASTER)
                .comboStyle(ComboStyle.STRIKE)
                .combo("RLR")
                .holdCharge(MAX_CHARGE_TICKS)
                .chiCost(8.0)
                .cooldown(12)
                // The charge animation loops while you hold. The shoot animation is 12 ticks long and the bullet leaves at tick 5.
                .animation(AnimationSpec.chargedStrike("air_bullet_charge", "air_bullet_shoot", 12, 5, true)));
    }

    /** Where the air gathers: just in front of the user, at hand height. */
    private static Vec3 gatherPoint(LivingEntity user) {
        return user.getEyePosition().add(0, -0.3, 0).add(user.getLookAngle().scale(0.7));
    }

    // ---------- casting: slowed ----------

    @Override
    public void onCastStart(AbilityContext ctx) {
        EffectManager.castSlow(ctx.user(), COMBO_SLOW);
    }

    @Override
    public void tickCasting(AbilityContext ctx, int ticks, int step) {
        EffectManager.castSlow(ctx.user(), COMBO_SLOW);
    }

    /** Wrong click, timeout, being hurt or a cancel: the slow ends with the cast. */
    @Override
    public void onFail(AbilityContext ctx, FailReason reason) {
        EffectManager.clearCastSlow(ctx.user());
    }

    // ---------- charging: slowed more ----------

    @Override
    public void tickCharge(AbilityContext ctx, double charge) {
        LivingEntity user = ctx.user();
        ServerLevel level = ctx.level();
        EffectManager.castSlow(user, CHARGE_SLOW);

        Vec3 point = gatherPoint(user);

        // Air streams into the gather point: more of it, from further out, as the charge grows
        RandomSource random = user.getRandom();
        int streams = 1 + (int) (charge * 2);
        double radius = 0.5 + charge * 0.5;
        for (int i = 0; i < streams; i++) {
            Vec3 offset = new Vec3(random.nextGaussian(), random.nextGaussian(), random.nextGaussian());
            if (offset.lengthSqr() < 1.0e-4) continue;
            Vec3 from = point.add(offset.normalize().scale(radius));
            Vec3 velocity = point.subtract(from).scale(0.15);
            // Particle count 0 means: spawn one particle moving in the given direction
            level.sendParticles(ParticleTypes.CLOUD, from.x, from.y, from.z, 0, velocity.x, velocity.y, velocity.z, 1.0);
        }

        // A hum that rises as the charge builds
        if (user.tickCount % 8 == 0) {
            level.playSound(null, point.x, point.y, point.z, SoundEvents.AMETHYST_BLOCK_RESONATE,
                    SoundSource.PLAYERS, 0.4f, 0.8f + 0.8f * (float) charge);
        }

        // A chime when it reaches 100 percent (half of the charge) and 150 percent (all of it)
        long held = Math.round(charge * MAX_CHARGE_TICKS);
        if (held == MAX_CHARGE_TICKS / 2) {
            level.playSound(null, point.x, point.y, point.z, SoundEvents.EXPERIENCE_ORB_PICKUP,
                    SoundSource.PLAYERS, 0.7f, 1.2f);
        } else if (held == MAX_CHARGE_TICKS) {
            level.playSound(null, point.x, point.y, point.z, SoundEvents.EXPERIENCE_ORB_PICKUP,
                    SoundSource.PLAYERS, 0.8f, 1.7f);
        }
    }

    // ---------- firing ----------

    @Override
    public boolean activate(AbilityContext ctx) {
        LivingEntity user = ctx.user();
        ServerLevel level = ctx.level();

        // The shot is away: speed returns to normal straight away
        EffectManager.clearCastSlow(user);

        double mastery = Mastery.of(user, this);
        double charge = Mth.clamp(ctx.charge(), 0.0, 1.0);
        double power = Mth.lerp(charge, POWER_MIN, POWER_MAX);   // 0.5 for a tap, 1.0 at 3 seconds, 1.5 at 6

        float damage = DAMAGE * (float) (power * ctx.quality());
        double range = Mth.lerp(mastery, RANGE, RANGE_MASTERED);

        BendingProjectile bullet = BendingProjectile.shoot(user, level, damage, SPEED, range);
        bullet.knockback(KNOCKBACK * (float) power);

        // A sharp crack, a little lower and louder the stronger the shot
        level.playSound(null, user.blockPosition(), SoundEvents.ARROW_SHOOT,
                SoundSource.PLAYERS, (float) Mth.clamp(0.4 + power * 0.3, 0.4, 0.9), (float) (2.0 - 0.3 * power));
        return true;
    }
}