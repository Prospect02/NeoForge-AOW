package net.prospect.otherworlds.bending.style;

import net.minecraft.Util;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.Optional;
import java.util.function.Supplier;

/** One bending style: a main element (Fire) or a sub-style (Lightning bending, whose parent is Fire). */
public class BendingStyle {
    private final Supplier<BendingStyle> parent; // null for the four main styles
    private final int color;                     // used later for particles, HUD, and tints

    /** A main style, with no parent. */
    public BendingStyle(int color) {
        this(null, color);
    }

    /** A sub-style of the given parent. */
    public BendingStyle(Supplier<BendingStyle> parent, int color) {
        this.parent = parent;
        this.color = color;
    }

    public int getColor() {
        return color;
    }

    public Optional<BendingStyle> getParent() {
        return Optional.ofNullable(parent).map(Supplier::get);
    }

    public boolean isSubStyle() {
        return parent != null;
    }

    /** True if the other style is this style's parent, or its parent's parent, and so on. */
    public boolean isSubStyleOf(BendingStyle other) {
        BendingStyle current = getParent().orElse(null);
        while (current != null) {
            if (current == other) return true;
            current = current.getParent().orElse(null);
        }
        return false;
    }

    public ResourceLocation getId() {
        return ModBendingStyles.REGISTRY.getKey(this);
    }

    public Component getDisplayName() {
        return Component.translatable(Util.makeDescriptionId("bending_style", getId()));
    }
}