
package net.prospect.otherworlds.bending.ability.air;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.prospect.otherworlds.bending.ability.*;
import net.prospect.otherworlds.bending.style.ModBendingStyles;

/** Pushes everything in front of the caster back. No damage. Replaced by Breath of Wind later. */
public class AirBlastAbility extends Ability {

    public AirBlastAbility() {
        super(properties(ModBendingStyles.AIR)
                .type(MoveType.PROJECTILE)
                .difficulty(Difficulty.EASY)
                .learning(Learning.SCROLL_OR_MASTER)
                .comboStyle(ComboStyle.STRIKE)
                .combo("LR")
                .comboWindow(200)    // TEMPORARY: 10 seconds so you can type commands. Remove with real input.
                .chiCost(10.0)
                .cooldown(40)
                .animation(AnimationSpec.strike("air_blast", 20, 8, true)));
    }

    @Override
    public boolean activate(AbilityContext ctx) {
        LivingEntity user = ctx.user();
        ServerLevel level = ctx.level();

        Vec3 look = user.getLookAngle();
        Vec3 start = user.getEyePosition();
        double range = 6.0 * ctx.quality();
        double push = 1.2 * ctx.quality();

        // Visual: a line of clouds along the blast
        for (double d = 1.0; d <= range; d += 0.75) {
            Vec3 p = start.add(look.scale(d));
            level.sendParticles(ParticleTypes.CLOUD, p.x, p.y, p.z, 2, 0.15, 0.15, 0.15, 0.01);
        }
        level.playSound(null, user.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP,
                SoundSource.PLAYERS, 1.0f, 1.4f);

        // Push anything roughly in front of the caster
        AABB area = new AABB(start, start.add(look.scale(range))).inflate(1.5);
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, area,
                e -> e != user && e.isAlive())) {
            Vec3 toTarget = target.getBoundingBox().getCenter().subtract(start);
            double dist = toTarget.length();
            if (dist > range || dist < 0.001) continue;
            if (toTarget.normalize().dot(look) < 0.8) continue;   // must be roughly in front

            target.push(look.x * push, 0.35, look.z * push);
            target.hurtMarked = true;   // makes the push show up for players
        }
        return true;
    }
}