package net.prospect.otherworlds;

import net.neoforged.neoforge.common.ModConfigSpec;

public class Config {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    // --- Chi ---
    public static final ModConfigSpec.IntValue MAX_CHI_VARIANCE_PERCENT = BUILDER
            .comment("How far a player's natural max chi can differ from the base, in percent (0 turns it off).")
            .defineInRange("chi.maxChiVariancePercent", 10, 0, 50);

    public static final ModConfigSpec.IntValue REGEN_VARIANCE_PERCENT = BUILDER
            .comment("How far a player's natural chi regeneration can differ from the base, in percent (0 turns it off).")
            .defineInRange("chi.regenVariancePercent", 3, 0, 50);

    public static final ModConfigSpec.IntValue MAX_CHI_GROWTH_VARIANCE_PERCENT = BUILDER
            .comment("How much the max-chi roll nudges max chi growth per level, in percent (0 turns it off).")
            .defineInRange("chi.maxChiGrowthVariancePercent", 2, 0, 50);

    public static final ModConfigSpec.DoubleValue MAX_CHI_PER_LEVEL = BUILDER
            .comment("Max chi gained per chi level.")
            .defineInRange("chi.maxChiPerLevel", 5.0, 0.0, 1000.0);

    public static final ModConfigSpec.DoubleValue REGEN_PER_LEVEL = BUILDER
            .comment("Chi per second of regeneration gained per chi level.")
            .defineInRange("chi.regenPerLevel", 0.1, 0.0, 100.0);

    // Bending Config values above Spec

    static final ModConfigSpec SPEC = BUILDER.build();
}