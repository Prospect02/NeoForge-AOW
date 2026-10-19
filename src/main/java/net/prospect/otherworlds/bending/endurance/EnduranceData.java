package net.prospect.otherworlds.bending.endurance;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * One entity's Endurance, as saved. Immutable: build a changed copy with a "with" method.
 * Current is precise. Max comes from the attributes, which are worked out from level, rolls and training.
 */
public record EnduranceData(
        double current,
        int level,          // Endurance progression, raised by mastery in later phases
        double trained,     // max Endurance earned through sparring, taking hits and using moves
        float maxRoll,      // -1..1, rolled once on first join
        float regenRoll,    // -1..1, rolled once on first join
        boolean rolled
) {
    public static final EnduranceData DEFAULT = new EnduranceData(0.0, 0, 0.0, 0.0f, 0.0f, false);

    public static final Codec<EnduranceData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.DOUBLE.optionalFieldOf("current", 0.0).forGetter(EnduranceData::current),
            Codec.INT.optionalFieldOf("level", 0).forGetter(EnduranceData::level),
            Codec.DOUBLE.optionalFieldOf("trained", 0.0).forGetter(EnduranceData::trained),
            Codec.FLOAT.optionalFieldOf("max_roll", 0.0f).forGetter(EnduranceData::maxRoll),
            Codec.FLOAT.optionalFieldOf("regen_roll", 0.0f).forGetter(EnduranceData::regenRoll),
            Codec.BOOL.optionalFieldOf("rolled", false).forGetter(EnduranceData::rolled)
    ).apply(instance, EnduranceData::new));

    public EnduranceData withCurrent(double current) {
        return new EnduranceData(current, level, trained, maxRoll, regenRoll, rolled);
    }

    public EnduranceData withLevel(int level) {
        return new EnduranceData(current, Math.max(0, level), trained, maxRoll, regenRoll, rolled);
    }

    public EnduranceData withTrained(double trained) {
        return new EnduranceData(current, level, Math.max(0.0, trained), maxRoll, regenRoll, rolled);
    }

    public EnduranceData withRolls(float maxRoll, float regenRoll) {
        return new EnduranceData(current, level, trained, maxRoll, regenRoll, true);
    }
}