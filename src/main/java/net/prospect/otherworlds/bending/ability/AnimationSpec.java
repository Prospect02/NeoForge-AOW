package net.prospect.otherworlds.bending.ability;

import org.jetbrains.annotations.Nullable;

/**
 * The animation for a move. Gameplay never waits for the animation itself; the server counts ticks.
 *  - Cast-along moves: durationTicks is also the combo time limit.
 *  - Strike moves: releaseTicks is when the effect lands after the combo is finished.
 */
public record AnimationSpec(@Nullable String name, int durationTicks, int releaseTicks, boolean interruptible) {
    public static final AnimationSpec NONE = new AnimationSpec(null, 0, 0, false);

    /** For cast-along moves. */
    public static AnimationSpec of(String name, int durationTicks, boolean interruptible) {
        return new AnimationSpec(name, durationTicks, 0, interruptible);
    }

    /** For strike moves: the animation is durationTicks long and the effect lands at impactTick. */
    public static AnimationSpec strike(String name, int durationTicks, int impactTick, boolean interruptible) {
        return new AnimationSpec(name, durationTicks, impactTick, interruptible);
    }
}