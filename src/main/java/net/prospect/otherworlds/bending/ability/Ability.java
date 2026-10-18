package net.prospect.otherworlds.bending.ability;

import net.minecraft.Util;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.prospect.otherworlds.bending.ModAbilities;
import net.prospect.otherworlds.bending.style.BendingStyle;

import java.util.function.Supplier;

/** Base class for every bending move. */
public abstract class Ability {
    private final Supplier<BendingStyle> style;
    private final double chiCost;
    private final int cooldownTicks;

    protected Ability(Supplier<BendingStyle> style, double chiCost, int cooldownTicks) {
        this.style = style;
        this.chiCost = chiCost;
        this.cooldownTicks = cooldownTicks;
    }

    /**
     * What the move does. Runs on the server only.
     * Return true if it fired. Chi and cooldown are only charged when this returns true.
     */
    public abstract boolean activate(AbilityContext context);

    public BendingStyle getStyle() {
        return style.get();
    }

    public double getChiCost() {
        return chiCost;
    }

    /** 20 ticks = 1 second. */
    public int getCooldownTicks() {
        return cooldownTicks;
    }

    public ResourceLocation getId() {
        return ModAbilities.REGISTRY.getKey(this);
    }

    public Component getDisplayName() {
        return Component.translatable(Util.makeDescriptionId("ability", getId()));
    }
}