package net.prospect.otherworlds.bending.damage;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.phys.Vec3;
import net.prospect.otherworlds.Config;

/** Hurting things with bending that does not fly as a projectile (sweeps, strikes, shockwaves). */
public final class BendingDamage {
    private BendingDamage() {}

    /** False for the caster, dead or spectating targets, and (if the config says so) the caster's tamed pets. */
    public static boolean canHurt(LivingEntity attacker, Entity target) {
        if (target == attacker || !target.isAlive() || target.isSpectator()) return false;
        if (Config.BENDING_PROTECT_TAMED_PETS.get()
                && target instanceof OwnableEntity pet
                && attacker.getUUID().equals(pet.getOwnerUUID())) {
            return false;
        }
        return true;
    }

    /** Deals bending damage (after the global multiplier) and pushes the target. True if it was hurt. */
    public static boolean hit(LivingEntity attacker, Entity target, float amount, Vec3 push) {
        if (!(attacker.level() instanceof ServerLevel level)) return false;
        float scaled = amount * Config.BENDING_DAMAGE_MULTIPLIER.get().floatValue();
        boolean hurt = target.hurt(ModDamageTypes.bending(level, attacker, attacker), scaled);
        if (hurt) {
            target.setDeltaMovement(target.getDeltaMovement().add(push));
            target.hurtMarked = true;   // makes the push show up for players
        }
        return hurt;
    }
}