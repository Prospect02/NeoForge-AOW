package net.prospect.otherworlds.bending.damage;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.prospect.otherworlds.OtherWorlds;
import org.jetbrains.annotations.Nullable;

public final class ModDamageTypes {
    private ModDamageTypes() {}

    /** Every bending attack deals this type of damage. */
    public static final ResourceKey<DamageType> BENDING =
            ResourceKey.create(Registries.DAMAGE_TYPE, OtherWorlds.id("bending"));

    /** Damage types in this tag drain Endurance first. The tag is a data file you can edit. */
    public static final TagKey<DamageType> DRAINS_ENDURANCE =
            TagKey.create(Registries.DAMAGE_TYPE, OtherWorlds.id("drains_endurance"));

    /** direct = the thing that hit (a projectile), owner = who cast it. Either can be null. */
    public static DamageSource bending(Level level, @Nullable Entity direct, @Nullable Entity owner) {
        Holder<DamageType> type = level.registryAccess()
                .registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(BENDING);
        return new DamageSource(type, direct, owner);
    }
}