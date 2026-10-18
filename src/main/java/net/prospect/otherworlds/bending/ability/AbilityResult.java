package net.prospect.otherworlds.bending.ability;

public enum AbilityResult {
    SUCCESS,
    ON_COOLDOWN,
    NOT_ENOUGH_CHI,
    BUSY,        // already casting something else
    FAILED
}