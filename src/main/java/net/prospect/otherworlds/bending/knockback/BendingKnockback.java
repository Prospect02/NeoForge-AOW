package net.prospect.otherworlds.bending.knockback;

import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.prospect.otherworlds.Config;
import net.prospect.otherworlds.OtherWorlds;
import net.prospect.otherworlds.bending.damage.ModDamageTypes;
import net.prospect.otherworlds.network.CameraLockPayload;
import net.prospect.otherworlds.network.WallStuckPayload;
import net.prospect.otherworlds.registry.ModSounds;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import java.util.List;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/**
 * Bending knockback with three extras:
 *  1. A faster launch that brakes harder, so things move quicker without flying much further.
 *  2. Wall damage: an entity that hits a wall while it still has more than 25 percent of the top speed
 *     it reached takes bending damage, more the faster it was going.
 *  3. Wall pin: after a damaging wall hit the entity is held in place for a moment, back against the wall.
 *     Gravity, walking and jumping are off for everything, players cannot turn their camera, and mobs have
 *     their AI switched off. Nearby clients are told it is stuck (WallStuckPayload) for animations and marks.
 *
 * Use knock(...) instead of entity.push(...) for any bending move that throws a creature.
 */
@EventBusSubscriber(modid = OtherWorlds.MODID)
public final class BendingKnockback {
    private BendingKnockback() {}

    private static final Logger LOGGER = LogUtils.getLogger();

    // ---------- tuning ----------

    /** Prints what the tracker is doing to the console. Turn off once it feels right. */
    private static final boolean DEBUG = true;

    /** Horizontal launch speed is multiplied by this. 1.6 means 60 percent faster. */
    private static final double SPEED_BOOST = 1.6;

    /**
     * Extra braking each tick while flying, multiplied on top of normal drag. These keep the distance
     * near what it was before the boost. For boost B and wanted distance factor D (1.1 = 10 percent further):
     *   extra = (1 - B * (1 - drag) / D) / drag      where drag is 0.91 in the air, about 0.546 on the ground
     */
    private static final double EXTRA_DRAG_AIR = 0.955;
    private static final double EXTRA_DRAG_GROUND = 0.65;

    /** Players are moved by their own game, so their braking is done by measuring where they went. */
    private static final boolean DAMP_PLAYERS = true;

    /** Hitting a wall only hurts while the speed is still above this share of the top speed. */
    private static final double MOMENTUM_LIMIT = 0.25;

    private static final double WALL_DAMAGE_PER_SPEED = 4.0;   // damage per block/tick of speed
    private static final double WALL_DAMAGE_OFFSET = 1.0;      // taken off, so slow bumps do nothing
    private static final double WALL_DAMAGE_MAX = 15.0;

    /** How long an entity stays stuck on the wall after a damaging hit. 40 ticks is 2 seconds. */
    private static final int PIN_TICKS = 40;

    /** How far it is pushed into the wall, as a share of its body width (about 0.06 blocks for a zombie). */
    private static final double EMBED_FRACTION = 0.10;

    /** Taken off the full amount so it never counts as suffocating in the wall. */
    private static final double EMBED_MARGIN = 0.01;

    /** How far behind it to look for the wall face. */
    private static final double EMBED_PROBE = 0.5;

    private static final float IMPACT_VOLUME = 1.4f;
    private static final float IMPACT_PITCH = 1.0f;

    private static final int MAX_TICKS = 80;                   // stop tracking a flight after 4 seconds
    private static final int PLAYER_GRACE_TICKS = 8;           // a player's position lags behind by their ping
    private static final int START_WAIT_TICKS = 3;             // wait this long for the push to start moving the entity

    private static final ResourceLocation PIN_MODIFIER_ID =
            ResourceLocation.fromNamespaceAndPath(OtherWorlds.MODID, "wall_pin");

    /** Saved on the entity while pinned, so it can be put right if it is unloaded or the server crashes mid-pin. */
    private static final String PIN_TAG = "otherworlds_wall_pin";

    // ---------- state ----------

    private static final Map<UUID, State> TRACKED = new HashMap<>();
    private static final Map<UUID, Pin> PINNED = new HashMap<>();

    private static final class State {
        final ResourceKey<Level> dimension;
        final @Nullable UUID owner;
        Vec3 lastPos;
        Vec3 lastDir;       // the way it is travelling, flat. Starts as the push direction.
        double peak;        // fastest horizontal speed seen, blocks per tick
        double lastSpeed;   // horizontal speed on the previous tick
        boolean hasMoved;   // has it actually been seen moving yet?
        int age;

