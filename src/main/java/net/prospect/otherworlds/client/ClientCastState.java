package net.prospect.otherworlds.client;

import net.minecraft.resources.ResourceLocation;

/** What the server last told this client about the local player's cast. */
public class ClientCastState {
    private static boolean open;
    private static ResourceLocation ability;
    private static int step;
    private static int total;

    public static void set(boolean inputOpen, ResourceLocation abilityId, int comboStep, int comboTotal) {
        open = inputOpen;
        ability = abilityId;
        step = comboStep;
        total = comboTotal;
    }

    public static void reset() {
        open = false;
        step = 0;
        total = 0;
    }

    public static boolean isOpen() { return open; }
    public static ResourceLocation getAbility() { return ability; }
    public static int getStep() { return step; }
    public static int getTotal() { return total; }
}