package net.prospect.otherworlds.bending.endurance;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.registries.datamaps.DataMapType;
import net.neoforged.neoforge.registries.datamaps.RegisterDataMapTypesEvent;
import net.prospect.otherworlds.OtherWorlds;

/** Data maps let a data file attach extra values to items. This one is read from endurance_food.json. */
@EventBusSubscriber(modid = OtherWorlds.MODID, bus = EventBusSubscriber.Bus.MOD)
public class ModDataMaps {

    public static final DataMapType<Item, EnduranceFood> ENDURANCE_FOOD =
            DataMapType.builder(OtherWorlds.id("endurance_food"), Registries.ITEM, EnduranceFood.CODEC).build();

    @SubscribeEvent
    public static void register(RegisterDataMapTypesEvent event) {
        event.register(ENDURANCE_FOOD);
    }
}