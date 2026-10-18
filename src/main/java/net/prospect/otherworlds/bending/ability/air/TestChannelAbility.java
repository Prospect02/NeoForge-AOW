package net.prospect.otherworlds.bending.ability.air;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.LivingEntity;
import net.prospect.otherworlds.OtherWorlds;
import net.prospect.otherworlds.bending.ability.*;
import net.prospect.otherworlds.bending.style.ModBendingStyles;

/** TEMPORARY: exercises combo, charge and held phases. Delete once real moves cover them. */
public class TestChannelAbility extends Ability {

    public TestChannelAbility() {
        super(properties(ModBendingStyles.AIR)
                .type(MoveType.SELF_BUFF)
                .castType(CastType.HELD)
                .comboStyle(ComboStyle.CAST_ALONG)
                .combo("LRL")
                .charge(60)                       // up to 3 seconds
                .chiCost(2.0)
                .chiPerTick(0.25)                 // 5 chi per second
                .cooldown(20)
                .animation(AnimationSpec.of("test_channel", 300, true)));   // 15 second window, for typing commands
    }

    @Override
    public void onComboStep(AbilityContext ctx, int step, int total) {
        OtherWorlds.LOGGER.info("[TEST] combo step {}/{}", step, total);
        LivingEntity user = ctx.user();
        ctx.level().sendParticles(ParticleTypes.CLOUD, user.getX(), user.getY() + 1.0, user.getZ(),
                5 * step, 0.4, 0.4, 0.4, 0.02);
    }

    @Override
    public void tickCharge(AbilityContext ctx, double charge) {
        LivingEntity user = ctx.user();
        ctx.level().sendParticles(ParticleTypes.END_ROD, user.getX(), user.getY() + 1.0, user.getZ(),
                1 + (int) (charge * 6), 0.3, 0.3, 0.3, 0.02);
    }

    @Override
    public boolean activate(AbilityContext ctx) {
        OtherWorlds.LOGGER.info("[TEST] released: quality={}, charge={}", ctx.quality(), ctx.charge());
        return true;
    }

    @Override
    public boolean tickActive(AbilityContext ctx, int ticksActive) {
        LivingEntity user = ctx.user();
        if (ticksActive % 2 == 0) {
            ctx.level().sendParticles(ParticleTypes.CLOUD, user.getX(), user.getY() + 0.1, user.getZ(),
                    3, 0.3, 0.05, 0.3, 0.01);
        }
        return ticksActive < 200;   // ends itself after 10 seconds
    }

    @Override
    public void onEnd(AbilityContext ctx, EndReason reason) {
        OtherWorlds.LOGGER.info("[TEST] channel ended: {}", reason);
    }

    @Override
    public void onFail(AbilityContext ctx, FailReason reason) {
        OtherWorlds.LOGGER.info("[TEST] cast failed: {}", reason);
    }
}