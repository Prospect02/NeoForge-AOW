package net.prospect.otherworlds.bending.ability.air;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.prospect.otherworlds.OtherWorlds;
import net.prospect.otherworlds.bending.ability.*;
import net.prospect.otherworlds.bending.style.ModBendingStyles;
import net.prospect.otherworlds.bending.util.Motion;

/** Combo, then hold the last click to charge. Letting go launches you. Longer hold = higher and faster. */
public class WindJumpAbility extends Ability {
    // Launch speeds are blocks per tick of upward velocity: 0.7 is about 3 blocks, 1.3 about 9, 1.7 about 15.
    private static final double MIN_LAUNCH = 0.7;              // a tap
    private static final double MAX_LAUNCH = 1.3;              // full hold, no mastery
    private static final double MAX_LAUNCH_MASTERED = 1.7;     // full hold, full mastery
    private static final double MIN_FORWARD = 0.25;
    private static final double MAX_FORWARD = 0.55;
    private static final double FORWARD_MASTERY_BONUS = 0.5;   // +50% forward speed at full mastery

    public WindJumpAbility() {
        super(properties(ModBendingStyles.AIR)
                .difficulty(Difficulty.EASY)
                .learning(Learning.SCROLL_OR_MASTER)
                .comboStyle(ComboStyle.STRIKE)
                .combo("RR")
                .holdCharge(30)          // 1.5 seconds to full charge
                .chiCost(15.0)
                .cooldown(60));
    }

    @Override
    public void tickCharge(AbilityContext ctx, double charge) {
        LivingEntity user = ctx.user();
        if (user.tickCount % 2 != 0) return;
        Vec3 p = user.position();
        int count = 1 + (int) (charge * 4);   // clouds gather faster as the charge grows
        ctx.level().sendParticles(ParticleTypes.CLOUD, p.x, p.y + 0.1, p.z, count, 0.3, 0.05, 0.3, 0.02);
    }

    @Override
    public boolean activate(AbilityContext ctx) {
        OtherWorlds.LOGGER.info("[DEBUG] wind jump activate: charge={} onGround={}", ctx.charge(), ctx.user().onGround());
        LivingEntity user = ctx.user();
        ServerLevel level = ctx.level();

        // Must be standing on something (a small forgiveness for tiny hops while charging)
        if (!user.onGround() && Motion.groundDistance(user, 1.0) > 0.6) return false;

        double mastery = Mastery.of(user, this);
        double charge = Mth.clamp(ctx.charge(), 0.0, 1.0);

        double maxLaunch = Mth.lerp(mastery, MAX_LAUNCH, MAX_LAUNCH_MASTERED);
        double up = Mth.lerp(charge, MIN_LAUNCH, maxLaunch);
        double forward = Mth.lerp(charge, MIN_FORWARD, MAX_FORWARD) * (1.0 + FORWARD_MASTERY_BONUS * mastery);

        Vec3 dir = Motion.horizontalLook(user);
        Motion.setVelocity(user, new Vec3(dir.x * forward, up, dir.z * forward));
        user.fallDistance = 0;

        Vec3 p = user.position();
        level.sendParticles(ParticleTypes.CLOUD, p.x, p.y + 0.1, p.z, 20, 0.4, 0.1, 0.4, 0.05);
        level.playSound(null, user.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP,
                SoundSource.PLAYERS, 1.0f, 0.7f);
        return true;
    }
}