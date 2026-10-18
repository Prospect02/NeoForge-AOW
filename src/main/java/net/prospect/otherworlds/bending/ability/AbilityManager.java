package net.prospect.otherworlds.bending.ability;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.prospect.otherworlds.bending.chi.ChiHandler;
import net.prospect.otherworlds.registry.ModAttachments;

/** Every cast, from a command, a click combo, or a mob, goes through here. Server side only. */
public class AbilityManager {

    public static AbilityResult tryUse(LivingEntity user, Ability ability, double quality) {
        if (!(user.level() instanceof ServerLevel level)) return AbilityResult.FAILED;

        ResourceLocation id = ability.getId();
        long now = level.getGameTime();
        AbilityCooldowns cooldowns = user.getData(ModAttachments.COOLDOWNS);

        // Later phases add "has this player learned the move?" checks here.
        if (!cooldowns.isReady(id, now)) return AbilityResult.ON_COOLDOWN;

        // Players pay chi. Mobs are free for now.
        ServerPlayer player = user instanceof ServerPlayer p ? p : null;
        double cost = ability.getChiCost();
        if (player != null && ChiHandler.getChi(player) < cost) return AbilityResult.NOT_ENOUGH_CHI;

        if (!ability.activate(new AbilityContext(user, level, quality))) return AbilityResult.FAILED;

        if (player != null) ChiHandler.trySpend(player, cost);
        cooldowns.start(id, now, ability.getCooldownTicks());
        return AbilityResult.SUCCESS;
    }
}