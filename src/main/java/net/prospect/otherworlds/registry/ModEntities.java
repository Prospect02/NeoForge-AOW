package net.prospect.otherworlds.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.prospect.otherworlds.OtherWorlds;
import net.prospect.otherworlds.bending.projectile.BendingProjectile;

public class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(Registries.ENTITY_TYPE, OtherWorlds.MODID);

    /** The one projectile every flying bending attack uses. Never saved to disk. */
    public static final DeferredHolder<EntityType<?>, EntityType<BendingProjectile>> BENDING_PROJECTILE =
            ENTITY_TYPES.register("bending_projectile", () ->
                    EntityType.Builder.<BendingProjectile>of(BendingProjectile::new, MobCategory.MISC)
                            .sized(0.4f, 0.4f)
                            .clientTrackingRange(8)
                            .updateInterval(1)
                            .noSave()
                            .noSummon()
                            .fireImmune()
                            .build("bending_projectile"));

    public static void register(IEventBus eventBus) {
        ENTITY_TYPES.register(eventBus);
    }
}