package net.messer.mystical_index.util.request;

import com.google.common.collect.ImmutableList;
import net.messer.mystical_index.block.ModTags;
import net.messer.mystical_index.util.ContentsIndex;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;

public class LibraryIndex implements IIndexInteractable {
    public static final int ITEM_SEARCH_RANGE = 5; // TODO: config // TODO: higher range for lectern based somehow, extenders?
    public static final int LECTERN_SEARCH_RANGE = 8;
    private final ArrayList<IIndexInteractable> blockEntities;

    private LibraryIndex(ArrayList<IIndexInteractable> blockEntities) {
        this.blockEntities = blockEntities;
    }

    public ContentsIndex getContents() {
        ContentsIndex result = new ContentsIndex();
        for (IIndexInteractable entity : blockEntities) {
            result.merge(entity.getContents());
        }
        return result;
    }

    public boolean isEmpty() {
        return blockEntities.isEmpty();
    }

    public static LibraryIndex get(World world, BlockPos pos, int searchRange) {
        ArrayList<IIndexInteractable> entities = new ArrayList<>();
        for (int x = -searchRange; x <= searchRange; x++) {
            for (int z = -searchRange; z <= searchRange; z++) {
                for (int y = -searchRange; y <= searchRange; y++) {
                    BlockPos testPos = pos.add(x, y, z);
                    if (ModTags.INDEX_INTRACTABLE.contains(world.getBlockState(testPos).getBlock()) &&
                            world.getBlockEntity(testPos) instanceof IIndexInteractable entity) {
                        entities.add(entity);
                    }
                }
            }
        }

        return new LibraryIndex(entities);
    }

    @Override
    public List<ItemStack> extractItems(ExtractionRequest request, boolean apply) {
        ImmutableList.Builder<ItemStack> builder = ImmutableList.builder();

        for (IIndexInteractable entity : blockEntities) {
            if (request.isSatisfied()) break;

            builder.addAll(entity.extractItems(request, apply));
        }

        return builder.build();
    }

    @Override
    public void insertStack(InsertionRequest request) {
        for (IIndexInteractable entity : blockEntities) {
            if (request.isSatisfied()) break;

            entity.insertStack(request);
        }
    }
}
