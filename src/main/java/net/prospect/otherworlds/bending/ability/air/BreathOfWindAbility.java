package net.prospect.otherworlds.bending.ability.air;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.prospect.otherworlds.bending.ability.*;
import net.prospect.otherworlds.bending.knockback.BendingKnockback;
import net.prospect.otherworlds.bending.style.ModBendingStyles;

/**
 * The caster breathes in, then blows a wide gust in front of them. The gust throws creatures and pushes
 * dropped items away. It does no damage itself, but a creature thrown into a wall while it is still moving
 * fast takes bending damage (see BendingKnockback). Closer targets are pushed harder.
 *
 * Hold the last click to breathe in (the inhale animation loops while you hold). While you inhale, creatures
 * and items in front of you are pulled toward your mouth, harder the longer you hold. Letting go plays the
 * exhale. A tap is 10 percent power, 100 percent is the gust as tuned below, and a full 4 second hold is
 * 150 percent.
 */
public class BreathOfWindAbility extends Ability {
    private static final int MAX_CHARGE_TICKS = 80;       // 4 seconds to full charge
    private static final double POWER_MIN = 0.10;         // just tapping the last click
    private static final double POWER_MAX = 1.50;         // full hold

    // The gust, at 100 percent power
    private static final double RANGE = 7.0;              // blocks, no mastery
    private static final double RANGE_MASTERED = 10.0;    // blocks, full mastery
    private static final double CONE_DOT = 0.5;           // how wide: 0.5 is 60 degrees to each side
    private static final double PUSH = 1.6;               // blocks per tick of speed, point blank
    private static final double PUSH_MASTERED = 2.4;
    private static final double LIFT = 0.45;

    // The suck while inhaling
    private static final double PULL_RANGE_MIN = 4.0;     // blocks, just started inhaling
    private static final double PULL_RANGE_MAX = 9.0;     // blocks, full charge
    private static final double PULL_MIN = 0.02;          // speed added each tick, just started
    private static final double PULL_MAX = 0.14;          // speed added each tick, full charge
    private static final double PULL_CONE_DOT = 0.2;      // how wide the suck reaches in front (0.2 is about 100 degrees each side)
    private static final double PULL_STOP_DISTANCE = 1.2; // closer than this is left alone, so nothing jitters at your face

    public BreathOfWindAbility() {
        super(properties(ModBendingStyles.AIR)
                .type(MoveType.WAVE)
                .difficulty(Difficulty.EASY)
                .learning(Learning.SCROLL_OR_MASTER)
                .comboStyle(ComboStyle.STRIKE)
                .combo("LR")
                .holdCharge(MAX_CHARGE_TICKS)
                .chiCost(10.0)
                .cooldown(40)
                // Inhale loops while you hold. The exhale is 20 ticks long and the breath leaves at tick 8.
                .animation(AnimationSpec.chargedStrike("breath_of_wind_inhale", "breath_of_wind_exhale", 20, 8, true)));
    }

    /** Where the breath comes from: just in front of the face. */
    private static Vec3 mouth(LivingEntity user) {
        return user.getEyePosition().add(0, -0.15, 0).add(user.getLookAngle().scale(0.25));
    }

