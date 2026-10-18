package net.prospect.otherworlds.bending.ability;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.network.PacketDistributor;
import net.prospect.otherworlds.OtherWorlds;
import net.prospect.otherworlds.bending.chi.ChiHandler;
import net.prospect.otherworlds.network.CastAnimationPayload;
import net.prospect.otherworlds.network.CastStatePayload;
import net.prospect.otherworlds.registry.ModAttachments;
import net.prospect.otherworlds.bending.ability.Click;

/** Every cast, from the click combo, a command, or a mob, goes through here. Server side only. */
public class AbilityManager {
    private static final double QUALITY_FAST = 1.2;   // combo finished instantly
    private static final double QUALITY_SLOW = 0.8;   // combo finished at the last moment
    private static final int FAIL_COOLDOWN_TICKS = 20;

    // ---------- starting a cast ----------

    /** The player route: opens the combo window. The combo then arrives through input(). */
    public static AbilityResult startCast(LivingEntity user, Ability ability) {
        AbilityResult result = tryUse(user, ability, 1.0, false);
        sync(user);
        return result;
    }

    /** Skips the combo, charge and impact delay. For mobs and debug commands. */
    public static AbilityResult castNow(LivingEntity user, Ability ability, double quality) {
        return tryUse(user, ability, quality, true);
    }

    private static AbilityResult tryUse(LivingEntity user, Ability ability, double quality, boolean skipCombo) {
        if (!(user.level() instanceof ServerLevel level)) return AbilityResult.FAILED;

        CastTracker tracker = user.getData(ModAttachments.CASTING);
        if (tracker.isCasting()) {
            // Using an active held or toggle move again turns it off.
            boolean sameActiveMove = tracker.ability == ability
                    && tracker.phase == CastTracker.Phase.ACTIVE
                    && ability.getCastType() != CastType.INSTANT;
            if (sameActiveMove) {
                stop(user, EndReason.RELEASED);
                return AbilityResult.SUCCESS;
            }
            return AbilityResult.BUSY;
        }

        // Later phases add "has this player learned the move?" checks here.
        ResourceLocation id = ability.getId();
        if (!user.getData(ModAttachments.COOLDOWNS).isReady(id, level.getGameTime())) {
            return AbilityResult.ON_COOLDOWN;
        }
        if (user instanceof ServerPlayer player && ChiHandler.getChi(player) < ability.getChiCost()) {
            return AbilityResult.NOT_ENOUGH_CHI;
        }

        tracker.start(ability, quality);
        ability.onCastStart(ctx(user, level, tracker, 0.0));

        if (skipCombo || ability.getCombo().length() == 0) {
            playAnimation(user, ability);
            return release(user, level, tracker, 0.0);
        }
        if (ability.getComboStyle() == ComboStyle.CAST_ALONG) {
            playAnimation(user, ability);   // the animation plays while the combo is entered
        }
        return AbilityResult.SUCCESS;   // combo window is open
    }

    // ---------- input ----------

    /** A left or right click from the player (or a stand-in command). */
    public static void input(LivingEntity user, Click click) {
        handleInput(user, click);
        sync(user);
    }

    /** The player let go of a click. Only matters for hold-charge moves. */
    public static void inputRelease(LivingEntity user, Click click) {
        OtherWorlds.LOGGER.info("[DEBUG] release received: {} phase={}", click, user.getData(ModAttachments.CASTING).phase);
        if (!(user.level() instanceof ServerLevel level)) return;
        CastTracker tracker = user.getData(ModAttachments.CASTING);
        if (!tracker.isCasting() || tracker.phase != CastTracker.Phase.CHARGE) return;
        if (!tracker.ability.isReleaseOnLetGo() || tracker.holdClick != click) return;

        beginRelease(user, level, tracker, chargeFraction(tracker));
        sync(user);
    }

