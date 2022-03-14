package net.messer.mystical_index.util.request;

import com.google.common.collect.ImmutableList;
import net.messer.mystical_index.block.ModTags;
import net.messer.mystical_index.util.ContentsIndex;
import net.messer.mystical_index.util.IndexCache;
import net.messer.mystical_index.util.ParticleSystem;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.*;
import java.util.function.Consumer;

public class LibraryIndex implements IIndexInteractable {
    public static final int ITEM_SEARCH_RANGE = 5; // TODO: config // TODO: higher range for lectern based somehow, extenders?
    public static final int LECTERN_SEARCH_RANGE = 8;
    public final Set<IIndexInteractable> interactables;

    public LibraryIndex() {
        this.interactables = new HashSet<>();
    }

    public LibraryIndex(Set<IIndexInteractable> interactables) {
        this.interactables = interactables;
    }

    public ContentsIndex getContents() {
        ContentsIndex result = new ContentsIndex();
        for (IIndexInteractable entity : interactables) {
            result.merge(entity.getContents());
        }
        return result;
    }

    public boolean isEmpty() {
        return interactables.isEmpty();
    }

    public static LibraryIndex fromRange(World world, BlockPos pos, int searchRange) {
//        // Check if we have an index cached for this location
//        Optional<LibraryIndex> cachedIndex = IndexCache.get(pos, world, searchRange);
//        if (cachedIndex.isPresent())
//            return cachedIndex.get();

        // If not, iterate over nearby blocks and generate the index
        var result = new LibraryIndex();
        for (int x = -searchRange; x <= searchRange; x++) {
            for (int z = -searchRange; z <= searchRange; z++) {
                for (int y = -searchRange; y <= searchRange; y++) {
                    BlockPos testPos = pos.add(x, y, z);
                    if (ModTags.INDEX_INTRACTABLE.contains(world.getBlockState(testPos).getBlock()) &&
                            world.getBlockEntity(testPos) instanceof IIndexInteractable entity) {
                        result.add(entity, ParticleSystem::registrationParticles);
                    }
                }
            }
        }

        // Cache the generated index and return it
//        IndexCache.put(pos, world, searchRange, result);
        return result;
    }

    public void add(IIndexInteractable interactable, Consumer<IIndexInteractable> callback) {
        interactables.add(interactable);
        callback.accept(interactable);
    }

    @Override
    public List<ItemStack> extractItems(ExtractionRequest request, boolean apply) {
        ImmutableList.Builder<ItemStack> builder = ImmutableList.builder();

        for (IIndexInteractable entity : interactables) {
            if (request.isSatisfied()) break;

            builder.addAll(entity.extractItems(request, apply));
        }

        return builder.build();
    }

    @Override
    public void insertStack(InsertionRequest request) {
        for (IIndexInteractable entity : interactables) {
            if (request.isSatisfied()) break;

            entity.insertStack(request);
        }
    }
}
