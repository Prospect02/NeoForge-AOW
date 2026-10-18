package net.prospect.otherworlds.bending.util;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** Server-side helpers for real velocity changes. */
public final class Motion {
    private Motion() {}

    /** Replaces the entity's velocity and tells its client (players included) about it. */
    public static void setVelocity(LivingEntity entity, Vec3 velocity) {
        entity.setDeltaMovement(velocity);
        entity.hurtMarked = true;
    }

    /** The direction the entity is facing, flattened to the ground. Zero if looking straight up or down. */
    public static Vec3 horizontalLook(LivingEntity entity) {
        Vec3 look = entity.getLookAngle();
        Vec3 flat = new Vec3(look.x, 0, look.z);
        return flat.lengthSqr() < 1.0E-4 ? Vec3.ZERO : flat.normalize();
    }

    /** Blocks between the entity's feet and the ground below, or maxDistance if there is none in range. */
    public static double groundDistance(LivingEntity entity, double maxDistance) {
        Level level = entity.level();
        Vec3 start = entity.position();
        Vec3 end = start.subtract(0, maxDistance, 0);
        BlockHitResult hit = level.clip(new ClipContext(
                start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, entity));
        if (hit.getType() == HitResult.Type.MISS) return maxDistance;
        return start.y - hit.getLocation().y;
    }
}