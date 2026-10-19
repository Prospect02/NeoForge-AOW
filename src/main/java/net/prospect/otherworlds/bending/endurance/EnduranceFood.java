package net.prospect.otherworlds.bending.endurance;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * How much eating a food boosts Endurance regeneration.
 * regenBoost: 0.5 means regeneration is 50 percent faster. durationSeconds: how long the boost lasts.
 * Set either to 0 to give a food no boost at all.
 */
public record EnduranceFood(double regenBoost, double durationSeconds) {
    public static final Codec<EnduranceFood> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.doubleRange(0.0, 100.0).fieldOf("regen_boost").forGetter(EnduranceFood::regenBoost),
            Codec.doubleRange(0.0, 36000.0).fieldOf("duration_seconds").forGetter(EnduranceFood::durationSeconds)
    ).apply(instance, EnduranceFood::new));
}