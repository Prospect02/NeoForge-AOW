package net.prospect.otherworlds.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.prospect.otherworlds.OtherWorlds;
import net.prospect.otherworlds.bending.scooter.AirScooterEntity;
import net.prospect.otherworlds.bending.scooter.ScooterNetwork;

/**
 * Client only: the Jump key gets you off the scooter, and riders do not play the vanilla walking animation.
 */
@EventBusSubscriber(modid = OtherWorlds.MODID, value = Dist.CLIENT)
public final class AirScooterClient {
    private static final int HINT_TICKS = 30;   // vanilla shows its own "Press Shift to dismount", so ours is repeated over it

    private static boolean jumpLatched = false;
    private static int hintTicksLeft = 0;

    private AirScooterClient() {}

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;

        if (player == null || !(player.getVehicle() instanceof AirScooterEntity scooter)
                || scooter.getState() != AirScooterEntity.STATE_RIDING) {
            jumpLatched = false;
            hintTicksLeft = HINT_TICKS;
            return;
        }

        if (hintTicksLeft > 0) {
            if (hintTicksLeft % 3 == 0) {
                mc.gui.setOverlayMessage(Component.literal("Press Jump to hop off"), false);
            }
            hintTicksLeft--;
        }

        // One message per press, not one per tick
        boolean down = mc.options.keyJump.isDown();
        if (down && !jumpLatched) {
            PacketDistributor.sendToServer(new ScooterNetwork.DismountPayload());
        }
        jumpLatched = down;
    }

    /**
     * Every player on this screen, ourselves and others: a rider on the scooter does not walk. The sitting pose is off
     * (so the custom ride animation can control the body), which would otherwise let vanilla turn tiny position changes
     * into walking legs. This runs at the end of the player's tick, after vanilla has worked the walk animation out,
     * so what gets drawn is a still body.
     */
    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player.getVehicle() instanceof AirScooterEntity) {
            player.walkAnimation.setSpeed(0.0f);
        }
    }
}