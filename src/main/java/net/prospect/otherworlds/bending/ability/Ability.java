package net.prospect.otherworlds.bending.ability;

import net.minecraft.Util;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.prospect.otherworlds.bending.ModAbilities;
import net.prospect.otherworlds.bending.style.BendingStyle;

import java.util.function.Supplier;

/** Base class for every bending move. All hooks run on the server only. */
public abstract class Ability {
    private static final int DEFAULT_COMBO_WINDOW = 60;   // used when a cast-along move has no animation yet

    private final Properties props;

    protected Ability(Properties props) {
        this.props = props;
    }

    // ---------- hooks, in the order they happen ----------

    /** The cast just started and the combo window opened. */
    public void onCastStart(AbilityContext context) {
    }

    /** Every tick during the combo. Lets a move keep forming something while the player casts. */
    public void tickCasting(AbilityContext context, int ticks, int step) {
    }

    /** After each correct click. step = clicks done so far. */
    public void onComboStep(AbilityContext context, int step, int total) {
    }

    /** Every tick while charging (only for moves with a charge). charge is 0..1. */
    public void tickCharge(AbilityContext context, double charge) {
    }

    /**
     * The move fires. Return true if it did. Chi and cooldown are only charged when this returns true.
     * context.quality() comes from combo speed, context.charge() from the charge phase.
     */
    public abstract boolean activate(AbilityContext context);

    /** Held and toggle moves: every tick while active. Return false to end the move. */
    public boolean tickActive(AbilityContext context, int ticksActive) {
        return true;
    }

    /** Held and toggle moves: cleanup when the move ends for any reason. */
    public void onEnd(AbilityContext context, EndReason reason) {
    }

    /** The cast failed before it fired. Override to clean up partly formed effects or to backfire. */
    public void onFail(AbilityContext context, FailReason reason) {
    }

    // ---------- info ----------

    public BendingStyle getStyle() { return props.style.get(); }
    public MoveType getType() { return props.type; }
    public Difficulty getDifficulty() { return props.difficulty; }
    public Learning getLearning() { return props.learning; }
    public CastType getCastType() { return props.castType; }
    public ComboStyle getComboStyle() { return props.comboStyle; }
    public ComboPattern getCombo() { return props.combo; }
    /** Charged once, at release. */
    public double getChiCost() { return props.chiCost; }
    /** Charged every tick while a held or toggle move is active. */
    public double getChiPerTick() { return props.chiPerTick; }
    /** 20 ticks = 1 second. Starts when the move finishes. */
    public int getCooldownTicks() { return props.cooldownTicks; }
    /** 0 means the move has no charge phase. */
    public int getMaxChargeTicks() { return props.maxChargeTicks; }
    /** True: the charge lasts while the last combo click is held, and letting go fires the move. */
    public boolean isReleaseOnLetGo() { return props.releaseOnLetGo; }
    /** Share of the chi cost lost on a wrong click or timeout. */
    public double getFailChiFraction() { return props.failChiFraction; }
    public AnimationSpec getAnimation() { return props.animation; }
    /** Element level a player needs to learn this move from a scroll. A master asks for a little less. */
    public int getRequiredElementLevel() { return props.requiredElementLevel; }

    /** Strike moves: ticks between "combo finished" and the effect landing. */
    public int getReleaseDelayTicks() {
        return props.comboStyle == ComboStyle.STRIKE ? props.animation.releaseTicks() : 0;
    }

    /**
     * The combo time limit.
     *  - Set explicitly on the move: that value.
     *  - Cast-along: the animation length.
     *  - Strike: 1 second plus half a second per click.
     */
    public int getComboWindowTicks() {
        if (props.comboWindowTicks > 0) return props.comboWindowTicks;
        if (props.comboStyle == ComboStyle.CAST_ALONG) {
            return props.animation.durationTicks() > 0 ? props.animation.durationTicks() : DEFAULT_COMBO_WINDOW;
        }
        return 20 + 10 * props.combo.length();
    }

    public ResourceLocation getId() {
        return ModAbilities.REGISTRY.getKey(this);
    }

    public Component getDisplayName() {
        return Component.translatable(Util.makeDescriptionId("ability", getId()));
    }

    // ---------- builder ----------

    public static Properties properties(Supplier<BendingStyle> style) {
        return new Properties(style);
    }

    public static class Properties {
        private final Supplier<BendingStyle> style;
        private MoveType type = MoveType.PROJECTILE;
        private Difficulty difficulty = Difficulty.EASY;
        private Learning learning = Learning.SCROLL_OR_MASTER;
        private CastType castType = CastType.INSTANT;
        private ComboStyle comboStyle = ComboStyle.STRIKE;
        private ComboPattern combo = ComboPattern.of("LR");
        private int comboWindowTicks = 0;   // 0 = work it out automatically
        private double chiCost = 0.0;
        private double chiPerTick = 0.0;
        private int cooldownTicks = 0;
        private int maxChargeTicks = 0;
        private boolean releaseOnLetGo = false;
        private double failChiFraction = 0.25;
        private AnimationSpec animation = AnimationSpec.NONE;
        private int requiredElementLevel = 1;

        private Properties(Supplier<BendingStyle> style) { this.style = style; }

        public Properties type(MoveType v) { this.type = v; return this; }
        public Properties difficulty(Difficulty v) { this.difficulty = v; return this; }
        public Properties learning(Learning v) { this.learning = v; return this; }
        public Properties castType(CastType v) { this.castType = v; return this; }
        public Properties comboStyle(ComboStyle v) { this.comboStyle = v; return this; }
        /** Click combo as text, for example "LRL". Longer combos for harder moves. */
        public Properties combo(String pattern) { this.combo = ComboPattern.of(pattern); return this; }
        /** Optional override of the combo time limit, in ticks. */
        public Properties comboWindow(int ticks) { this.comboWindowTicks = ticks; return this; }
        public Properties chiCost(double v) { this.chiCost = v; return this; }
        public Properties chiPerTick(double v) { this.chiPerTick = v; return this; }
        public Properties cooldown(int ticks) { this.cooldownTicks = ticks; return this; }
        /** Adds a charge phase after the combo, up to this many ticks. A further click fires it. */
        public Properties charge(int maxTicks) { this.maxChargeTicks = maxTicks; return this; }
        /** Charge phase that lasts while the last combo click is held. Letting go fires the move. */
        public Properties holdCharge(int maxTicks) {
            this.maxChargeTicks = maxTicks;
            this.releaseOnLetGo = true;
            return this;
        }
        public Properties failChiFraction(double v) { this.failChiFraction = v; return this; }
        public Properties animation(AnimationSpec v) { this.animation = v; return this; }
        /** Element level needed to learn the move from a scroll (1 to 45). Defaults to 1. */
        public Properties requiredElementLevel(int v) { this.requiredElementLevel = Math.max(1, v); return this; }
    }
}