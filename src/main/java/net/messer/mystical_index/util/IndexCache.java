package net.messer.mystical_index.util;

import net.messer.mystical_index.util.request.LibraryIndex;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.HashMap;
import java.util.Objects;
import java.util.Optional;

public class IndexCache {
    private static final HashMap<Location, LibraryIndex> indexCache = new HashMap<>();

    public static void markDirty() {
        indexCache.clear();
    }

    public static Optional<LibraryIndex> get(BlockPos pos, World world, int range) {
        return Optional.ofNullable(indexCache.get(new Location(pos, world, range)));
    }

    public static void put(BlockPos pos, World world, int range, LibraryIndex index) {
        indexCache.put(new Location(pos, world, range), index);
    }

    private record Location(BlockPos pos, World world, int range) {
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Location location = (Location) o;
            return range == location.range && pos.equals(location.pos) && world.equals(location.world);
        }

        @Override
        public int hashCode() {
            return Objects.hash(pos, world);
        }
    }
}
