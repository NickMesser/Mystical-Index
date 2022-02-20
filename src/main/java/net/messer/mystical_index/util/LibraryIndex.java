package net.messer.mystical_index.util;

import com.google.common.collect.ImmutableList;
import net.messer.mystical_index.block.entity.LibraryBlockEntity;
import net.messer.mystical_index.item.custom.InventoryBookItem;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
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

    public ContentsIndex getItems() {
        ContentsIndex result = new ContentsIndex();
        for (BlockEntity entity : blockEntities) {
            result.merge(((LibraryBlockEntity) entity).getContents());
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
                    if (world.getBlockEntity(pos.add(x, y, z)) instanceof LibraryBlockEntity entity) {
                        entities.add(entity);
                    }
                }
            }
        }

        return new LibraryIndex(entities);
    }

    @Override
    public List<ItemStack> extractItems(Request request, boolean apply) {
        ImmutableList.Builder<ItemStack> builder = ImmutableList.builder();

        for (BlockEntity entity : blockEntities)
            if (entity instanceof IIndexInteractable interactable)
                builder.addAll(interactable.extractItems(request, apply));

        return builder.build();
    }
}
