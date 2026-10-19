package net.prospect.otherworlds.bending.endurance;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.event.entity.living.LivingEvent;

/** Posted when a hit takes an entity's Endurance down to zero. The sparring system will listen for this. */
public class EnduranceDepletedEvent extends LivingEvent {
    private final DamageSource source;

    public EnduranceDepletedEvent(LivingEntity entity, DamageSource source) {
        super(entity);
        this.source = source;
    }

    public DamageSource getSource() {
        return source;
    }
}