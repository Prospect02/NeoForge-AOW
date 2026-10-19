package net.prospect.otherworlds.bending.progression;

import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.Map;

/** Short-lived progression state. Not saved. Tracks "fatigue" from repeating a move, so spamming gives less XP. */
public class ProgressionState {
    public static final class Heat {
        public double value;   // fatigue points
        public long tick;      // game time it was last updated
    }

    public final Map<ResourceLocation, Heat> heat = new HashMap<>();
}