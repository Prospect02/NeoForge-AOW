package net.prospect.otherworlds.bending.ability;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;

/** Everything a move needs to know about this cast. charge is 0..1 and only matters for charged moves. */
public record AbilityContext(LivingEntity user, ServerLevel level, double quality, double charge) {
    public AbilityContext(LivingEntity user, ServerLevel level, double quality) {
        this(user, level, quality, 0.0);
    }
}