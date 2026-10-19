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

    // --- Progression: general ---
    public static final ModConfigSpec.BooleanValue HARDCORE_MODE = BUILDER
            .comment("When true, dying wipes all progression: player level, element levels, move levels, learned moves, and the chi and Endurance levels they gave.")
            .define("progression.hardcoreMode", false);

    public static final ModConfigSpec.BooleanValue REQUIRE_LEARNED_MOVES = BUILDER
            .comment("When true, a move can only be cast after it has been learned. Leave off until scrolls exist, so every move can be tested.")
            .define("progression.requireLearnedMoves", false);

    public static final ModConfigSpec.IntValue MASTER_LEVEL_DISCOUNT = BUILDER
            .comment("A master teaches a move at this many element levels below what a scroll needs.")
            .defineInRange("progression.masterLevelDiscount", 2, 0, 44);

    // --- Progression: player level (earned by defeating opponents) ---
    public static final ModConfigSpec.IntValue PLAYER_MAX_LEVEL = BUILDER
            .comment("The highest player level.")
            .defineInRange("progression.player.maxLevel", 30, 1, 200);

    public static final ModConfigSpec.DoubleValue PLAYER_XP_BASE = BUILDER
            .comment("XP to go from player level L to L+1 is base * L ^ exponent.")
            .defineInRange("progression.player.xpBase", 100.0, 1.0, 1000000.0);

    public static final ModConfigSpec.DoubleValue PLAYER_XP_EXPONENT = BUILDER
            .comment("See xpBase.")
            .defineInRange("progression.player.xpExponent", 1.5, 0.5, 4.0);

    public static final ModConfigSpec.DoubleValue KILL_XP_PER_HEALTH = BUILDER
            .comment("Player XP for defeating an opponent, per point of the opponent's max health (until mob levels exist).")
            .defineInRange("progression.player.killXpPerHealth", 1.0, 0.0, 1000.0);

    // --- Progression: element level (gates learning, gives no combat bonuses) ---
    public static final ModConfigSpec.IntValue ELEMENT_MAX_LEVEL = BUILDER
            .comment("The highest element level.")
            .defineInRange("progression.element.maxLevel", 45, 1, 200);

    public static final ModConfigSpec.DoubleValue ELEMENT_XP_BASE = BUILDER
            .comment("XP to go from element level L to L+1 is base * L ^ exponent.")
            .defineInRange("progression.element.xpBase", 60.0, 1.0, 1000000.0);

    public static final ModConfigSpec.DoubleValue ELEMENT_XP_EXPONENT = BUILDER
            .comment("See xpBase.")
            .defineInRange("progression.element.xpExponent", 1.4, 0.5, 4.0);

    public static final ModConfigSpec.DoubleValue ELEMENT_XP_PER_CAST = BUILDER
            .comment("Element XP for a successful cast of a learned move.")
            .defineInRange("progression.element.xpPerCast", 2.0, 0.0, 10000.0);

    // --- Progression: move level ---
    public static final ModConfigSpec.IntValue MOVE_SOFT_CAP = BUILDER
            .comment("Move level that counts as fully mastered (Mastery 1.0). Levels past it still happen, but take far longer.")
            .defineInRange("progression.move.softCap", 50, 2, 200);

    public static final ModConfigSpec.IntValue MOVE_HARD_CAP = BUILDER
            .comment("The highest possible move level.")
            .defineInRange("progression.move.hardCap", 100, 2, 500);

    public static final ModConfigSpec.DoubleValue MOVE_XP_BASE = BUILDER
            .comment("XP to go from move level L to L+1 is base * L ^ exponent.")
            .defineInRange("progression.move.xpBase", 40.0, 1.0, 1000000.0);

    public static final ModConfigSpec.DoubleValue MOVE_XP_EXPONENT = BUILDER
            .comment("See xpBase.")
            .defineInRange("progression.move.xpExponent", 1.3, 0.5, 4.0);

    public static final ModConfigSpec.DoubleValue MOVE_OVERFLOW_MULTIPLIER = BUILDER
            .comment("Past the soft cap, the XP needed per level is multiplied by this.")
            .defineInRange("progression.move.overflowMultiplier", 10.0, 1.0, 1000.0);

    public static final ModConfigSpec.DoubleValue MOVE_XP_PER_CAST = BUILDER
            .comment("Move XP for a successful cast.")
            .defineInRange("progression.move.xpPerCast", 4.0, 0.0, 10000.0);

    public static final ModConfigSpec.DoubleValue MOVE_XP_PER_SCROLL = BUILDER
            .comment("Move XP for reading another scroll of a move that is already known.")
            .defineInRange("progression.move.xpPerScroll", 150.0, 0.0, 100000.0);

    // --- Progression: XP multipliers and anti-farming ---
    public static final ModConfigSpec.DoubleValue HIT_XP_MULTIPLIER = BUILDER
            .comment("A move that hits a real target gives this many times the XP of a plain cast.")
            .defineInRange("progression.xp.hitMultiplier", 1.5, 1.0, 100.0);

    public static final ModConfigSpec.DoubleValue TRAINING_XP_MULTIPLIER = BUILDER
            .comment("XP multiplier while training or sparring.")
            .defineInRange("progression.xp.trainingMultiplier", 2.0, 1.0, 100.0);

    public static final ModConfigSpec.DoubleValue DIMINISH_RECOVER_SECONDS = BUILDER
            .comment("Repeating a move builds up fatigue that lowers its XP. Each point of fatigue fades after this many seconds.")
            .defineInRange("progression.xp.diminishRecoverSeconds", 6.0, 0.1, 600.0);

    public static final ModConfigSpec.DoubleValue DIMINISH_STRENGTH = BUILDER
            .comment("How hard fatigue cuts XP. The XP multiplier is 1 / (1 + fatigue * strength).")
            .defineInRange("progression.xp.diminishStrength", 0.5, 0.0, 100.0);

    public static final ModConfigSpec.DoubleValue DIMINISH_MIN = BUILDER
            .comment("The lowest the fatigue multiplier can go (0.1 = at least 10 percent of the XP).")
            .defineInRange("progression.xp.diminishMin", 0.1, 0.0, 1.0);

    // Bending Config values above Spec

    static final ModConfigSpec SPEC = BUILDER.build();
}