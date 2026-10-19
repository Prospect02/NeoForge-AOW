package net.prospect.otherworlds.bending.ability;

import org.jetbrains.annotations.Nullable;

/**
 * The animation for a move. Gameplay never waits for the animation itself; the server counts ticks.
 *  - Cast-along moves: durationTicks is also the combo time limit.
 *  - Strike moves: releaseTicks is when the effect lands after the combo is finished.
 *  - Charged strike moves: chargeName plays (and should loop) from the end of the combo until the
 *    charge is released, then name plays and the effect lands at releaseTicks into it.
 */
public record AnimationSpec(@Nullable String name, int durationTicks, int releaseTicks, boolean interruptible,
                            @Nullable String chargeName) {
    public static final AnimationSpec NONE = new AnimationSpec(null, 0, 0, false, null);

    /** For cast-along moves. */
    public static AnimationSpec of(String name, int durationTicks, boolean interruptible) {
        return new AnimationSpec(name, durationTicks, 0, interruptible, null);
    }

    /** For strike moves: the animation is durationTicks long and the effect lands at impactTick. */
    public static AnimationSpec strike(String name, int durationTicks, int impactTick, boolean interruptible) {
        return new AnimationSpec(name, durationTicks, impactTick, interruptible, null);
    }

    /**
     * For charged strike moves. chargeName loops while the player holds the last click.
     * On release, name plays and the effect lands at impactTick into it.
     */
    public static AnimationSpec chargedStrike(String chargeName, String name, int durationTicks, int impactTick,
                                              boolean interruptible) {
        return new AnimationSpec(name, durationTicks, impactTick, interruptible, chargeName);
    }

    public boolean hasChargeAnimation() {
        return chargeName != null && !chargeName.isEmpty();
    }
}