        State(ResourceKey<Level> dimension, @Nullable UUID owner, Vec3 pos, Vec3 dir) {
            this.dimension = dimension;
            this.owner = owner;
            this.lastPos = pos;
            this.lastDir = dir;
        }
    }

    /** An entity stuck on a wall. Kept for the facing, the clients, and the marks and animation later. */
    private static final class Pin {
        final ResourceKey<Level> dimension;
        final Direction wall;       // the side of the wall it is against, the way it was flying when it hit
        final BlockPos pos;         // the block it is stuck in
        final Vec3 impactPos;       // exactly where it stopped
        final float facingYaw;      // looking away from the wall, so its back is against it
        final boolean hadNoGravity; // so we give back exactly what it had before
        final boolean hadNoAi;
        int ticksLeft;

        Pin(ResourceKey<Level> dimension, Direction wall, BlockPos pos, Vec3 impactPos, float facingYaw,
            boolean hadNoGravity, boolean hadNoAi, int ticksLeft) {
            this.dimension = dimension;
            this.wall = wall;
            this.pos = pos;
            this.impactPos = impactPos;
            this.facingYaw = facingYaw;
            this.hadNoGravity = hadNoGravity;
            this.hadNoAi = hadNoAi;
            this.ticksLeft = ticksLeft;
        }
    }

    // ---------- using it ----------

    /** Is this entity currently stuck on a wall? */
    public static boolean isPinned(Entity entity) {
        return PINNED.containsKey(entity.getUUID());
    }

    /** Throws a creature. x, y, z is the push you would have given entity.push(...). The owner gets the credit. */
    public static void knock(LivingEntity target, @Nullable Entity owner, double x, double y, double z) {
        if (isPinned(target)) return;   // stuck on a wall, it is not going anywhere

        target.push(x * SPEED_BOOST, y, z * SPEED_BOOST);
        target.hurtMarked = true;   // makes the push show up for players

        if (!(target.level() instanceof ServerLevel level)) return;
        Vec3 v = target.getDeltaMovement();
        double flat = Math.sqrt(v.x * v.x + v.z * v.z);
        Vec3 dir = flat > 1.0e-4 ? new Vec3(v.x / flat, 0, v.z / flat) : Vec3.ZERO;

        State state = new State(level.dimension(), owner == null ? null : owner.getUUID(), target.position(), dir);
        state.peak = flat;
        // A player's real speed is only known once their position arrives, so they start at 0.
        // Everything else starts at the launch speed, so standing right against a wall still counts.
        state.lastSpeed = target instanceof ServerPlayer ? 0.0 : flat;
        TRACKED.put(target.getUUID(), state);

        if (DEBUG) LOGGER.info("[Knockback] start {} launch={}", target.getName().getString(), String.format("%.2f", flat));
    }

    // ---------- every tick ----------

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();

