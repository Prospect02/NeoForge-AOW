package net.prospect.otherworlds.bending.chi;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.prospect.otherworlds.Config;
import net.prospect.otherworlds.OtherWorlds;
import net.prospect.otherworlds.bending.data.BendingData;
import net.prospect.otherworlds.registry.ModAttachments;
import net.prospect.otherworlds.registry.ModAttributes;

public class ChiHandler {
    // One fixed id per attribute, so re-applying replaces the old modifier instead of stacking.
    private static final ResourceLocation MAX_CHI_NATURAL_ID = OtherWorlds.id("max_chi_natural");
    private static final ResourceLocation REGEN_NATURAL_ID = OtherWorlds.id("regen_natural");

    // Ids from the first version of part B. Removed on refresh so old test characters
    // don't keep a stale modifier. You can delete these once all your test worlds have been opened.
    private static final ResourceLocation OLD_MAX_CHI_ID = OtherWorlds.id("max_chi_variance");
    private static final ResourceLocation OLD_REGEN_ID = OtherWorlds.id("regen_variance");

    // ---------- reading ----------

    public static double getMaxChi(Player player) {
        return round2(player.getAttributeValue(ModAttributes.MAX_CHI));
    }

    /** Chi per second, rounded to hundredths. */
    public static double getRegen(Player player) {
        return round2(player.getAttributeValue(ModAttributes.CHI_REGEN));
    }

    /** Stored chi is precise. Round with round2 when showing it. */
    public static double getChi(Player player) {
        return player.getData(ModAttachments.BENDING).chi();
    }

    public static int getChiLevel(Player player) {
        return player.getData(ModAttachments.BENDING).chiLevel();
    }

    // ---------- changing (server side only) ----------

    /** Spends chi if the player has enough. Returns false and spends nothing otherwise. */
    public static boolean trySpend(ServerPlayer player, double amount) {
        BendingData data = player.getData(ModAttachments.BENDING);
        if (data.chi() < amount) return false;
        player.setData(ModAttachments.BENDING, data.withChi(data.chi() - amount));
        return true;
    }

    public static void addChi(ServerPlayer player, double amount) {
        setChi(player, getChi(player) + amount);
    }

    public static void setChi(ServerPlayer player, double value) {
        double clamped = Mth.clamp(value, 0.0, getMaxChi(player));
        BendingData data = player.getData(ModAttachments.BENDING);
        if (clamped != data.chi()) {
            player.setData(ModAttachments.BENDING, data.withChi(clamped));
        }
    }

    /** Sets the chi level and recalculates max chi and regen. Current chi is not refilled. */
    public static void setChiLevel(ServerPlayer player, int level) {
        BendingData data = player.getData(ModAttachments.BENDING);
        player.setData(ModAttachments.BENDING, data.withChiLevel(level));
        refreshAttributes(player);
        setChi(player, getChi(player));
    }

    public static void addChiLevels(ServerPlayer player, int amount) {
        setChiLevel(player, getChiLevel(player) + amount);
    }

    // ---------- lifecycle ----------

    /** Call on login. Makes the rolls on first join, applies the attributes, and fills chi on first join. */
    public static void initialize(ServerPlayer player) {
        BendingData data = player.getData(ModAttachments.BENDING);
        boolean firstJoin = !data.rollsAssigned();

        if (firstJoin) {
            RandomSource random = player.getRandom();
            data = data.withRolls(rollVariance(random), rollVariance(random));
            player.setData(ModAttachments.BENDING, data);
        }

        refreshAttributes(player);

        if (firstJoin) {
            setChi(player, getMaxChi(player));   // new players start full
        } else {
            setChi(player, getChi(player));      // re-clamp in case max chi went down
        }
    }

    /** Call after respawn. Attributes are re-applied and chi is refilled. */
    public static void onRespawn(ServerPlayer player) {
        refreshAttributes(player);
        setChi(player, getMaxChi(player));
    }

    /**
     * Recalculates max chi and regen from the player's level and rolls.
     * This is the ONE place the scaling formulas live. Safe to call any number of times.
     */
    public static void refreshAttributes(ServerPlayer player) {
        BendingData data = player.getData(ModAttachments.BENDING);
        int level = data.chiLevel();

        // Max chi: (base + growth) * (1 + roll * variance)
        // The max-chi roll also nudges the growth slightly.
        AttributeInstance maxChi = player.getAttribute(ModAttributes.MAX_CHI);
        if (maxChi != null) {
            double base = maxChi.getBaseValue();
            double growthScale = 1.0 + data.maxChiRoll() * Config.MAX_CHI_GROWTH_VARIANCE_PERCENT.get() / 100.0;
            double growth = level * Config.MAX_CHI_PER_LEVEL.get() * growthScale;
            double variance = 1.0 + data.maxChiRoll() * Config.MAX_CHI_VARIANCE_PERCENT.get() / 100.0;
            double total = round2((base + growth) * variance);
            setNaturalModifier(maxChi, MAX_CHI_NATURAL_ID, OLD_MAX_CHI_ID, total - base);
        }

        // Regen: (base + growth) * (1 + roll * variance)
        AttributeInstance regen = player.getAttribute(ModAttributes.CHI_REGEN);
        if (regen != null) {
            double base = regen.getBaseValue();
            double growth = level * Config.REGEN_PER_LEVEL.get();
            double variance = 1.0 + data.regenRoll() * Config.REGEN_VARIANCE_PERCENT.get() / 100.0;
            double total = round2((base + growth) * variance);
            setNaturalModifier(regen, REGEN_NATURAL_ID, OLD_REGEN_ID, total - base);
        }
    }

    /** Called every server tick for each player. */
    public static void tick(ServerPlayer player) {
        double max = getMaxChi(player);
        double chi = getChi(player);
        if (chi >= max) return;
        addChi(player, getRegen(player) / 20.0);   // 20 ticks per second
    }

    // ---------- helpers ----------

    /** Rounds to the hundredths place (2 decimals). */
    public static double round2(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    /** Most players land near the middle of the range, and only a few reach the edges. */
    private static float rollVariance(RandomSource random) {
        return Mth.clamp((float) random.nextGaussian() * 0.4f, -1.0f, 1.0f);
    }

    /** Stores the player's natural bonus as one permanent flat modifier on top of the attribute's base. */
    private static void setNaturalModifier(AttributeInstance instance, ResourceLocation id,
                                           ResourceLocation oldId, double bonus) {
        instance.removeModifier(oldId);
        instance.addOrReplacePermanentModifier(
                new AttributeModifier(id, bonus, AttributeModifier.Operation.ADD_VALUE));
    }
}