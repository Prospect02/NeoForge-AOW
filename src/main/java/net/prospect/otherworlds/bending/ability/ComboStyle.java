package net.prospect.otherworlds.bending.ability;

public enum ComboStyle {
    /** Combo first (no animation), then the animation plays and the effect lands at its impact tick. */
    STRIKE,
    /** The animation plays while the combo is entered. Its length is the time limit. */
    CAST_ALONG
}