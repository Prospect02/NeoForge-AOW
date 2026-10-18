package net.prospect.otherworlds.bending.ability;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;

/** Everything a move needs to know about this cast. */
public record AbilityContext(LivingEntity user, ServerLevel level, double quality) {
}