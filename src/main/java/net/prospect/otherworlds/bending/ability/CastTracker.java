package net.prospect.otherworlds.bending.ability;

import org.jetbrains.annotations.Nullable;

/** What an entity is casting right now. Not saved. */
public class CastTracker {
    public enum Phase { COMBO, CHARGE, RELEASE_DELAY, ACTIVE }

    @Nullable public Ability ability;
    public double quality = 1.0;
    public double charge;   // kept while a strike move waits for its impact tick
    public int ticks;       // ticks spent in the current phase
    public int step;        // correct combo clicks so far
    public Phase phase = Phase.COMBO;

    public boolean isCasting() {
        return ability != null;
    }

    public void start(Ability ability, double quality) {
        this.ability = ability;
        this.quality = quality;
        this.charge = 0.0;
        this.ticks = 0;
        this.step = 0;
        this.phase = Phase.COMBO;
    }

    public void clear() {
        this.ability = null;
        this.charge = 0.0;
        this.ticks = 0;
        this.step = 0;
        this.phase = Phase.COMBO;
    }
}