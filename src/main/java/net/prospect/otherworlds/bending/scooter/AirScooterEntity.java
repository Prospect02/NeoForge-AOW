package net.prospect.otherworlds.bending.scooter;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import net.prospect.otherworlds.OtherWorlds;
import net.prospect.otherworlds.bending.chi.ChiHandler;
import net.prospect.otherworlds.bending.util.Motion;
import net.prospect.otherworlds.network.CastAnimationPayload;
import net.prospect.otherworlds.registry.ModAttachments;

import java.util.UUID;

/**
 * The air ball you ride for Air Scooter.
 *
 * Three stages, kept in the synced STATE (the model and GeckoLib animations can read it later):
 *  - FORMING:   the caster hops with the ball in their hands. The entity exists but draws nothing. It follows
 *               under the caster's feet, so it is already in place when it appears.
 *  - RIDING:    the ball is placed under the caster at the top of the hop and they are seated on it.
 *               Steered by the rider. Hovers, glides and steps over obstacles, climbs tall walls, drains chi.
 *  - UNRAVELING: the rider got off. The rider hops off and the ball lingers a moment, then disappears.
 *
 * Steering works like a boat: the rider's own game moves the ball (so it feels instant) and the server accepts
 * where it ends up. That is why steer() runs on the rider's client only. Chi and stage changes run on the server.
 *
 * Getting off: the Jump key (see AirScooterClient / ScooterNetwork). Crouching does nothing (see AirScooterEvents).
 *
 * For the model: only draw the ball when the state is not FORMING. The hitbox is 1.0 wide and 0.9 tall (see ModEntities),
 * hovering HOVER_HEIGHT above the ground, and the rider's feet are at the top of it.
 */
public class AirScooterEntity extends Entity {
    // ---------- animation names (files in assets/otherworlds/player_animations) ----------

    public static final String ANIM_FORM = "air_scooter_form";
    public static final String ANIM_MOUNT = "air_scooter_mount";
    public static final String ANIM_RIDE = "air_scooter_ride";
    public static final String ANIM_DISMOUNT = "air_scooter_dismount";

    // ---------- stages ----------

    public static final int STATE_FORMING = 0;
    public static final int STATE_RIDING = 1;
    public static final int STATE_UNRAVELING = 2;

    /**
     * How high above the ball's base the rider's feet sit, in blocks. It matches the hitbox height set in ModEntities
     * (0.9) plus half a block of room, so the model sits under the rider. Raise both if the model is bigger.
     * While riding, the rider's feet are this plus HOVER_HEIGHT above the ground: 1.9 blocks.
     */
    public static final float RIDER_HEIGHT = 1.4f;

    // ---------- tuning: the sequence (20 ticks = 1 second) ----------

    private static final double HOP_VELOCITY = 0.53;           // the hop onto the ball: peaks about 1.9 blocks up
    private static final int MIN_HOP_TICKS = 3;                // never seat earlier than this
    private static final int MAX_HOP_TICKS = 20;               // seat anyway after this long (low ceiling, lag, ...)
    private static final double RISEN_MIN = 0.4;               // the hop only counts once you are this far up
    private static final double APEX_RISE_PER_TICK = 0.05;     // slower than this per tick means you are at the top
    private static final double HOP_OFF_UP = 0.42;             // the hop off: about 1.2 blocks high
    private static final double HOP_OFF_FORWARD = 0.35;
    private static final int UNRAVEL_TICKS = 10;              // how long the empty ball lingers
    private static final int RESUME_RIDE_ANIM_TICKS = 12;      // after another move's cast ends, the ride loop resumes
    private static final double MAX_CASTER_DISTANCE = 6.0;     // if the caster gets knocked away while forming

    // ---------- tuning: riding ----------

