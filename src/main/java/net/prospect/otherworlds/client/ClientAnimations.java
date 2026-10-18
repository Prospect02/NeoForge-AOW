package net.prospect.otherworlds.client;

import com.zigythebird.playeranim.animation.PlayerAnimResources;
import com.zigythebird.playeranim.animation.PlayerAnimationController;
import com.zigythebird.playeranim.api.PlayerAnimationAccess;
import com.zigythebird.playeranim.api.PlayerAnimationFactory;
import com.zigythebird.playeranimcore.enums.PlayState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.prospect.otherworlds.OtherWorlds;

/** Plays bending animations on players using Player Animation Library. Client only. */
public class ClientAnimations {
    public static final ResourceLocation LAYER_ID = OtherWorlds.id("bending");

    // Gameplay animations should use priority 1500 or higher so emotes and idle poses don't override them.
    private static final int LAYER_PRIORITY = 1500;

    public static void register(IEventBus modEventBus) {
        modEventBus.addListener(ClientAnimations::onClientSetup);
    }

    private static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() ->
                PlayerAnimationFactory.ANIMATION_DATA_FACTORY.registerFactory(LAYER_ID, LAYER_PRIORITY,
                        player -> new PlayerAnimationController(player,
                                (controller, state, animSetter) -> PlayState.STOP)));
    }

    public static void play(int entityId, ResourceLocation animation) {
        PlayerAnimationController controller = controllerFor(entityId);
        if (controller == null) return;

        // No animation file for this move yet: that's fine, it just plays nothing.
        if (PlayerAnimResources.getAnimation(animation) == null) return;
        controller.triggerAnimation(animation);
    }

    public static void stop(int entityId) {
        PlayerAnimationController controller = controllerFor(entityId);
        if (controller != null) controller.stopTriggeredAnimation();
    }

    private static PlayerAnimationController controllerFor(int entityId) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return null;
        Entity entity = mc.level.getEntity(entityId);
        if (!(entity instanceof AbstractClientPlayer player)) return null;   // mobs use GeckoLib later
        if (PlayerAnimationAccess.getPlayerAnimationLayer(player, LAYER_ID) instanceof PlayerAnimationController controller) {
            return controller;
        }
        return null;
    }
}