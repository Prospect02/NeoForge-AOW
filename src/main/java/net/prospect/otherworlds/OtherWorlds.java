package net.prospect.otherworlds;

import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.prospect.otherworlds.bending.ModAbilities;
import net.prospect.otherworlds.bending.style.BendingStyle;
import net.prospect.otherworlds.bending.style.ModBendingStyles;
import net.prospect.otherworlds.block.ModBlocks;
import net.prospect.otherworlds.client.ClientAnimations;
import net.prospect.otherworlds.item.ModCreativeModeTabs;
import net.prospect.otherworlds.item.ModItems;
import net.prospect.otherworlds.network.ModNetwork;
import net.prospect.otherworlds.registry.ModAttachments;
import net.prospect.otherworlds.registry.ModAttributes;
import net.prospect.otherworlds.registry.ModEntities;
import org.slf4j.Logger;

// The value here should match an entry in the META-INF/neoforge.mods.toml file
@Mod(OtherWorlds.MODID)
public class OtherWorlds {
    // Define mod id in a common place for everything to reference
    public static final String MODID = "otherworlds";
    // Directly reference a slf4j logger
    public static final Logger LOGGER = LogUtils.getLogger();

    public OtherWorlds(IEventBus modEventBus, ModContainer modContainer) {
        // Register the commonSetup method for modloading
        modEventBus.addListener(this::commonSetup);

        ModBendingStyles.register(modEventBus);
        ModAbilities.register(modEventBus);
        ModBlocks.register(modEventBus);
        ModItems.register(modEventBus);
        ModCreativeModeTabs.register(modEventBus);
        ModAttachments.register(modEventBus);
        ModAttributes.register(modEventBus);
        ModEntities.register(modEventBus);
        ModNetwork.register(modEventBus);

        if (FMLEnvironment.dist == Dist.CLIENT) {
            ClientAnimations.register(modEventBus);
        }

        NeoForge.EVENT_BUS.register(this);

        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        LOGGER.info("Other Worlds common setup");
    }

    // You can use SubscribeEvent and let the Event Bus discover methods to call
    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        for (BendingStyle style : ModBendingStyles.REGISTRY) {
            LOGGER.info("Bending style: {} (sub-style: {})", style.getId(), style.isSubStyle());
        }
        LOGGER.info("Registered abilities: {}", ModAbilities.REGISTRY.keySet().size());
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MODID, path);
    }
}