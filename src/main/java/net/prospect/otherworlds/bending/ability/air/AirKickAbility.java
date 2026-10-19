package net.prospect.otherworlds.bending.ability.air;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.prospect.otherworlds.bending.ability.*;
import net.prospect.otherworlds.bending.projectile.BendingProjectile;
import net.prospect.otherworlds.bending.style.ModBendingStyles;

/**
 * A kick of air. For now it fires the same bolt as Air Punch; the numbers below are its own,
 * so it can be rebalanced separately. Only the animation will differ.
 */
public class AirKickAbility extends Ability {
    private static final float DAMAGE = 5.0f;
    private static final double SPEED = 1.5;            // blocks per tick
    private static final double RANGE = 10.0;           // blocks, no mastery
    private static final double RANGE_MASTERED = 16.0;  // blocks, full mastery
    private static final float KNOCKBACK = 1.0f;

    public AirKickAbility() {
        super(properties(ModBendingStyles.AIR)
                .type(MoveType.PROJECTILE)
                .difficulty(Difficulty.EASY)
                .learning(Learning.SCROLL_OR_MASTER)
                .comboStyle(ComboStyle.STRIKE)
                .combo("RL")
                .chiCost(8.0)
                .cooldown(25));
    }

    @Override
    public boolean activate(AbilityContext ctx) {
        LivingEntity user = ctx.user();
        ServerLevel level = ctx.level();

        double mastery = Mastery.of(user, this);
        float damage = DAMAGE * (float) ctx.quality();
        double range = Mth.lerp(mastery, RANGE, RANGE_MASTERED);

        BendingProjectile bolt = BendingProjectile.shoot(user, level, damage, SPEED, range);
        bolt.knockback(KNOCKBACK);

        level.playSound(null, user.blockPosition(), SoundEvents.PLAYER_ATTACK_STRONG,
                SoundSource.PLAYERS, 0.9f, 1.5f);
        return true;
    }
}