package net.prospect.otherworlds.client;

import net.minecraft.client.renderer.entity.NoopRenderer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.prospect.otherworlds.OtherWorlds;
import net.prospect.otherworlds.registry.ModEntities;

/** Client only: tells the game how to draw our entities. */
@EventBusSubscriber(modid = OtherWorlds.MODID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public class ClientEntityRenderers {

    @SubscribeEvent
    public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.BENDING_PROJECTILE.get(), NoopRenderer::new);
    }
}