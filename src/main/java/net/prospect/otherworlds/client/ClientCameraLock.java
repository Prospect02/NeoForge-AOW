package net.prospect.otherworlds.client;

import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.CalculatePlayerTurnEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.prospect.otherworlds.OtherWorlds;

/** Holds the local player's camera still while the server says so (for example stuck on a wall). */
@EventBusSubscriber(modid = OtherWorlds.MODID, value = Dist.CLIENT)
public class ClientCameraLock {
    /** If the unlock message never arrives, let go after this long (200 ticks = 10 seconds). */
    private static final int FAILSAFE_TICKS = 200;

    private static boolean locked = false;
    private static int ticksLeft = 0;
    private static float yaw;
    private static float pitch;

    public static void set(boolean lock) {
        Minecraft mc = Minecraft.getInstance();
        if (lock && mc.player != null) {
            locked = true;
            ticksLeft = FAILSAFE_TICKS;
            yaw = mc.player.getYRot();
            pitch = mc.player.getXRot();
        } else {
            locked = false;
        }
    }

    public static boolean isLocked() {
        return locked;
    }

    /** Mouse look: a sensitivity of -1/3 works out to exactly zero turning in the game's formula. */
    @SubscribeEvent
    public static void onTurn(CalculatePlayerTurnEvent event) {
        if (!locked) return;
        event.setMouseSensitivity(-1.0 / 3.0);
        event.setCinematicCameraEnabled(false);
    }

    /** Backup: put the view back to where it was every tick, in case anything else turned it. */
    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        if (!locked) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || --ticksLeft <= 0) {
            locked = false;
            return;
        }
        mc.player.setYRot(yaw);
        mc.player.setXRot(pitch);
        mc.player.yRotO = yaw;
        mc.player.xRotO = pitch;
    }

    @SubscribeEvent
    public static void onRespawn(ClientPlayerNetworkEvent.Clone event) {
        locked = false;
    }

    @SubscribeEvent
    public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        locked = false;
    }
}