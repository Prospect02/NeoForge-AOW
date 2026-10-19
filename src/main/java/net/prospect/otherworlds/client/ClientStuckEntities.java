package net.prospect.otherworlds.client;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.prospect.otherworlds.OtherWorlds;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Which entities near this client are stuck on a wall right now, and against which side.
 * Later: the stuck animations and the wall marks read this.
 */
@EventBusSubscriber(modid = OtherWorlds.MODID, value = Dist.CLIENT)
public class ClientStuckEntities {
    /** Prints a line when the server tells this client about a stuck entity. Turn off once it works. */
    private static final boolean DEBUG = true;

    /** If the "free" message never arrives, forget the entity after this long (100 ticks = 5 seconds). */
    private static final int FAILSAFE_TICKS = 100;

    /** wall = the side of the wall it is against. pos = the block it is stuck in. */
    public record Info(Direction wall, BlockPos pos) {}

    private static final class Entry {
        final Info info;
        int ticksLeft = FAILSAFE_TICKS;

        Entry(Info info) {
            this.info = info;
        }
    }

    private static final Map<Integer, Entry> STUCK = new HashMap<>();

    public static void set(int entityId, boolean stuck, Direction wall, BlockPos pos) {
        if (stuck) STUCK.put(entityId, new Entry(new Info(wall, pos)));
        else STUCK.remove(entityId);
        if (DEBUG) OtherWorlds.LOGGER.info("[Stuck] client: entity {} stuck={} wall={} pos={}", entityId, stuck, wall, pos);
    }

    public static boolean isStuck(Entity entity) {
        return STUCK.containsKey(entity.getId());
    }

    /** The wall side and block of a stuck entity, or null if it is not stuck. */
    public static @Nullable Info get(Entity entity) {
        Entry entry = STUCK.get(entity.getId());
        return entry == null ? null : entry.info;
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        if (STUCK.isEmpty()) return;
        Minecraft mc = Minecraft.getInstance();
        Iterator<Map.Entry<Integer, Entry>> it = STUCK.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Integer, Entry> e = it.next();
            boolean gone = mc.level == null || mc.level.getEntity(e.getKey()) == null;
            if (gone || --e.getValue().ticksLeft <= 0) it.remove();
        }
    }

    @SubscribeEvent
    public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        STUCK.clear();
    }
}