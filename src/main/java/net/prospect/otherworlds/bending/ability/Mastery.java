package net.prospect.otherworlds.bending.ability;

import net.minecraft.world.entity.LivingEntity;

/**
 * How well an entity has mastered a move, from 0.0 (just learned) to 1.0 (fully mastered).
 * Placeholder until the progression phase. Moves read their numbers through this, so when
 * real mastery arrives, only this one method changes.
 */
public final class Mastery {
    private Mastery() {}

    public static double of(LivingEntity user, Ability ability) {
        return 0.0;
    }
}