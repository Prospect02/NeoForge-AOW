package net.prospect.otherworlds.bending.endurance;

/** Short-lived Endurance state. Not saved. */
public class EnduranceState {
    public long lastHitTick = -1_000_000L;   // game time of the last hit Endurance took
    public double lastIncoming;              // damage of that hit after armor
    public double lastAbsorbed;              // how much of it Endurance took
    public boolean sparring;                 // in a spar: hearts are untouchable, no regen

    public double regenBoost;                // food boost: 0.5 = regeneration 50 percent faster
    public long boostEndTick;                // game time the food boost runs out
}