package net.prospect.otherworlds.bending.ability;

import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.Map;

/** Tracks when each ability is ready again, using the world's game time. */
public class AbilityCooldowns {
    private final Map<ResourceLocation, Long> readyAt = new HashMap<>();

    public boolean isReady(ResourceLocation ability, long now) {
        return readyAt.getOrDefault(ability, 0L) <= now;
    }

    public int ticksLeft(ResourceLocation ability, long now) {
        return (int) Math.max(0L, readyAt.getOrDefault(ability, 0L) - now);
    }

    public void start(ResourceLocation ability, long now, int ticks) {
        readyAt.put(ability, now + ticks);
    }
}