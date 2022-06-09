package net.messer.mystical_index.block.entity;

import eu.pb4.polymer.api.utils.PolymerObject;
import net.messer.mystical_index.block.ModBlockEntities;
import net.messer.mystical_index.item.ModItems;
import net.messer.mystical_index.item.custom.book.CustomIndexBook;
import net.messer.mystical_index.util.LecternTracker;
import net.messer.mystical_index.util.WorldEffects;
import net.messer.mystical_index.util.request.LibraryIndex;
import net.minecraft.block.BlockState;
import net.minecraft.block.LecternBlock;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.block.entity.LecternBlockEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public class IndexLecternBlockEntity extends LecternBlockEntity implements PolymerObject {
    private static final int CIRCLE_PERIOD = 200;
    private static final int CIRCLE_INTERVAL = 2;
    private static final int FLAME_INTERVAL = 4;
    private static final int SOUND_INTERVAL = 24;

    private boolean firstTick = true;
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

    public boolean isLinked() {
        return ((CustomIndexBook) ModItems.CUSTOM_INDEX).getLinks(getBook()) != 0;
    }

    public void setLinkedLibraries(LibraryIndex linkedLibraries) {
        this.linkedLibraries = linkedLibraries;
    }

    public LibraryIndex getLinkedLibraries() {
        if (isLinked()) {
            return ((CustomIndexBook) ModItems.CUSTOM_INDEX).getIndex(getBook(), getWorld(), getPos());
        }
        return linkedLibraries;
    }

    public void loadLinkedLibraries() {
        if (!isLinked()) {
            setLinkedLibraries(LibraryIndex.fromRange(world, pos, getMaxRange(true), true));
        }
    }

    public static void serverTick(World world, BlockPos pos, BlockState state, IndexLecternBlockEntity be) {
        if (world instanceof ServerWorld serverWorld) {
            if (state.get(LecternBlock.HAS_BOOK)) {
                if (be.firstTick) {
                    be.loadLinkedLibraries();

                    be.firstTick = false;
                }

                Vec3d centerPos = Vec3d.ofCenter(pos, 0.5);
                if (
                        serverWorld.getServer().getTicks() % CIRCLE_INTERVAL == 0 &&
                        serverWorld.getClosestPlayer(
                                pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                                CustomIndexBook.LECTERN_PICKUP_RADIUS, false
                        ) != null
                ) {
                    WorldEffects.drawParticleCircle(
                            serverWorld, centerPos, CIRCLE_PERIOD,
                            0, CustomIndexBook.LECTERN_PICKUP_RADIUS
                    );
                    WorldEffects.drawParticleCircle(
                            serverWorld, centerPos, -CIRCLE_PERIOD,
                            0, CustomIndexBook.LECTERN_PICKUP_RADIUS
                    );
                    WorldEffects.drawParticleCircle(
                            serverWorld, centerPos, CIRCLE_PERIOD,
                            CIRCLE_PERIOD / 2, CustomIndexBook.LECTERN_PICKUP_RADIUS
                    );
                    WorldEffects.drawParticleCircle(
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

                LecternTracker.addIndexLectern(be); // TODO lectern shouldnt index nearby libraries if in linked mode
            } else {
                LecternTracker.removeIndexLectern(be);
            }
        }
    }

    @Override
    public BlockEntityType<?> getType() {
        return ModBlockEntities.INDEX_LECTERN_BLOCK_ENTITY;
    }
}