    private static void handleInput(LivingEntity user, Click click) {
        if (!(user.level() instanceof ServerLevel level)) return;
        CastTracker tracker = user.getData(ModAttachments.CASTING);
        if (!tracker.isCasting()) return;
        Ability ability = tracker.ability;

        switch (tracker.phase) {
            case COMBO -> {
                ComboPattern combo = ability.getCombo();
                if (combo.get(tracker.step) != click) {
                    fail(user, FailReason.WRONG_CLICK);
                    return;
                }
                tracker.step++;
                ability.onComboStep(ctx(user, level, tracker, 0.0), tracker.step, combo.length());

                if (tracker.step >= combo.length()) {
                    // Faster combo = higher quality
                    double used = Mth.clamp((double) tracker.ticks / ability.getComboWindowTicks(), 0.0, 1.0);
                    tracker.quality = Mth.lerp(used, QUALITY_FAST, QUALITY_SLOW);

                    // Strike moves start their animation once the combo is finished
                    if (ability.getComboStyle() == ComboStyle.STRIKE) {
                        playAnimation(user, ability);
                    }

                    if (ability.getMaxChargeTicks() > 0) {
                        tracker.phase = CastTracker.Phase.CHARGE;
                        tracker.ticks = 0;
                        if (ability.isReleaseOnLetGo()) tracker.holdClick = click;   // fires when this is let go
                    } else {
                        beginRelease(user, level, tracker, 0.0);
                    }
                }
            }
            case CHARGE -> {
                // Hold-charge moves ignore further presses and fire on release instead.
                if (!ability.isReleaseOnLetGo()) {
                    beginRelease(user, level, tracker, chargeFraction(tracker));
                }
            }
            case RELEASE_DELAY, ACTIVE -> { }   // clicks do nothing here
        }
    }

    // ---------- every tick ----------

    public static void tick(LivingEntity user) {
        if (!(user.level() instanceof ServerLevel level)) return;
        CastTracker tracker = user.getData(ModAttachments.CASTING);
        if (!tracker.isCasting()) return;
        Ability ability = tracker.ability;
        tracker.ticks++;

        switch (tracker.phase) {
            case COMBO -> {
                if (tracker.ticks > ability.getComboWindowTicks()) {
                    fail(user, FailReason.TIMEOUT);
                    return;
                }
                ability.tickCasting(ctx(user, level, tracker, 0.0), tracker.ticks, tracker.step);
            }
            case CHARGE -> {
                double charge = chargeFraction(tracker);
                ability.tickCharge(ctx(user, level, tracker, charge), charge);
                if (tracker.ticks >= ability.getMaxChargeTicks()) {
                    beginRelease(user, level, tracker, 1.0);   // fully charged: fires on its own
                }
            }
            case RELEASE_DELAY -> {
                if (tracker.ticks >= ability.getReleaseDelayTicks()) {
                    release(user, level, tracker, tracker.charge);
                }
            }
            case ACTIVE -> {
                if (user instanceof ServerPlayer player && ability.getChiPerTick() > 0) {
                    if (!ChiHandler.trySpend(player, ability.getChiPerTick())) {
                        stop(user, EndReason.OUT_OF_CHI);
                        return;
                    }
                }
                if (!ability.tickActive(ctx(user, level, tracker, 0.0), tracker.ticks)) {
                    stop(user, EndReason.FINISHED);
                }
            }
        }
    }

    // ---------- ending ----------

    /** Ends whatever the entity is casting, in any phase. Safe to call when it isn't casting. */
    public static void stop(LivingEntity user, EndReason reason) {
        if (!(user.level() instanceof ServerLevel level)) return;
        CastTracker tracker = user.getData(ModAttachments.CASTING);
        if (!tracker.isCasting()) return;

        if (tracker.phase == CastTracker.Phase.ACTIVE) {
            // A move that fired: cleanup and cooldown
            tracker.ability.onEnd(ctx(user, level, tracker, 0.0), reason);
            startCooldown(user, level, tracker.ability, tracker.ability.getCooldownTicks());
            stopAnimation(user);
            tracker.clear();
        } else {
            // Still in the combo, charge or impact delay: it never fired
            fail(user, reason == EndReason.INTERRUPTED ? FailReason.INTERRUPTED : FailReason.CANCELLED);
        }
    }

    /** The cast ends before the move fires. */
    public static void fail(LivingEntity user, FailReason reason) {
        if (!(user.level() instanceof ServerLevel level)) return;
        CastTracker tracker = user.getData(ModAttachments.CASTING);
        if (!tracker.isCasting() || tracker.phase == CastTracker.Phase.ACTIVE) return;
        Ability ability = tracker.ability;

        ability.onFail(ctx(user, level, tracker, 0.0), reason);

        // Mistakes cost chi and a short cooldown. Interruptions and cancels are free.
        if (reason == FailReason.WRONG_CLICK || reason == FailReason.TIMEOUT) {
            if (user instanceof ServerPlayer player) {
                ChiHandler.setChi(player, ChiHandler.getChi(player) - ability.getChiCost() * ability.getFailChiFraction());
            }
            startCooldown(user, level, ability, FAIL_COOLDOWN_TICKS);
        }
        stopAnimation(user);
        tracker.clear();
    }

