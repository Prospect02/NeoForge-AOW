package net.prospect.otherworlds.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.prospect.otherworlds.OtherWorlds;
import net.prospect.otherworlds.bending.projectile.BendingProjectile;
import net.prospect.otherworlds.bending.scooter.AirScooterEntity;

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

    /**
     * The rideable ball of air for Air Scooter. Never saved to disk.
     * 1.0 wide (so it still fits through a one block gap) and 0.9 tall. The rider's feet are at AirScooterEntity.RIDER_HEIGHT.
     */
    public static final DeferredHolder<EntityType<?>, EntityType<AirScooterEntity>> AIR_SCOOTER =
            ENTITY_TYPES.register("air_scooter", () ->
                    EntityType.Builder.<AirScooterEntity>of(AirScooterEntity::new, MobCategory.MISC)
                            .sized(1.0f, 0.9f)
                            .passengerAttachments(AirScooterEntity.RIDER_HEIGHT)
                            .clientTrackingRange(10)
                            .updateInterval(1)
                            .noSave()
                            .noSummon()
                            .fireImmune()
                            .build("air_scooter"));

    public static void register(IEventBus eventBus) {
        ENTITY_TYPES.register(eventBus);
    }
}