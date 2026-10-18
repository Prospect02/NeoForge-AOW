package net.prospect.otherworlds.bending.effect;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/** Timed effects currently on one entity. Not saved: they end on logout and death. */
public class ActiveEffects {

    public static class State {
        public int ticksLeft;
        public int age;
        public double strength = 1.0;   // 1.0 = full effect; mastery will change this later
        public boolean airborne;        // effect specific flags
        public boolean engaged;
        public Vec3 lastPos;            // set by EffectManager each tick
        public Vec3 velocity = Vec3.ZERO;   // blocks moved last tick, measured on the server
    }

    private final Map<ResourceLocation, State> effects = new HashMap<>();

    public State add(ResourceLocation id, int ticks, double strength) {
        State state = new State();
        state.ticksLeft = ticks;
        state.strength = strength;
        effects.put(id, state);   // casting again simply restarts it
        return state;
    }

    public State get(ResourceLocation id) { return effects.get(id); }
    public boolean has(ResourceLocation id) { return effects.containsKey(id); }
    public void remove(ResourceLocation id) { effects.remove(id); }
    public boolean isEmpty() { return effects.isEmpty(); }
    public Set<ResourceLocation> ids() { return effects.keySet(); }
}