    private static final float DEFAULT_TOP_SPEED = 0.70f;      // blocks per tick. The ability sets the real value
    private static final double ACCELERATION = 0.12;           // share of the gap to top speed closed each tick
    private static final double COAST = 0.92;                  // speed kept each tick with no input
    private static final double HOVER_HEIGHT = 0.5;            // blocks above the ground or water
    private static final double HOVER_PULL = 0.3;              // how hard it corrects downward toward the hover height
    private static final double RISE_PULL = 0.45;              // ... and upward: it rises more eagerly than it sinks
    private static final double MAX_RISE = 0.8;                // blocks per tick
    private static final double MAX_SINK = 0.45;               // also the speed of a soft landing
    private static final double CLIMB_SPEED = 0.3;             // blocks per tick up a wall too tall to step onto
    private static final double FALL_ACCEL = 0.06;             // pull of gravity once there is nothing below
    private static final double MAX_FALL = 1.2;
    private static final double PROBE_DEPTH = 6.0;             // how far down it looks for the ground
    private static final double CHI_PER_TICK = 0.15;           // 3 chi per second

    // ---------- tuning: looking ahead for obstacles (a fence is 1.5 high and the ball hovers 0.5 up) ----------

    private static final double CENTER_PROBE_UP = 0.5;         // the ground check under the ball starts this far up
    private static final double AHEAD_PROBE_UP = 1.8;          // the checks ahead start higher, so they land on top of obstacles
    private static final double AHEAD_NEAR = 0.8;              // just past the front edge of the ball
    private static final double AHEAD_SIDE = 0.45;             // the near check also looks to each side
    private static final double LOOKAHEAD_TICKS = 4.0;         // the far check is this many ticks of travel further on
    private static final double BEHIND = 0.6;                  // stays up until its back end has cleared too
    private static final double LOOKAHEAD_MIN_SPEED = 0.05;    // not worth looking ahead when almost stopped

    // ---------- tuning: autostep (it covers anything the lookahead did not see coming) ----------

    private static final double STEP_MAX_RISE = 1.6;           // the tallest thing it will step straight up onto
    private static final double STEP_INCREMENT = 0.1;          // how finely it searches for the smallest lift that clears
    private static final double STEP_MARGIN = 0.02;            // a hair of extra room, so it never just touches

