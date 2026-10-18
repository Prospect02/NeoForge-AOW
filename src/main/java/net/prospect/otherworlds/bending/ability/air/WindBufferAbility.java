package net.prospect.otherworlds.bending.ability.air;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.prospect.otherworlds.bending.ability.*;
import net.prospect.otherworlds.bending.effect.EffectManager;
import net.prospect.otherworlds.bending.style.ModBendingStyles;

/**
 * Cast it while falling (or before a drop). It stays armed for 10 seconds and only engages
 * about 3 blocks above the ground: slows the fall and sideways momentum, and cuts fall damage.
 */
public class WindBufferAbility extends Ability {
    private static final int ARMED_TICKS = 200;
    private static final double DAMAGE_CUT = 0.7;            // 70% less fall damage at no mastery
    private static final double DAMAGE_CUT_MASTERED = 1.0;   // none at full mastery

    public WindBufferAbility() {
        super(properties(ModBendingStyles.AIR)
                .difficulty(Difficulty.EASY)
                .learning(Learning.SCROLL_OR_MASTER)
                .comboStyle(ComboStyle.STRIKE)
                .combo("LLR")
                .chiCost(12.0)
                .cooldown(100));
    }

    @Override
    public boolean activate(AbilityContext ctx) {
        LivingEntity user = ctx.user();
        ServerLevel level = ctx.level();

        double cut = Mth.lerp(Mastery.of(user, this), DAMAGE_CUT, DAMAGE_CUT_MASTERED);
        EffectManager.apply(user, EffectManager.WIND_BUFFER, ARMED_TICKS, cut);

        // No particles here: the buffer shows itself only once it starts slowing you down.
        level.playSound(null, user.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP,
                SoundSource.PLAYERS, 0.8f, 1.6f);
        return true;
    }
}