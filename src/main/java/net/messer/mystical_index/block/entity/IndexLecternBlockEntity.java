package net.messer.mystical_index.block.entity;

import net.messer.mystical_index.block.ModBlockEntities;
import net.messer.mystical_index.client.Particles;
import net.messer.mystical_index.item.ModItems;
import net.messer.mystical_index.util.LecternTracker;
import net.messer.mystical_index.util.request.LibraryIndex;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.block.entity.LecternBlockEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.UUID;

import static net.minecraft.block.LecternBlock.HAS_BOOK;

public class IndexLecternBlockEntity extends LecternBlockEntity { // TODO seperate IndexingBlockEntity
    private static final int CIRCLE_PERIOD = 200;
    private static final int CIRCLE_INTERVAL = 2;
    private static final int FLAME_INTERVAL = 4;
    private static final int SOUND_INTERVAL = 24;

    public static final double LECTERN_PICKUP_RADIUS = 2d;
    public static final UUID EXTRACTED_DROP_UUID = UUID.randomUUID();

    public int tick = 0;
    private LibraryIndex linkedLibraries = new LibraryIndex();

    public IndexLecternBlockEntity(BlockPos pos, BlockState state) {
        super(pos, state);
    }

    public int getMaxRange(boolean linked) {
        return ModItems.INDEXING_TYPE_PAGE.getMaxRange(getBook(), linked);
    }

    public int getMaxLinks() {
        return ModItems.INDEXING_TYPE_PAGE.getMaxLinks(getBook());
    }

    public boolean hasRangedLinking() {
        return ModItems.INDEXING_TYPE_PAGE.getLinks(getBook()) != 0;
    }

    public void setLinkedLibraries(LibraryIndex linkedLibraries) {
        this.linkedLibraries = linkedLibraries;
    }

    public LibraryIndex getLinkedLibraries() {
        return linkedLibraries;
    }

    public void loadLinkedLibraries() {
        if (hasRangedLinking()) {
            // Set linked libraries from range if no specific links are set.
            setLinkedLibraries(LibraryIndex.fromRange(world, pos, getMaxRange(false), true));
        } else {
            // Set linked libraries to specific links taken from the book.
            setLinkedLibraries(ModItems.INDEXING_TYPE_PAGE.getIndex(getBook(), getWorld(), getPos()));
        }
    }

    public static void serverTick(World world, BlockPos pos, BlockState state, IndexLecternBlockEntity be) {
        if (!world.isClient()) {
            if (state.get(HAS_BOOK)) {
                if (be.tick == 0) {
                    be.loadLinkedLibraries();
                }

                LecternTracker.addIndexLectern(be);
            } else {
                LecternTracker.removeIndexLectern(be);
            }
        } else {
            if (state.get(HAS_BOOK)) {
                var centerPos = Vec3d.ofCenter(pos, 0.5);
                if (
                        be.tick % CIRCLE_INTERVAL == 0 &&
                        world.getClosestPlayer(
                                pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                                LECTERN_PICKUP_RADIUS, false
                        ) != null
                ) {
                    Particles.drawParticleCircle(
                            be.tick,
                            world, centerPos, CIRCLE_PERIOD,
                            0, LECTERN_PICKUP_RADIUS
                    );
                    Particles.drawParticleCircle(
                            be.tick,
                            world, centerPos, -CIRCLE_PERIOD,
                            0, LECTERN_PICKUP_RADIUS
                    );
                    Particles.drawParticleCircle(
                            be.tick,
                            world, centerPos, CIRCLE_PERIOD,
                            CIRCLE_PERIOD / 2, LECTERN_PICKUP_RADIUS
                    );
                    Particles.drawParticleCircle(
                            be.tick,
                            world, centerPos, -CIRCLE_PERIOD,
                            CIRCLE_PERIOD / 2, LECTERN_PICKUP_RADIUS
                    );

                    if (be.tick % SOUND_INTERVAL == 0) {
                        world.playSound(centerPos.getX(), centerPos.getY(), centerPos.getZ(),
                                SoundEvents.BLOCK_BEACON_AMBIENT, SoundCategory.BLOCKS,
                                0.3f, 1.4f + world.getRandom().nextFloat() * 0.4f, true);
                    }
                }
            }
        }

        be.tick++;
    }

    @Override
    public BlockEntityType<?> getType() {
        return ModBlockEntities.INDEX_LECTERN_BLOCK_ENTITY;
    }
}
