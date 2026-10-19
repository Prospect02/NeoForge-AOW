package net.prospect.otherworlds.bending.projectile;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.prospect.otherworlds.Config;
import net.prospect.otherworlds.bending.damage.ModDamageTypes;
import net.prospect.otherworlds.registry.ModEntities;

import java.util.HashSet;
import java.util.Set;

/**
 * A bolt of bending that flies in a straight line. The owner can be any LivingEntity, so mobs can fire it too.
 * Everything about it is set before it spawns, so each move decides its own damage, speed, range and so on.
 * Server side only: the client just sees the particles it leaves.
 */
public class BendingProjectile extends Projectile {
    /** Every move starts from this damage until it sets its own. */
    public static final float DEFAULT_DAMAGE = 5.0f;

    private float damage = DEFAULT_DAMAGE;
    private float knockback = 0.6f;       // horizontal push on a hit
    private double maxRange = 24.0;       // blocks travelled before it fades
    private int pierce = 0;               // extra entities it can pass through
    private double travelled = 0.0;
    private final Set<Integer> alreadyHit = new HashSet<>();

    public BendingProjectile(EntityType<? extends BendingProjectile> type, Level level) {
        super(type, level);
        setNoGravity(true);
    }

    public BendingProjectile(Level level, LivingEntity owner) {
        this(ModEntities.BENDING_PROJECTILE.get(), level);
        setOwner(owner);
    }

    // ---------- settings (chain them before launching) ----------

    public BendingProjectile damage(float value) { this.damage = value; return this; }
    public BendingProjectile knockback(float value) { this.knockback = value; return this; }
    public BendingProjectile range(double blocks) { this.maxRange = blocks; return this; }
    public BendingProjectile pierce(int extraEntities) { this.pierce = extraEntities; return this; }

    /** Puts it at "from" and sends it along "direction" at "speed" blocks per tick. */
    public void launch(Vec3 from, Vec3 direction, double speed) {
        setPos(from);
        setDeltaMovement(direction.normalize().scale(speed));
    }

    /**
     * The usual way to fire one: from in front of the user's eyes, along where they look.
     * Moves with a different shape can build the projectile themselves.
     */
    public static BendingProjectile shoot(LivingEntity user, ServerLevel level, float damage, double speed, double range) {
        BendingProjectile projectile = new BendingProjectile(level, user).damage(damage).range(range);
        Vec3 look = user.getLookAngle();
        projectile.launch(user.getEyePosition().add(look.scale(0.6)), look, speed);
        level.addFreshEntity(projectile);
        return projectile;
    }

    // ---------- behaviour ----------

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
    }

    @Override
    protected boolean canHitEntity(Entity target) {
        if (!super.canHitEntity(target)) return false;
        if (alreadyHit.contains(target.getId())) return false;

        // Optional: bending never hurts the caster's own tamed pets
        if (Config.BENDING_PROTECT_TAMED_PETS.get()) {
            Entity owner = getOwner();
            if (owner != null && target instanceof OwnableEntity pet && owner.getUUID().equals(pet.getOwnerUUID())) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void tick() {
        super.tick();
        if (!(level() instanceof ServerLevel server)) return;

        Vec3 velocity = getDeltaMovement();
        Vec3 start = position();
        Vec3 end = start.add(velocity);

        HitResult hit = ProjectileUtil.getHitResultOnMoveVector(this, this::canHitEntity);
        if (hit.getType() != HitResult.Type.MISS) {
            onHit(hit);
            if (isRemoved()) return;
        }

        setPos(end);
        travelled += velocity.length();

        // Trail: a few puffs along the step it just made
        int puffs = Math.max(1, (int) Math.ceil(velocity.length() / 0.5));
        for (int i = 0; i < puffs; i++) {
            Vec3 p = start.lerp(end, (i + 1.0) / puffs);
            server.sendParticles(ParticleTypes.CLOUD, p.x, p.y, p.z, 1, 0.03, 0.03, 0.03, 0.0);
        }

        if (travelled >= maxRange || tickCount > 400) discard();
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);
        if (!(level() instanceof ServerLevel server)) return;

        Entity target = result.getEntity();
        alreadyHit.add(target.getId());
        Entity owner = getOwner();

        float amount = damage * Config.BENDING_DAMAGE_MULTIPLIER.get().floatValue();
        boolean hurt = target.hurt(ModDamageTypes.bending(server, this, owner), amount);

        if (hurt) {
            Vec3 dir = getDeltaMovement().normalize();
            target.setDeltaMovement(target.getDeltaMovement().add(dir.x * knockback, 0.25 * knockback, dir.z * knockback));
            target.hurtMarked = true;   // makes the push show up for players
        }

        Vec3 p = result.getLocation();
        server.sendParticles(ParticleTypes.POOF, p.x, p.y, p.z, 6, 0.2, 0.2, 0.2, 0.05);

        if (pierce > 0) pierce--;
        else discard();
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
        super.onHitBlock(result);
        if (level() instanceof ServerLevel server) {
            Vec3 p = result.getLocation();
            server.sendParticles(ParticleTypes.POOF, p.x, p.y, p.z, 8, 0.2, 0.2, 0.2, 0.05);
        }
        discard();
    }
}