    /** Called when the entity takes damage. */
    public static void onHurt(LivingEntity user) {
        CastTracker tracker = user.getData(ModAttachments.CASTING);
        if (tracker.isCasting() && tracker.ability.getAnimation().interruptible()) {
            stop(user, EndReason.INTERRUPTED);
        }
    }

    /**
     * Tells a player's client whether clicks are being collected right now.
     * Only sends when something changed. Called after starts, after clicks, and every tick.
     */
    public static void sync(LivingEntity user) {
        if (!(user instanceof ServerPlayer player)) return;
        CastTracker tracker = player.getData(ModAttachments.CASTING);

        boolean open = tracker.isCasting()
                && (tracker.phase == CastTracker.Phase.COMBO || tracker.phase == CastTracker.Phase.CHARGE);
        int step = open ? tracker.step : 0;
        if (open == tracker.sentOpen && step == tracker.sentStep) return;

        tracker.sentOpen = open;
        tracker.sentStep = step;
        ResourceLocation id = open ? tracker.ability.getId() : OtherWorlds.id("none");
        int total = open ? tracker.ability.getCombo().length() : 0;
        PacketDistributor.sendToPlayer(player, new CastStatePayload(open, id, step, total));
    }

    // ---------- animation ----------

    /** Tells every client that can see this entity to start the ability's animation. */
    private static void playAnimation(LivingEntity user, Ability ability) {
        AnimationSpec spec = ability.getAnimation();
        if (spec == null || spec.durationTicks() <= 0) return;   // moves without an animation yet
        String name = spec.name();
        if (name == null || name.isEmpty()) return;
        PacketDistributor.sendToPlayersTrackingEntityAndSelf(user,
                new CastAnimationPayload(user.getId(), OtherWorlds.id(name), true));
    }

    /** Tells every client that can see this entity to stop its cast animation. */
    private static void stopAnimation(LivingEntity user) {
        PacketDistributor.sendToPlayersTrackingEntityAndSelf(user,
                new CastAnimationPayload(user.getId(), OtherWorlds.id("none"), false));
    }

    // ---------- internals ----------

    /** The combo (and charge) is done. Strike moves wait for their impact tick; others fire now. */
    private static void beginRelease(LivingEntity user, ServerLevel level, CastTracker tracker, double charge) {
        if (tracker.ability.getReleaseDelayTicks() > 0) {
            tracker.charge = charge;
            tracker.phase = CastTracker.Phase.RELEASE_DELAY;
            tracker.ticks = 0;
        } else {
            release(user, level, tracker, charge);
        }
    }

    private static AbilityResult release(LivingEntity user, ServerLevel level, CastTracker tracker, double charge) {
        Ability ability = tracker.ability;
        ServerPlayer player = user instanceof ServerPlayer p ? p : null;

        if (player != null && ChiHandler.getChi(player) < ability.getChiCost()) {
            fail(user, FailReason.NOT_ENOUGH_CHI);
            return AbilityResult.NOT_ENOUGH_CHI;
        }
        if (!ability.activate(ctx(user, level, tracker, charge))) {
            fail(user, FailReason.CANCELLED);
            return AbilityResult.FAILED;
        }
        if (player != null) ChiHandler.trySpend(player, ability.getChiCost());

        if (ability.getCastType() == CastType.INSTANT) {
            startCooldown(user, level, ability, ability.getCooldownTicks());
            tracker.clear();   // animation is left to finish on its own
        } else {
            tracker.phase = CastTracker.Phase.ACTIVE;
            tracker.ticks = 0;
        }
        return AbilityResult.SUCCESS;
    }

    private static double chargeFraction(CastTracker tracker) {
        return Mth.clamp((double) tracker.ticks / tracker.ability.getMaxChargeTicks(), 0.0, 1.0);
    }

    private static AbilityContext ctx(LivingEntity user, ServerLevel level, CastTracker tracker, double charge) {
        return new AbilityContext(user, level, tracker.quality, charge);
    }

    private static void startCooldown(LivingEntity user, ServerLevel level, Ability ability, int ticks) {
        user.getData(ModAttachments.COOLDOWNS).start(ability.getId(), level.getGameTime(), ticks);
    }
}