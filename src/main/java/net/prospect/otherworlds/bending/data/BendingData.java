package net.prospect.otherworlds.bending.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;

import java.util.Optional;

/**
 * Everything the mod remembers about one player's bending.
 * Immutable: to change something, build a modified copy with a "with" method
 * and store it back on the player.
 *
 * maxChiRoll and regenRoll are each -1..1. They are rolled once on first join;
 * the config decides how much each roll is worth (see Phase 2b).
 */
public record BendingData(
        double chi,
        float maxChiRoll,
        float regenRoll,
        boolean rollsAssigned,
        Optional<ResourceLocation> affinity
) {
    public static final BendingData DEFAULT =
            new BendingData(0.0, 0.0f, 0.0f, false, Optional.empty());

    // Every field is optional with a default, so you can add fields in later
    // phases without breaking worlds that were saved earlier.
    public static final Codec<BendingData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.DOUBLE.optionalFieldOf("chi", 0.0).forGetter(BendingData::chi),
            Codec.FLOAT.optionalFieldOf("max_chi_roll", 0.0f).forGetter(BendingData::maxChiRoll),
            Codec.FLOAT.optionalFieldOf("regen_roll", 0.0f).forGetter(BendingData::regenRoll),
            Codec.BOOL.optionalFieldOf("rolls_assigned", false).forGetter(BendingData::rollsAssigned),
            ResourceLocation.CODEC.optionalFieldOf("affinity").forGetter(BendingData::affinity)
    ).apply(instance, BendingData::new));

    public BendingData withChi(double chi) {
        return new BendingData(chi, maxChiRoll, regenRoll, rollsAssigned, affinity);
    }

    /** Sets both rolls together and marks them as assigned. */
    public BendingData withRolls(float maxChiRoll, float regenRoll) {
        return new BendingData(chi, maxChiRoll, regenRoll, true, affinity);
    }

    public BendingData withAffinity(ResourceLocation style) {
        return new BendingData(chi, maxChiRoll, regenRoll, rollsAssigned, Optional.of(style));
    }
}