package net.prospect.otherworlds.bending.ability;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.prospect.otherworlds.bending.progression.Progression;

/**
 * How well an entity has mastered a move, from 0.0 (just learned, move level 1) to 1.0 (move level 50).
 * Every move reads its numbers (damage, speed, size, range, duration) through this one method.
 * Mobs and NPCs stay at 0.0 until they get levels.
 */
public final class Mastery {
    private Mastery() {}

    public static double of(LivingEntity user, Ability ability) {
        if (user instanceof Player player) return Progression.moveMastery(player, ability);
        return 0.0;
    }
}