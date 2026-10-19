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

    // --- Endurance (all values are damage points: 2 points = 1 heart) ---
    public static final ModConfigSpec.IntValue ENDURANCE_VARIANCE_PERCENT = BUILDER
            .comment("How far a player's natural max Endurance can differ from the base, in percent (0 turns it off).")
            .defineInRange("endurance.maxVariancePercent", 10, 0, 50);

    public static final ModConfigSpec.IntValue ENDURANCE_REGEN_VARIANCE_PERCENT = BUILDER
            .comment("How far a player's natural Endurance regeneration can differ from the base, in percent.")
            .defineInRange("endurance.regenVariancePercent", 3, 0, 50);

    public static final ModConfigSpec.DoubleValue ENDURANCE_PER_LEVEL = BUILDER
            .comment("Max Endurance gained per Endurance level.")
            .defineInRange("endurance.maxPerLevel", 2.0, 0.0, 1000.0);

    public static final ModConfigSpec.DoubleValue ENDURANCE_REGEN_PER_LEVEL = BUILDER
            .comment("Endurance per second of regeneration gained per Endurance level.")
            .defineInRange("endurance.regenPerLevel", 0.02, 0.0, 100.0);

    public static final ModConfigSpec.DoubleValue ENDURANCE_REGEN_DELAY_SECONDS = BUILDER
            .comment("Seconds without taking combat damage before Endurance starts to regenerate.")
            .defineInRange("endurance.regenDelaySeconds", 10.0, 0.0, 600.0);

    public static final ModConfigSpec.DoubleValue ENDURANCE_TRAINED_CAP = BUILDER
            .comment("The most max Endurance that training (sparring, taking hits, using moves) can add.")
            .defineInRange("endurance.trainedCap", 50.0, 1.0, 10000.0);

    public static final ModConfigSpec.DoubleValue ENDURANCE_TRAIN_PER_DAMAGE = BUILDER
            .comment("Max Endurance trained per point of damage Endurance absorbs. Gains shrink as you near the cap.")
            .defineInRange("endurance.trainPerDamage", 0.01, 0.0, 10.0);

    // --- Endurance food boosts ---
    public static final ModConfigSpec.BooleanValue ENDURANCE_FOOD_ENABLED = BUILDER
            .comment("Turns the food boost to Endurance regeneration on or off.")
            .define("endurance.food.enabled", true);

    public static final ModConfigSpec.BooleanValue ENDURANCE_FOOD_FALLBACK_ENABLED = BUILDER
            .comment("Foods that are not listed in data/otherworlds/data_maps/item/endurance_food.json get a boost worked out from how filling they are.")
            .define("endurance.food.fallbackEnabled", true);

    public static final ModConfigSpec.DoubleValue ENDURANCE_FOOD_BOOST_PER_NUTRITION = BUILDER
            .comment("Fallback: regen boost per point of food nutrition. 0.05 means a steak (8) gives +40 percent faster regeneration.")
            .defineInRange("endurance.food.fallbackBoostPerNutrition", 0.05, 0.0, 10.0);

    public static final ModConfigSpec.DoubleValue ENDURANCE_FOOD_SECONDS_PER_NUTRITION = BUILDER
            .comment("Fallback: seconds the boost lasts per point of food nutrition. 5.0 means a steak (8) lasts 40 seconds.")
            .defineInRange("endurance.food.fallbackSecondsPerNutrition", 5.0, 0.0, 600.0);

    public static final ModConfigSpec.DoubleValue ENDURANCE_FOOD_MAX_BOOST = BUILDER
            .comment("The most any food can boost regeneration (3.0 means up to +300 percent faster).")
            .defineInRange("endurance.food.maxBoost", 3.0, 0.0, 100.0);

    // --- Bending damage ---
    public static final ModConfigSpec.DoubleValue BENDING_VANILLA_ARMOR_FACTOR = BUILDER
            .comment("How much vanilla armor counts against bending damage. 1.0 = same as any other damage, 0.0 = vanilla armor does nothing against bending. Lower it so custom Avatar armor can beat vanilla armor.")
            .defineInRange("bending.vanillaArmorFactor", 1.0, 0.0, 1.0);

    public static final ModConfigSpec.DoubleValue MAX_BENDING_PROTECTION = BUILDER
            .comment("The most bending damage the bending_protection attribute can cut (0.8 = 80 percent).")
            .defineInRange("bending.maxProtection", 0.8, 0.0, 1.0);

    public static final ModConfigSpec.DoubleValue BENDING_DAMAGE_MULTIPLIER = BUILDER
            .comment("Scales the damage of every bending projectile (1.0 = as set on each move, 2.0 = double).")
            .defineInRange("bending.damageMultiplier", 1.0, 0.0, 100.0);

    public static final ModConfigSpec.BooleanValue BENDING_PROTECT_TAMED_PETS = BUILDER
            .comment("When true, your bending never hurts your own tamed pets. When false (the default), bending can hurt anything.")
            .define("bending.protectTamedPets", false);

    // Bending Config values above Spec

    static final ModConfigSpec SPEC = BUILDER.build();
}