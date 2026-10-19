package net.prospect.otherworlds.bending.progression;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.Map;

/**
 * Everything the mod remembers about one player's progression. Immutable: use the "with" methods.
 * elements: one level track per main element (missing = level 1, 0 xp).
 * moves: one level track per LEARNED move. A move is learned if and only if it has an entry here.
 */
public record ProgressionData(
        int playerLevel,
        double playerXp,
        Map<ResourceLocation, Track> elements,
        Map<ResourceLocation, Track> moves
) {
    public record Track(int level, double xp) {
        public static final Track START = new Track(1, 0.0);

        public static final Codec<Track> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.INT.optionalFieldOf("level", 1).forGetter(Track::level),
                Codec.DOUBLE.optionalFieldOf("xp", 0.0).forGetter(Track::xp)
        ).apply(instance, Track::new));
    }

    public static final ProgressionData DEFAULT = new ProgressionData(1, 0.0, Map.of(), Map.of());

    private static final Codec<Map<ResourceLocation, Track>> TRACKS =
            Codec.unboundedMap(ResourceLocation.CODEC, Track.CODEC);

    public static final Codec<ProgressionData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.optionalFieldOf("player_level", 1).forGetter(ProgressionData::playerLevel),
            Codec.DOUBLE.optionalFieldOf("player_xp", 0.0).forGetter(ProgressionData::playerXp),
            TRACKS.optionalFieldOf("elements", Map.of()).forGetter(ProgressionData::elements),
            TRACKS.optionalFieldOf("moves", Map.of()).forGetter(ProgressionData::moves)
    ).apply(instance, ProgressionData::new));

    // ---------- reading ----------

    public Track elementTrack(ResourceLocation element) {
        return elements.getOrDefault(element, Track.START);
    }

    public boolean hasLearned(ResourceLocation move) {
        return moves.containsKey(move);
    }

    public Track moveTrack(ResourceLocation move) {
        return moves.getOrDefault(move, Track.START);
    }

    // ---------- changing (returns a copy) ----------

    public ProgressionData withPlayer(int level, double xp) {
        return new ProgressionData(Math.max(1, level), Math.max(0.0, xp), elements, moves);
    }

    public ProgressionData withElement(ResourceLocation element, Track track) {
        Map<ResourceLocation, Track> copy = new HashMap<>(elements);
        copy.put(element, track);
        return new ProgressionData(playerLevel, playerXp, Map.copyOf(copy), moves);
    }

    public ProgressionData withMove(ResourceLocation move, Track track) {
        Map<ResourceLocation, Track> copy = new HashMap<>(moves);
        copy.put(move, track);
        return new ProgressionData(playerLevel, playerXp, elements, Map.copyOf(copy));
    }

    public ProgressionData withoutMove(ResourceLocation move) {
        Map<ResourceLocation, Track> copy = new HashMap<>(moves);
        copy.remove(move);
        return new ProgressionData(playerLevel, playerXp, elements, Map.copyOf(copy));
    }
}