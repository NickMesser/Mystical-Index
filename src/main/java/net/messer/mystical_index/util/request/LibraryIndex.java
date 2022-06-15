package net.messer.mystical_index.util.request;

import com.google.common.collect.ImmutableList;
import net.messer.mystical_index.block.ModTags;
import net.messer.mystical_index.util.WorldEffects;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

import static net.messer.mystical_index.block.ModTags.INDEX_INTRACTABLE;

public class LibraryIndex implements IndexInteractable {
    public static final int ITEM_SEARCH_RANGE = 5; // TODO: config // TODO: higher range for lectern based somehow, extenders?
    public static final int LECTERN_SEARCH_RANGE = 8;
    public final Set<IndexInteractable> interactables;

    public LibraryIndex() {
        this.interactables = new HashSet<>();
    }

    public LibraryIndex(Set<IndexInteractable> interactables) {
        this.interactables = interactables;
    }

//    public ContentsIndex getContents() {
//        ContentsIndex result = new ContentsIndex();
//        for (IndexInteractable entity : interactables) {
//            result.merge(entity.getContents());
//        }
//        return result;
//    }

    public boolean isEmpty() {
        return interactables.isEmpty();
    }

    public static LibraryIndex fromRange(World world, BlockPos pos, int searchRange) {
        return fromRange(world, pos, searchRange, true);
    }

    public static LibraryIndex fromRange(World world, BlockPos pos, int searchRange, boolean particles) {
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

//                    if (ModTags.INDEX_INTRACTABLE.contains(world.getBlockState(testPos).getBlock()) &&
//                            world.getBlockEntity(testPos) instanceof IndexInteractable entity) {
//                        result.add(entity, particles ? WorldEffects::registrationParticles : i -> {});

                    if(world.getBlockState(testPos).isIn(INDEX_INTRACTABLE) &&
                            world.getBlockEntity(testPos) instanceof IndexInteractable entity) {
                        result.add(entity, particles ? WorldEffects::registrationParticles : i -> {});
                    }
                }
            }
        }

        // Cache the generated index and return it
//        IndexCache.put(pos, world, searchRange, result);
        return result;
    }

    public void add(IndexInteractable interactable, Consumer<IndexInteractable> callback) {
        interactables.add(interactable);
        callback.accept(interactable);
    }

    @Override
    public List<IndexSource> getSources() {
        ImmutableList.Builder<IndexSource> builder = ImmutableList.builder();

        for (IndexInteractable entity : interactables) {
            builder.addAll(entity.getSources());
        }

        return builder.build();
    }

    //    @Override
//    public List<ItemStack> extractItems(ExtractionRequest request, boolean apply) {
//        ImmutableList.Builder<ItemStack> builder = ImmutableList.builder();
//
//        for (IndexInteractable entity : interactables) {
//            if (request.isSatisfied()) break;
//
//            builder.addAll(entity.extractItems(request, apply));
//        }
//
//        return builder.build();
//    }
//
//    @Override
//    public void insertStack(InsertionRequest request) {
//        for (IndexInteractable entity : interactables) {
//            if (request.isSatisfied()) break;
//
//            entity.insertStack(request);
//        }
//    }
}
