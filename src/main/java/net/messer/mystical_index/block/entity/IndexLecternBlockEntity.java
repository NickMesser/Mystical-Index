package net.messer.mystical_index.block.entity;

import eu.pb4.polymer.api.utils.PolymerObject;
import net.messer.mystical_index.MysticalIndex;
import net.messer.mystical_index.block.ModBlockEntities;
import net.messer.mystical_index.item.ModItems;
import net.messer.mystical_index.item.custom.book.CustomIndexBook;
import net.messer.mystical_index.util.LecternTracker;
import net.messer.mystical_index.util.ParticleSystem;
import net.messer.mystical_index.util.request.IIndexInteractable;
import net.messer.mystical_index.util.request.LibraryIndex;
import net.minecraft.block.BlockState;
import net.minecraft.block.LecternBlock;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.block.entity.LecternBlockEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.*;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.Objects;

public class IndexLecternBlockEntity extends LecternBlockEntity implements PolymerObject {
    private static final String LINKED_LIBRARIES_TAG = "linked_libraries";

    private static final int CIRCLE_PERIOD = 200;
    private static final int CIRCLE_INTERVAL = 2;
    private static final int FLAME_INTERVAL = 4;
    private static final int SOUND_INTERVAL = 24;

    private LibraryIndex linkedLibraries = new LibraryIndex();

    public IndexLecternBlockEntity(BlockPos pos, BlockState state) {
        super(pos, state);
    }

    public int getMaxRange(boolean lectern) {
        return ((CustomIndexBook) ModItems.CUSTOM_INDEX).getMaxRange(getBook(), lectern);
    }

    public int getMaxLinks(boolean lectern) {
        return ((CustomIndexBook) ModItems.CUSTOM_INDEX).getMaxLinks(getBook(), lectern);
    }

    public void setLinkedLibraries(LibraryIndex linkedLibraries) {
        this.linkedLibraries = linkedLibraries;
    }

    public LibraryIndex getLinkedLibraries() {
        return linkedLibraries;
    }

    public static void serverTick(World world, BlockPos pos, BlockState state, IndexLecternBlockEntity be) {
        if (world instanceof ServerWorld serverWorld) {
            if (state.get(LecternBlock.HAS_BOOK)) {
                Vec3d centerPos = Vec3d.ofCenter(pos, 0.5);
                if (
                        serverWorld.getServer().getTicks() % CIRCLE_INTERVAL == 0 &&
                        serverWorld.getClosestPlayer(
                                pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                                CustomIndexBook.LECTERN_PICKUP_RADIUS, false
                        ) != null
                ) {
                    ParticleSystem.drawParticleCircle(
                            serverWorld, centerPos, CIRCLE_PERIOD,
                            0, CustomIndexBook.LECTERN_PICKUP_RADIUS
                    );
                    ParticleSystem.drawParticleCircle(
                            serverWorld, centerPos, -CIRCLE_PERIOD,
                            0, CustomIndexBook.LECTERN_PICKUP_RADIUS
                    );
                    ParticleSystem.drawParticleCircle(
                            serverWorld, centerPos, CIRCLE_PERIOD,
                            CIRCLE_PERIOD / 2, CustomIndexBook.LECTERN_PICKUP_RADIUS
                    );
                    ParticleSystem.drawParticleCircle(
                            serverWorld, centerPos, -CIRCLE_PERIOD,
                            CIRCLE_PERIOD / 2, CustomIndexBook.LECTERN_PICKUP_RADIUS
                    );

                    if (serverWorld.getServer().getTicks() % FLAME_INTERVAL == 0) {
                        serverWorld.spawnParticles(
                                ParticleTypes.SOUL_FIRE_FLAME, centerPos.getX(), centerPos.getY() + 0.3, centerPos.getZ(),
                                1, 0.3, 0.3, 0.3, 0);
                    }

                    if (serverWorld.getServer().getTicks() % SOUND_INTERVAL == 0) {
                        serverWorld.playSound(null, centerPos.getX(), centerPos.getY(), centerPos.getZ(),
                                SoundEvents.BLOCK_BEACON_AMBIENT, SoundCategory.BLOCKS,
                                0.3f, 1.4f + world.getRandom().nextFloat() * 0.4f);
                    }
                }

                LecternTracker.addIndexLectern(be);
            } else {
                LecternTracker.removeIndexLectern(be);
            }
        }
    }

    @Override
    public BlockEntityType<?> getType() {
        return ModBlockEntities.INDEX_LECTERN_BLOCK_ENTITY;
    }

    @Override
    protected void writeNbt(NbtCompound nbt) {
        super.writeNbt(nbt);

        var nbtList = new NbtList();
        linkedLibraries.interactables.stream()
                .map(i -> i instanceof BlockEntity ibe ? ibe.getPos() : null)
                .filter(Objects::nonNull)
                .forEach(blockPos -> {
                    var posList = new NbtList();
                    posList.add(NbtInt.of(blockPos.getX()));
                    posList.add(NbtInt.of(blockPos.getY()));
                    posList.add(NbtInt.of(blockPos.getZ()));
                    nbtList.add(posList);
                });
        nbt.put(LINKED_LIBRARIES_TAG, nbtList);
    }

    @Override
    public void readNbt(NbtCompound nbt) {
        super.readNbt(nbt);

        var nbtList = nbt.getList(LINKED_LIBRARIES_TAG, NbtElement.LIST_TYPE);
        for (int i = 0; i < nbtList.size(); i++) {
            var posList = nbtList.getList(i);
            if (world.getBlockEntity(
                    new BlockPos(posList.getInt(0), posList.getInt(1), posList.getInt(2))
                    ) instanceof IIndexInteractable interactable)
                linkedLibraries.interactables.add(interactable);
        }
    }
}