    /** Top speed in blocks per tick. Synced, since the rider's client does the moving. Mastery raises it. */
    private static final EntityDataAccessor<Float> TOP_SPEED =
            SynchedEntityData.defineId(AirScooterEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> STATE =
            SynchedEntityData.defineId(AirScooterEntity.class, EntityDataSerializers.INT);

    private UUID casterId;           // who made it (server only)
    private int hopTicks = 0;
    private double hopStartY = 0.0;
    private double lastCasterY = 0.0;
    private int unravelCountdown = 0;
    private boolean wasCasting = false;
    private int resumeRideAnimIn = 0;
    private boolean dismountAllowed = false;   // true only while we ourselves are getting the rider off

    public AirScooterEntity(EntityType<? extends AirScooterEntity> type, Level level) {
        super(type, level);
        setNoGravity(true);   // it hovers by its own rules. This also keeps servers from kicking the rider for "floating".
    }

    // ---------- setup (server) ----------

    /** Called right after spawning at the caster's feet. Gives the caster the hop; they are seated at its top. */
    public void beginForming(LivingEntity caster, float topSpeed) {
        casterId = caster.getUUID();
        hopTicks = 0;
        hopStartY = caster.getY();
        lastCasterY = caster.getY();
        entityData.set(TOP_SPEED, topSpeed);
        entityData.set(STATE, STATE_FORMING);

        if (caster.onGround()) {
            Vec3 v = caster.getDeltaMovement();
            Motion.setVelocity(caster, new Vec3(v.x * 0.3, HOP_VELOCITY, v.z * 0.3));
        }
    }

    public int getState() {
        return entityData.get(STATE);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(TOP_SPEED, DEFAULT_TOP_SPEED);
        builder.define(STATE, STATE_FORMING);
    }

    // ---------- riding ----------

    @Override
    public LivingEntity getControllingPassenger() {
        return getFirstPassenger() instanceof LivingEntity living ? living : null;
    }

    @Override
    protected boolean canAddPassenger(Entity passenger) {
        return getPassengers().isEmpty() && getState() != STATE_UNRAVELING;
    }

    /**
     * Keeps the rider upright instead of in the vanilla sitting pose, so the custom ride animation controls the body.
     * (A NeoForge addition. If your version does not have it, delete this method and the pose just stays vanilla.)
     */
    public boolean shouldRiderSit() {
        return false;
    }

    @Override
    protected MovementEmission getMovementEmission() {
        return MovementEmission.NONE;   // no footstep or splash sounds
    }

    /** True only while the scooter itself is getting the rider off. AirScooterEvents blocks every other crouch dismount. */
    public boolean isDismountAllowed() {
        return dismountAllowed;
    }

    /** Server: gets the rider off. The hop and the animation are handled when the next tick notices they are gone. */
    public void dismount() {
        LivingEntity rider = getControllingPassenger();
        if (rider == null) return;
        dismountAllowed = true;
        try {
            rider.stopRiding();
        } finally {
            dismountAllowed = false;
        }
    }

    // ---------- every tick ----------

    @Override
    public void tick() {
        super.tick();

        LivingEntity rider = getControllingPassenger();

        if (rider != null && getState() == STATE_RIDING && isControlledByLocalInstance()) {
            steer(rider);
            move(MoverType.SELF, getDeltaMovement());
        } else {
            setDeltaMovement(Vec3.ZERO);   // not ours to move: the rider's game moves it and tells the server
        }

        // No fall damage for whoever is on it
        for (Entity passenger : getPassengers()) {
            passenger.fallDistance = 0.0f;
        }

        if (level().isClientSide) {
            if (getState() == STATE_RIDING) swirl();   // not while forming: the ball is still in the caster's hands
            return;
        }

        switch (getState()) {
            case STATE_FORMING -> tickForming();
            case STATE_RIDING -> tickRiding(rider);
            default -> {
                if (--unravelCountdown <= 0) discard();
            }
        }
    }

    // ---------- server: forming ----------

    private void tickForming() {
        LivingEntity caster = findCaster();
        if (caster == null || !caster.isAlive() || caster.isPassenger()
                || caster.distanceToSqr(this) > MAX_CASTER_DISTANCE * MAX_CASTER_DISTANCE) {
            discard();
            return;
        }

        setYRot(caster.getYRot());
        caster.fallDistance = 0.0f;   // the hop must never hurt

        // The ball (invisible for now) follows under the caster's feet, so it is already in place when it appears
        followCaster(caster);

        // Watch the hop from the server's side: place the ball and seat them once they have risen and stopped rising
        hopTicks++;
        double y = caster.getY();
        double rise = y - lastCasterY;
        lastCasterY = y;

        boolean hasRisen = y > hopStartY + RISEN_MIN;
        boolean atApex = hasRisen && hopTicks >= MIN_HOP_TICKS && rise < APEX_RISE_PER_TICK;
        if (atApex || hopTicks >= MAX_HOP_TICKS) {
            seat(caster);
        }
    }

    /** Keeps the ball directly under the caster's feet while they hop. It stays on the ground until they are above it. */
    private void followCaster(LivingEntity caster) {
        double ballY = Math.max(hopStartY, caster.getY() - RIDER_HEIGHT);
        setPos(caster.getX(), ballY, caster.getZ());
    }

    /** The apex: the ball is placed under their feet (it shows up now) and they are seated on it. */
    private void seat(LivingEntity caster) {
        followCaster(caster);

        if (caster.startRiding(this, true)) {
            entityData.set(STATE, STATE_RIDING);
            animate(caster, ANIM_RIDE);
            if (level() instanceof ServerLevel server) {
                server.sendParticles(ParticleTypes.CLOUD, getX(), getY() + 0.2, getZ(), 20, 0.4, 0.1, 0.4, 0.05);
            }
        } else {
            discard();
        }
    }

    // ---------- server: riding ----------

    private void tickRiding(LivingEntity rider) {
        // The rider is gone: they got off (Jump, out of chi, ...). Hop them off and let the ball unravel.
        if (rider == null) {
            LivingEntity who = findCaster();
            if (who != null && who.isAlive()) hopOff(who);
            entityData.set(STATE, STATE_UNRAVELING);
            unravelCountdown = UNRAVEL_TICKS;
            return;
        }

        // Another move's cast animation stops the ride loop when it ends, so bring it back afterwards
        boolean casting = rider.getData(ModAttachments.CASTING).isCasting();
        if (wasCasting && !casting) resumeRideAnimIn = RESUME_RIDE_ANIM_TICKS;
        wasCasting = casting;
        if (resumeRideAnimIn > 0 && --resumeRideAnimIn == 0 && !casting) {
            animate(rider, ANIM_RIDE);
        }

        if (rider instanceof ServerPlayer player && !ChiHandler.trySpend(player, CHI_PER_TICK)) {
            player.displayClientMessage(Component.literal("Out of chi."), true);
            dismount();   // noticed next tick, which hops them off
        }
    }

    private void hopOff(LivingEntity who) {
        Vec3 forward = Motion.horizontalLook(who);
        Motion.setVelocity(who, new Vec3(forward.x * HOP_OFF_FORWARD, HOP_OFF_UP, forward.z * HOP_OFF_FORWARD));
        who.fallDistance = 0.0f;
        animate(who, ANIM_DISMOUNT);
    }

    private LivingEntity findCaster() {
        if (casterId == null || !(level() instanceof ServerLevel server)) return null;
        return server.getEntity(casterId) instanceof LivingEntity living ? living : null;
    }

    /** Plays a player animation on this entity for everyone who can see it. */
    public static void animate(LivingEntity entity, String animation) {
        PacketDistributor.sendToPlayersTrackingEntityAndSelf(entity,
                new CastAnimationPayload(entity.getId(), OtherWorlds.id(animation), true));
    }

    // ---------- client + rider: steering ----------

    /** Runs on the rider's own game. Reads what they are pressing and works out this tick's movement. */
    private void steer(LivingEntity rider) {
        setYRot(rider.getYRot());   // faces where the rider looks
        double topSpeed = entityData.get(TOP_SPEED);

        // What the rider is pressing, turned into a direction on the ground. zza is forward, xxa is left.
        double yaw = Math.toRadians(getYRot());
        double sin = Math.sin(yaw);
        double cos = Math.cos(yaw);
        double wishX = -sin * rider.zza + cos * rider.xxa;
        double wishZ = cos * rider.zza + sin * rider.xxa;
        double wishLength = Math.sqrt(wishX * wishX + wishZ * wishZ);
        boolean pushing = wishLength > 0.01;
        if (wishLength > 1.0) {
            wishX /= wishLength;
            wishZ /= wishLength;
        }

        // Sideways: speed up toward the wanted direction, or coast
        Vec3 velocity = getDeltaMovement();
        double vx = velocity.x;
        double vz = velocity.z;
        if (pushing) {
            vx += (wishX * topSpeed - vx) * ACCELERATION;
            vz += (wishZ * topSpeed - vz) * ACCELERATION;
        } else {
            vx *= COAST;
            vz *= COAST;
        }

        // Where it wants to be vertically: hover above whatever is below or ahead, or fall if there is nothing
        double surface = hoverSurface(vx, vz);
        double hoverVy;
        if (!Double.isNaN(surface)) {
            double gap = surface + HOVER_HEIGHT - getY();
            hoverVy = Mth.clamp(gap * (gap > 0 ? RISE_PULL : HOVER_PULL), -MAX_SINK, MAX_RISE);
        } else {
            hoverVy = Math.max(velocity.y - FALL_ACCEL, -MAX_FALL);
        }

        // Autostep: if this tick's move would hit something low enough to step onto, rise by just enough as part of the
        // same move, so it keeps its speed. It only takes over when there really is something to step onto (lift above 0),
        // so the hover is free to sink the ball back down. If the obstacle is too tall to step onto, the wall climb takes over.
        double lift = stepLift(vx, vz);
        double vy;
        if (lift > 0.0) {
            vy = Math.max(hoverVy, lift);
        } else if (pushing && horizontalCollision) {
            vy = CLIMB_SPEED;
        } else {
            vy = hoverVy;
        }

        setDeltaMovement(vx, vy, vz);
    }

    /**
     * How far it must rise, this tick, for the sideways move (vx, vz) to be free of blocks. 0 if the way is already clear,
     * or if whatever is in the way is too tall to step onto (then the wall climb takes over).
     */
    private double stepLift(double vx, double vz) {
        if (vx * vx + vz * vz < 1.0E-4) return 0.0;

        AABB box = getBoundingBox();
        if (level().noCollision(this, box.move(vx, 0.0, vz))) return 0.0;

        for (double h = STEP_INCREMENT; h <= STEP_MAX_RISE + 1.0E-6; h += STEP_INCREMENT) {
            if (level().noCollision(this, box.move(vx, h + STEP_MARGIN, vz))) {
                return h + STEP_MARGIN;
            }
        }
        return 0.0;
    }

    /**
     * The height to hover over: the highest ground under the ball, just behind it, and just ahead of it in the direction
     * it is travelling. Looking ahead is what lets it rise before a fence reaches its front edge and glide over.
     * NaN if there is nothing within reach.
     */
    private double hoverSurface(double vx, double vz) {
        double best = surfaceAt(getX(), getZ(), getY() + CENTER_PROBE_UP, true);

        double speed = Math.sqrt(vx * vx + vz * vz);
        if (speed < LOOKAHEAD_MIN_SPEED) return best;

        double dx = vx / speed;      // direction of travel
        double dz = vz / speed;
        double sx = -dz;             // to its side
        double sz = dx;
        double startY = getY() + AHEAD_PROBE_UP;
        double far = AHEAD_NEAR + speed * LOOKAHEAD_TICKS;

        best = higher(best, surfaceAt(getX() - dx * BEHIND, getZ() - dz * BEHIND, startY, false));
        best = higher(best, surfaceAt(getX() + dx * AHEAD_NEAR, getZ() + dz * AHEAD_NEAR, startY, false));
        best = higher(best, surfaceAt(getX() + dx * AHEAD_NEAR + sx * AHEAD_SIDE,
                getZ() + dz * AHEAD_NEAR + sz * AHEAD_SIDE, startY, false));
        best = higher(best, surfaceAt(getX() + dx * AHEAD_NEAR - sx * AHEAD_SIDE,
                getZ() + dz * AHEAD_NEAR - sz * AHEAD_SIDE, startY, false));
        best = higher(best, surfaceAt(getX() + dx * far, getZ() + dz * far, startY, false));
        return best;
    }

    /**
     * Height of the ground or fluid surface straight below a point, or NaN if there is nothing within reach.
     * A check that starts inside a block (a ceiling or a tall wall) is ignored unless allowInside is set.
     */
    private double surfaceAt(double x, double z, double startY, boolean allowInside) {
        Vec3 from = new Vec3(x, startY, z);
        BlockHitResult hit = level().clip(new ClipContext(from, from.add(0, -(PROBE_DEPTH + 2.0), 0),
                ClipContext.Block.COLLIDER, ClipContext.Fluid.ANY, this));
        if (hit.getType() == HitResult.Type.MISS) return Double.NaN;
        if (hit.isInside() && !allowInside) return Double.NaN;
        return hit.getLocation().y;
    }

    private static double higher(double a, double b) {
        if (Double.isNaN(a)) return b;
        if (Double.isNaN(b)) return a;
        return Math.max(a, b);
    }

    /** A ring of wind turning under the ball. Replaced by the model later. */
    private void swirl() {
        double angle = tickCount * 0.5;
        for (int i = 0; i < 2; i++) {
            double a = angle + i * Math.PI;
            level().addParticle(ParticleTypes.CLOUD,
                    getX() + Math.cos(a) * 0.55, getY() + 0.05, getZ() + Math.sin(a) * 0.55,
                    -Math.sin(a) * 0.05, 0.0, Math.cos(a) * 0.05);
        }
    }

    // ---------- damage ----------

    @Override
    public boolean causeFallDamage(float fallDistance, float multiplier, DamageSource source) {
        return false;   // the rider would take it, so nobody does
    }

    @Override
    public boolean isInvulnerableTo(DamageSource source) {
        return true;
    }

    // ---------- saving: never saved ----------

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
    }
}