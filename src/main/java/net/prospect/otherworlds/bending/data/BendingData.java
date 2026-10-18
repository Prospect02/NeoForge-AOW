package net.prospect.otherworlds.bending.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;

import java.util.Optional;

/**
 * Everything the mod remembers about one player's bending.
 * Immutable: to change something, build a modified copy with a "with" method
 * and store it back on the player.
 */
public record BendingData(
        double chi,
        int chiLevel,           // chi progression; raised by mastery in later phases
        float maxChiRoll,       // -1..1, rolled once on first join
        float regenRoll,        // -1..1, rolled once on first join
        boolean rollsAssigned,
        Optional<ResourceLocation> affinity
) {
    public static final BendingData DEFAULT =
            new BendingData(0.0, 0, 0.0f, 0.0f, false, Optional.empty());

    // Every field is optional with a default, so older saves still load.
    public static final Codec<BendingData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.DOUBLE.optionalFieldOf("chi", 0.0).forGetter(BendingData::chi),
            Codec.INT.optionalFieldOf("chi_level", 0).forGetter(BendingData::chiLevel),
            Codec.FLOAT.optionalFieldOf("max_chi_roll", 0.0f).forGetter(BendingData::maxChiRoll),
            Codec.FLOAT.optionalFieldOf("regen_roll", 0.0f).forGetter(BendingData::regenRoll),
            Codec.BOOL.optionalFieldOf("rolls_assigned", false).forGetter(BendingData::rollsAssigned),
            ResourceLocation.CODEC.optionalFieldOf("affinity").forGetter(BendingData::affinity)
    ).apply(instance, BendingData::new));

    public BendingData withChi(double chi) {
        return new BendingData(chi, chiLevel, maxChiRoll, regenRoll, rollsAssigned, affinity);
    }

    public BendingData withChiLevel(int level) {
        return new BendingData(chi, Math.max(0, level), maxChiRoll, regenRoll, rollsAssigned, affinity);
    }

    /** Sets both rolls together and marks them as assigned. */
    public BendingData withRolls(float maxChiRoll, float regenRoll) {
        return new BendingData(chi, chiLevel, maxChiRoll, regenRoll, true, affinity);
    }

    public BendingData withAffinity(ResourceLocation style) {
        return new BendingData(chi, chiLevel, maxChiRoll, regenRoll, rollsAssigned, Optional.of(style));
    }
}