        if (!TRACKED.isEmpty()) {
            Iterator<Map.Entry<UUID, State>> it = TRACKED.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<UUID, State> entry = it.next();
                State state = entry.getValue();
                ServerLevel level = server.getLevel(state.dimension);
                Entity entity = level == null ? null : level.getEntity(entry.getKey());
                if (!(entity instanceof LivingEntity target) || !target.isAlive()) {
                    it.remove();
                    continue;
                }
                Entity owner = state.owner == null ? null : level.getEntity(state.owner);
                if (!step(level, target, owner, state)) it.remove();
            }
        }

        if (!PINNED.isEmpty()) tickPins(server);
    }

    /** Returns false when this entity should no longer be tracked. */
    private static boolean step(ServerLevel level, LivingEntity target, @Nullable Entity owner, State s) {
        boolean player = target instanceof ServerPlayer;
        Vec3 pos = target.position();
        Vec3 moved = pos.subtract(s.lastPos);
        s.lastPos = pos;
        s.age++;

        double speed = Math.sqrt(moved.x * moved.x + moved.z * moved.z);
        if (speed > 0.02) s.lastDir = new Vec3(moved.x / speed, 0, moved.z / speed);

        // Hit a wall while it still had momentum? Judged by the speed it had on the previous tick.
        // Either the game says it collided, or it lost most of its speed in one tick. Players only get the second,
        // since their collisions happen on their own game.
        boolean drop = s.hasMoved && s.lastSpeed > 0.0 && speed < s.lastSpeed * 0.3;
        boolean stopped = player ? drop : (target.horizontalCollision || drop);
        if (stopped && s.lastSpeed > 0.0 && s.lastSpeed >= MOMENTUM_LIMIT * s.peak) {
            if (wallAhead(level, target, s.lastDir)) {
                wallHit(level, target, owner, s.lastSpeed, s.lastDir);
                return false;
            } else if (DEBUG) {
                LOGGER.info("[Knockback] {} stopped but no wall ahead (collision={}, drop={})",
                        target.getName().getString(), target.horizontalCollision, drop);
            }
        }

        if (speed > 0.05) s.hasMoved = true;

        // The push has not moved it yet (it may tick after the cast): wait a few ticks instead of giving up
        if (s.age <= START_WAIT_TICKS && speed < 0.01) return true;

        s.peak = Math.max(s.peak, speed);
        s.lastSpeed = speed;

        // Momentum used up, or tracked long enough
        boolean graceOver = !player || s.age > PLAYER_GRACE_TICKS;
        if (graceOver && speed < MOMENTUM_LIMIT * s.peak) {
            if (DEBUG) LOGGER.info("[Knockback] {} ended: slowed below {}% (speed={}, peak={}, age={})",
                    target.getName().getString(), (int) (MOMENTUM_LIMIT * 100),
                    String.format("%.2f", speed), String.format("%.2f", s.peak), s.age);
            return false;
        }
        if (s.age > MAX_TICKS) {
            if (DEBUG) LOGGER.info("[Knockback] {} ended: timed out", target.getName().getString());
            return false;
        }

        // The extra braking that keeps the distance down
        boolean onGround = target.onGround();
        double extra = onGround ? EXTRA_DRAG_GROUND : EXTRA_DRAG_AIR;
        if (!player) {
            Vec3 v = target.getDeltaMovement();
            target.setDeltaMovement(v.x * extra, v.y, v.z * extra);
        } else if (DAMP_PLAYERS) {
            double baseDrag = onGround
                    ? level.getBlockState(target.blockPosition().below()).getBlock().getFriction() * 0.91
                    : 0.91;
            double vy = onGround ? 0.0 : (moved.y - 0.08) * 0.98;   // what gravity would leave them with
            target.setDeltaMovement(moved.x * baseDrag * extra, vy, moved.z * baseDrag * extra);
            target.hurtMarked = true;
        }
        return true;
    }

    /** Is there a solid block just ahead, in the direction it was travelling? */
    private static boolean wallAhead(ServerLevel level, LivingEntity target, Vec3 dir) {
        if (dir.lengthSqr() < 1.0e-4) return false;
        AABB box = target.getBoundingBox().move(dir.x * 0.4, 0.05, dir.z * 0.4);   // a little up, to ignore the floor
        return !level.noCollision(target, box);
    }

    private static void wallHit(ServerLevel level, LivingEntity target, @Nullable Entity owner,
                                double impactSpeed, Vec3 wallDir) {
        double damage = Mth.clamp(impactSpeed * WALL_DAMAGE_PER_SPEED - WALL_DAMAGE_OFFSET, 0.0, WALL_DAMAGE_MAX);
        if (damage <= 0.0) {
            if (DEBUG) LOGGER.info("[Knockback] {} hit a wall too slowly (speed={})",
                    target.getName().getString(), String.format("%.2f", impactSpeed));
            return;
        }

        boolean hurt = target.hurt(ModDamageTypes.bending(level, null, owner),
                (float) (damage * Config.BENDING_DAMAGE_MULTIPLIER.get()));

        if (DEBUG) LOGGER.info("[Knockback] {} WALL HIT speed={} damage={} hurt={}",
                target.getName().getString(), String.format("%.2f", impactSpeed),
                String.format("%.1f", damage), hurt);

        // The impact: a burst and the wall impact sound (the audio itself is set in sounds.json)
        Vec3 p = target.position().add(0, target.getBbHeight() * 0.5, 0);
        level.sendParticles(ParticleTypes.CRIT, p.x, p.y, p.z, 12, 0.3, 0.3, 0.3, 0.2);
        level.playSound(null, p.x, p.y, p.z, ModSounds.WALL_IMPACT.get(), SoundSource.PLAYERS,
                IMPACT_VOLUME, IMPACT_PITCH);

        // Stuck on the wall, unless the hit killed it
        if (target.isAlive()) pin(level, target, wallDir);
    }

    // ---------- the pin ----------

    /** Holds the entity where it is, back against the wall, and tells nearby clients it is stuck. */
    private static void pin(ServerLevel level, LivingEntity target, Vec3 wallDir) {
        if (PINNED.containsKey(target.getUUID())) return;

        // Which wall: the way it was flying, snapped to the nearest side since blocks are square.
        // Its back is to the wall, so it looks the opposite way.
        Direction wall = Direction.getNearest(wallDir.x, 0.0, wallDir.z);
        float facingYaw = wall.getOpposite().toYRot();

        boolean hadNoAi = target instanceof Mob mob && mob.isNoAi();
        Pin pin = new Pin(level.dimension(), wall, target.blockPosition(), target.position(), facingYaw,
                target.isNoGravity(), hadNoAi, PIN_TICKS);
        PINNED.put(target.getUUID(), pin);

        // Saved with the entity until it is released, in case it is unloaded or the server crashes while pinned
        CompoundTag saved = new CompoundTag();
        saved.putBoolean("hadNoGravity", pin.hadNoGravity);
        saved.putBoolean("hadNoAi", pin.hadNoAi);
        target.getPersistentData().put(PIN_TAG, saved);

        target.setNoGravity(true);
        target.setDeltaMovement(Vec3.ZERO);
        target.fallDistance = 0.0f;
        target.hurtMarked = true;

        // No walking and no jumping. These are temporary (not saved) and work for players too, since they sync.
        addPinModifier(target.getAttribute(Attributes.MOVEMENT_SPEED), -1.0);
        addPinModifier(target.getAttribute(Attributes.JUMP_STRENGTH), -1.0);

        // Mobs: AI off, so they cannot chase, attack or turn while stuck
        if (target instanceof Mob mob) {
            mob.getNavigation().stop();
            mob.setNoAi(true);
        }

        // Back against the wall. A player's view is turned by sending them their own position and rotation.
        // This goes before the camera lock below, so the lock holds the turned view.
        // Driven into the wall by the force of the hit, then turned back to the wall
        Vec3 toward = new Vec3(wall.getStepX(), 0.0, wall.getStepZ());
        Vec3 stuckPos = target.position().add(toward.scale(embedDistance(target, toward)));
        target.moveTo(stuckPos.x, stuckPos.y, stuckPos.z, facingYaw, 0.0f);
        face(target, facingYaw);
        if (target instanceof ServerPlayer p) {
            p.connection.teleport(p.getX(), p.getY(), p.getZ(), facingYaw, 0.0f);
            PacketDistributor.sendToPlayer(p, new CameraLockPayload(true));
        }

        // Tell everyone who can see it (and the player themselves)
        sendStuck(target, true, pin);

        if (DEBUG) LOGGER.info("[Knockback] {} pinned for {} ticks, wall={}",
                target.getName().getString(), PIN_TICKS, wall);
    }

    private static void face(LivingEntity target, float yaw) {
        target.setYRot(yaw);
        target.yRotO = yaw;
        target.setXRot(0.0f);
        target.xRotO = 0.0f;
        target.yHeadRot = yaw;
        target.yHeadRotO = yaw;
        target.yBodyRot = yaw;
        target.yBodyRotO = yaw;
    }

    /**
     * How far to move toward the wall: the gap still left to its face, plus a slice of its body width.
     * Zero if there is no wall within reach behind it.
     */
    private static double embedDistance(LivingEntity target, Vec3 toward) {
        Vec3 free = Entity.collideBoundingBox(target, toward.scale(EMBED_PROBE),
                target.getBoundingBox(), target.level(), List.of());
        double gap = free.length();
        if (gap >= EMBED_PROBE - 1.0e-6) return 0.0;   // no wall right behind it

        double depth = Math.max(0.0, EMBED_FRACTION * target.getBbWidth() - EMBED_MARGIN);
        if (DEBUG) LOGGER.info("[Knockback] {} embedded: gap={} depth={}",
                target.getName().getString(), String.format("%.3f", gap), String.format("%.3f", depth));
        return gap + depth;
    }
    private static void sendStuck(LivingEntity target, boolean stuck, Pin pin) {
        PacketDistributor.sendToPlayersTrackingEntityAndSelf(target,
                new WallStuckPayload(target.getId(), stuck, pin.wall.get2DDataValue(), pin.pos));
    }

    private static void addPinModifier(@Nullable AttributeInstance attribute, double amount) {
        if (attribute == null) return;
        attribute.removeModifier(PIN_MODIFIER_ID);
        // Multiplied total of -1 leaves the stat at 0 no matter what else is on it, such as sprinting
        attribute.addTransientModifier(new AttributeModifier(PIN_MODIFIER_ID, amount,
                AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
    }

    private static void tickPins(MinecraftServer server) {
        Iterator<Map.Entry<UUID, Pin>> it = PINNED.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, Pin> entry = it.next();
            Pin pin = entry.getValue();
            ServerLevel level = server.getLevel(pin.dimension);
            Entity entity = level == null ? null : level.getEntity(entry.getKey());

            if (!(entity instanceof LivingEntity target) || !target.isAlive()) {
                // Dead: clean up anyway. Unloaded: it is put right by the saved tag when it loads again.
                if (entity instanceof LivingEntity dead) release(dead, pin);
                it.remove();
                continue;
            }

            if (--pin.ticksLeft <= 0) {
                release(target, pin);
                it.remove();
                continue;
            }

            // Hold it still: nothing carries it off the wall
            target.setDeltaMovement(Vec3.ZERO);
            target.fallDistance = 0.0f;
            target.hurtMarked = true;

            // Keep its back to the wall. Players are held by their locked camera instead.
            if (!(target instanceof ServerPlayer)) face(target, pin.facingYaw);
        }
    }

    private static void release(LivingEntity target, Pin pin) {
        target.setNoGravity(pin.hadNoGravity);
        target.getPersistentData().remove(PIN_TAG);
        removePinModifier(target.getAttribute(Attributes.MOVEMENT_SPEED));
        removePinModifier(target.getAttribute(Attributes.JUMP_STRENGTH));
        if (target instanceof Mob mob) mob.setNoAi(pin.hadNoAi);
        target.hurtMarked = true;
        if (target instanceof ServerPlayer freed) {
            PacketDistributor.sendToPlayer(freed, new CameraLockPayload(false));
        }
        sendStuck(target, false, pin);
        if (DEBUG) LOGGER.info("[Knockback] {} released", target.getName().getString());
    }

    private static void removePinModifier(@Nullable AttributeInstance attribute) {
        if (attribute != null) attribute.removeModifier(PIN_MODIFIER_ID);
    }

    // ---------- cleanup and late joiners ----------

    /** A player who comes into range of an entity that is already stuck needs to be told too. */
    @SubscribeEvent
    public static void onStartTracking(PlayerEvent.StartTracking event) {
        if (!(event.getEntity() instanceof ServerPlayer viewer)) return;
        Entity tracked = event.getTarget();
        Pin pin = PINNED.get(tracked.getUUID());
        if (pin == null) return;
        PacketDistributor.sendToPlayer(viewer,
                new WallStuckPayload(tracked.getId(), true, pin.wall.get2DDataValue(), pin.pos));
    }

    /** No gravity and no AI are saved with the entity, so give everything back before the world saves. */
    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        MinecraftServer server = event.getServer();
        for (Map.Entry<UUID, Pin> entry : new ArrayList<>(PINNED.entrySet())) {
            ServerLevel level = server.getLevel(entry.getValue().dimension);
            Entity entity = level == null ? null : level.getEntity(entry.getKey());
            if (entity instanceof LivingEntity living) release(living, entry.getValue());
        }
        PINNED.clear();
        TRACKED.clear();
    }

    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        Pin pin = PINNED.remove(player.getUUID());
        if (pin != null) {
            player.setNoGravity(pin.hadNoGravity);   // no packet: they've already left
            player.getPersistentData().remove(PIN_TAG);
        }
        TRACKED.remove(player.getUUID());
    }

    /**
     * Safety net: an entity that was pinned when it was saved (unloaded with its chunk, or a crash) comes back
     * with no gravity and maybe no AI. Put it right as it loads.
     */
    @SubscribeEvent
    public static void onEntityJoin(EntityJoinLevelEvent event) {
        Entity entity = event.getEntity();
        if (event.getLevel().isClientSide() || PINNED.containsKey(entity.getUUID())) return;

        CompoundTag data = entity.getPersistentData();
        if (!data.contains(PIN_TAG)) return;

        CompoundTag saved = data.getCompound(PIN_TAG);
        entity.setNoGravity(saved.getBoolean("hadNoGravity"));
        if (entity instanceof Mob mob) mob.setNoAi(saved.getBoolean("hadNoAi"));
        data.remove(PIN_TAG);
        if (DEBUG) LOGGER.info("[Knockback] {} restored after being saved while pinned", entity.getName().getString());
    }
}