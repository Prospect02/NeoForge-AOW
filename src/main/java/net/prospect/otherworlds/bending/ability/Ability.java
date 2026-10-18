package net.prospect.otherworlds.bending.ability;

import net.minecraft.Util;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.prospect.otherworlds.bending.ModAbilities;
import net.prospect.otherworlds.bending.style.ModBendingStyles;

import java.util.function.Supplier;

/** Base class for every bending move. Phase 3 adds the cast lifecycle. */
public abstract class Ability {
    private final Supplier<ModBendingStyles> style;

    protected Ability(Supplier<ModBendingStyles> style) {
        this.style = style;
    }

    public ModBendingStyles getStyle() {
        return style.get();
    }

    public ResourceLocation getId() {
        return ModAbilities.REGISTRY.getKey(this);
    }

    public Component getDisplayName() {
        return Component.translatable(Util.makeDescriptionId("ability", getId()));
    }
}