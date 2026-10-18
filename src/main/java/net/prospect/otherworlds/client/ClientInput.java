package net.prospect.otherworlds.client;

import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.prospect.otherworlds.OtherWorlds;
import net.prospect.otherworlds.network.ComboClickPayload;

/** Turns left and right clicks into combo clicks while a cast is waiting for them. */
@EventBusSubscriber(modid = OtherWorlds.MODID, value = Dist.CLIENT)
public class ClientInput {
    /** After a cast ends, keep ignoring a held button for this long (20 ticks = 1 second). */
    private static final int RELEASE_GRACE_TICKS = 20;

    private static int graceTicks = 0;

    // One press = one combo click. After a click is sent, that button stays locked
    // until the physical mouse button has been let go.
    private static boolean leftLatched = false;
    private static boolean rightLatched = false;

    @SubscribeEvent
    public static void onInteraction(InputEvent.InteractionKeyMappingTriggered event) {
        if (!ClientCastState.isOpen()) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || !mc.player.isAlive()) {
            ClientCastState.reset();   // safety net: never stay stuck after dying
            resetLocal();
            return;
        }

        if (event.isAttack()) {
            event.setCanceled(true);
            event.setSwingHand(false);
            if (!leftLatched) {
                leftLatched = true;
                PacketDistributor.sendToServer(new ComboClickPayload(true));
            }
        } else if (event.isUseItem()) {
            event.setCanceled(true);
            event.setSwingHand(false);
            if (!rightLatched) {
                rightLatched = true;
                PacketDistributor.sendToServer(new ComboClickPayload(false));
            }
        }
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Pre event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        // Unlock a button once the physical mouse button has been released.
        if (!mc.mouseHandler.isLeftPressed()) leftLatched = false;
        if (!mc.mouseHandler.isRightPressed()) rightLatched = false;

        // Holding a button is a separate path in vanilla: it keeps breaking blocks (left) or
        // repeats the use action (right). While a cast is collecting clicks, and for a short
        // grace period afterwards, release the "held" state each tick so the last combo click
        // doesn't turn into mining. Fresh presses still work normally once the cast is over.
        if (ClientCastState.isOpen()) {
            graceTicks = RELEASE_GRACE_TICKS;   // keep refreshing while the cast is running
        } else if (graceTicks > 0) {
            graceTicks--;
        } else {
            return;
        }

        mc.options.keyAttack.setDown(false);
        mc.options.keyUse.setDown(false);
    }

    // Clear the state when the local player is replaced (respawn) or leaves the world.
    @SubscribeEvent
    public static void onRespawn(ClientPlayerNetworkEvent.Clone event) {
        ClientCastState.reset();
        resetLocal();
    }

    @SubscribeEvent
    public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        ClientCastState.reset();
        resetLocal();
    }

    private static void resetLocal() {
        graceTicks = 0;
        leftLatched = false;
        rightLatched = false;
    }
}