    @Override
    public void tickCharge(AbilityContext ctx, double charge) {
        LivingEntity user = ctx.user();
        ServerLevel level = ctx.level();
        Vec3 mouth = mouth(user);
        Vec3 look = user.getLookAngle();

        // Air streams in toward the mouth: more of it, from further out, as the charge grows
        RandomSource random = user.getRandom();
        int streams = 1 + (int) (charge * 3);
        double radius = 1.2 + charge * 1.3;
        for (int i = 0; i < streams; i++) {
            Vec3 offset = new Vec3(random.nextGaussian(), random.nextGaussian(), random.nextGaussian());
            if (offset.lengthSqr() < 1.0e-4) continue;
            Vec3 from = mouth.add(offset.normalize().scale(radius));
            Vec3 velocity = mouth.subtract(from).scale(0.12);   // reaches the mouth in a few ticks
            // Particle count 0 means: spawn one particle moving in the given direction
            level.sendParticles(ParticleTypes.CLOUD, from.x, from.y, from.z, 0, velocity.x, velocity.y, velocity.z, 1.0);
        }

        // A breath in: the pitch rises as the lungs fill
        if (user.tickCount % 12 == 0) {
            level.playSound(null, mouth.x, mouth.y, mouth.z, SoundEvents.PLAYER_BREATH, SoundSource.PLAYERS,
                    0.5f, 0.6f + 0.6f * (float) charge);
        }

        // The suck: drag creatures and items in front of you toward your mouth. Reach and pull grow with the charge.
        double pullRange = Mth.lerp(charge, PULL_RANGE_MIN, PULL_RANGE_MAX);
        double pullSpeed = Mth.lerp(charge, PULL_MIN, PULL_MAX);
        AABB area = new AABB(mouth, mouth).inflate(pullRange);
        for (Entity target : level.getEntities(user, area,
                e -> e.isAlive() && !e.isSpectator() && (e instanceof LivingEntity || e instanceof ItemEntity))) {
            Vec3 toMouth = mouth.subtract(target.getBoundingBox().getCenter());
            double dist = toMouth.length();
            if (dist > pullRange || dist < PULL_STOP_DISTANCE) continue;
            if (toMouth.reverse().normalize().dot(look) < PULL_CONE_DOT) continue;   // only what is in front of you

            double strength = pullSpeed * (1.0 - 0.5 * (dist / pullRange));   // closer is pulled harder
            target.setDeltaMovement(target.getDeltaMovement().add(toMouth.normalize().scale(strength)));
            target.hurtMarked = true;   // makes the pull show up for players
        }
    }

    @Override
    public boolean activate(AbilityContext ctx) {
        LivingEntity user = ctx.user();
        ServerLevel level = ctx.level();

        double mastery = Mastery.of(user, this);
        double charge = Mth.clamp(ctx.charge(), 0.0, 1.0);
        double power = Mth.lerp(charge, POWER_MIN, POWER_MAX);   // 0.10 to 1.50

        // Reach grows more gently than the push: about 55 percent at a tap, 125 percent at a full hold
        double range = Mth.lerp(mastery, RANGE, RANGE_MASTERED) * (0.5 + 0.5 * power);
        double push = Mth.lerp(mastery, PUSH, PUSH_MASTERED) * power * ctx.quality();
        double lift = LIFT * Mth.clamp(power, 0.3, 1.5);

        Vec3 look = user.getLookAngle();
        Vec3 start = mouth(user);

        // Push everything roughly in front of the caster, softer the further away it is
        AABB area = new AABB(start, start.add(look.scale(range))).inflate(range * 0.5);
        for (Entity target : level.getEntities(user, area,
                e -> e.isAlive() && (e instanceof LivingEntity || e instanceof ItemEntity))) {
            Vec3 toTarget = target.getBoundingBox().getCenter().subtract(start);
            double dist = toTarget.length();
            if (dist > range || dist < 0.001) continue;
            if (toTarget.normalize().dot(look) < CONE_DOT) continue;

            double strength = push * (1.0 - 0.5 * (dist / range));
            if (target instanceof LivingEntity living) {
                // Faster launch, extra braking, and wall damage
                BendingKnockback.knock(living, user, look.x * strength, lift, look.z * strength);
            } else {
                target.push(look.x * strength, lift, look.z * strength);
                target.hurtMarked = true;
            }
        }

        // Visual: rings of cloud that widen with distance, like a cone
        Vec3 right = look.cross(new Vec3(0, 1, 0));
        if (right.lengthSqr() < 1.0e-4) right = new Vec3(1, 0, 0);   // looking straight up or down
        right = right.normalize();
        Vec3 up = right.cross(look).normalize();
        for (double d = 1.0; d <= range; d += 1.0) {
            double radius = d * 0.5;
            for (int i = 0; i < 6; i++) {
                double angle = Math.toRadians(i * 60.0);
                Vec3 offset = right.scale(Math.cos(angle) * radius).add(up.scale(Math.sin(angle) * radius));
                Vec3 p = start.add(look.scale(d)).add(offset);
                level.sendParticles(ParticleTypes.CLOUD, p.x, p.y, p.z, 1, 0.05, 0.05, 0.05, 0.02);
            }
        }
        level.playSound(null, user.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP,
                SoundSource.PLAYERS, (float) Mth.clamp(0.4 + power * 0.5, 0.4, 1.2), (float) (1.6 - 0.5 * charge));
        return true;
    }
}