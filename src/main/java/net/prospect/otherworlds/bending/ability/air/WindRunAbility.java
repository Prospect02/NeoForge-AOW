package net.prospect.otherworlds.bending.ability.air;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.prospect.otherworlds.bending.ability.*;
import net.prospect.otherworlds.bending.effect.EffectManager;
import net.prospect.otherworlds.bending.style.ModBendingStyles;

/**
 * Finish the combo while sprinting to start a run. The run itself is a timed effect (EffectManager),
 * so the player can still cast other moves while running. It ends when sprint is released,
 * hunger gets too low to sprint, or chi runs out.
 */
public class WindRunAbility extends Ability {
    private static final double TOP_SPEED_BONUS = 0.15;            // +100%: double sprint speed
    private static final double TOP_SPEED_BONUS_MASTERED = 0.3;   // +200%: triple sprint speed

    public WindRunAbility() {
        super(properties(ModBendingStyles.AIR)
                .difficulty(Difficulty.EASY)
                .learning(Learning.SCROLL_OR_MASTER)
                .comboStyle(ComboStyle.STRIKE)
                .combo("LRL")
                .chiCost(5.0)
                .cooldown(20));
    }

    @Override
    public boolean activate(AbilityContext ctx) {
        LivingEntity user = ctx.user();
        ServerLevel level = ctx.level();

        if (!user.isSprinting()) {
            if (user instanceof ServerPlayer player) {
                player.displayClientMessage(Component.literal("Wind Run needs you to be sprinting."), true);
            }
            return false;
        }

        double boost = Mth.lerp(Mastery.of(user, this), TOP_SPEED_BONUS, TOP_SPEED_BONUS_MASTERED);
        EffectManager.apply(user, EffectManager.WIND_RUN, Integer.MAX_VALUE / 2, boost);

        Vec3 p = user.position();
        level.sendParticles(ParticleTypes.CLOUD, p.x, p.y + 0.1, p.z, 15, 0.4, 0.1, 0.4, 0.05);
        level.playSound(null, user.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP,
                SoundSource.PLAYERS, 1.0f, 1.2f);
        return true;
    }
}