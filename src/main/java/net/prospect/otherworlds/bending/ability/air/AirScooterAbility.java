package net.prospect.otherworlds.bending.ability.air;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.prospect.otherworlds.bending.ability.*;
import net.prospect.otherworlds.bending.scooter.AirScooterEntity;
import net.prospect.otherworlds.bending.style.ModBendingStyles;
import net.prospect.otherworlds.registry.ModEntities;

/**
 * Summons a rideable ball of air. The whole sequence (form, hop, sit, ride, hop off) is explained in AirScooterEntity.
 *
 * The form animation plays while the combo is entered, so its length is the combo time limit.
 * The hop and the mount animation start the moment the combo is done, which is when activate() runs.
 *
 * Top speed grows with mastery: a straight line from TOP_SPEED (just learned) to TOP_SPEED_MASTERED (fully mastered).
 */
public class AirScooterAbility extends Ability {
    // Blocks per tick. Sprinting is about 0.28, so 0.70 is about 2.5 times sprint speed and 0.94 is about 3.4 times.
    private static final double TOP_SPEED = 0.70;            // mastery 0: just learned
    private static final double TOP_SPEED_MASTERED = 0.94;   // mastery 1: fully mastered
    private static final int FORM_ANIMATION_TICKS = 40;      // 2 seconds to enter the combo

    public AirScooterAbility() {
        super(properties(ModBendingStyles.AIR)
                .type(MoveType.SELF_BUFF)
                .difficulty(Difficulty.MEDIUM)
                .learning(Learning.MASTER_ONLY)
                .comboStyle(ComboStyle.CAST_ALONG)
                .combo("LRL")
                .chiCost(10.0)
                .cooldown(40)
                .animation(AnimationSpec.of(AirScooterEntity.ANIM_FORM, FORM_ANIMATION_TICKS, true)));
    }

    @Override
    public boolean activate(AbilityContext ctx) {
        LivingEntity user = ctx.user();
        ServerLevel level = ctx.level();

        if (user.isPassenger()) {
            if (user instanceof ServerPlayer player) {
                player.displayClientMessage(Component.literal("Already riding. Jump to hop off."), true);
            }
            return false;
        }

        AirScooterEntity scooter = new AirScooterEntity(ModEntities.AIR_SCOOTER.get(), level);
        scooter.moveTo(user.getX(), user.getY(), user.getZ(), user.getYRot(), 0.0f);
        float topSpeed = (float) Mth.lerp(Mastery.of(user, this), TOP_SPEED, TOP_SPEED_MASTERED);
        scooter.beginForming(user, topSpeed);   // gives the hop
        level.addFreshEntity(scooter);

        AirScooterEntity.animate(user, AirScooterEntity.ANIM_MOUNT);

        level.playSound(null, user.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP,
                SoundSource.PLAYERS, 1.0f, 0.8f);
        return true;
    }
}