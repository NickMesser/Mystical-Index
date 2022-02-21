package net.messer.mystical_index.util.request;

import com.google.common.collect.ImmutableList;
import net.messer.mystical_index.util.ContentsIndex;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;

public class LibraryIndex implements IIndexInteractable {
    private static final int searchRange = 5; // TODO: config
    private final ArrayList<BlockEntity> blockEntities;

    private LibraryIndex(ArrayList<BlockEntity> blockEntities) {
        this.blockEntities = blockEntities;
    }

    public ContentsIndex getContents() {
        ContentsIndex result = new ContentsIndex();
        for (BlockEntity entity : blockEntities) {
            result.merge(((IIndexInteractable) entity).getContents());
        }
        return result;
    }

    public boolean isEmpty() {
        return blockEntities.isEmpty();
    }

    public static LibraryIndex get(World world, BlockPos pos) {
        ArrayList<BlockEntity> entities = new ArrayList<>();
        for (int x = -searchRange; x <= searchRange; x++) {
            for (int z = -searchRange; z <= searchRange; z++) {
                for (int y = -searchRange; y <= searchRange; y++) {
                    BlockEntity entity = world.getBlockEntity(pos.add(x, y, z));
                    if (entity instanceof IIndexInteractable) {
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

        for (BlockEntity entity : blockEntities) {
            if (request.isSatisfied()) break;

            if (entity instanceof IIndexInteractable intractable)
                builder.addAll(intractable.extractItems(request, apply));
        }

        return builder.build();
    }

    @Override
    public void insertStack(InsertionRequest request) {
        for (BlockEntity entity : blockEntities) {
            if (request.isSatisfied()) break;

            if (entity instanceof IIndexInteractable intractable)
                intractable.insertStack(request);
        }
    }
}
