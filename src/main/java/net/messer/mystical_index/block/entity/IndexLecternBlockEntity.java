package net.messer.mystical_index.block.entity;

import eu.pb4.polymer.api.utils.PolymerObject;
import net.messer.mystical_index.block.ModBlockEntities;
import net.messer.mystical_index.events.MixinHooks;
import net.messer.mystical_index.item.custom.book.CustomIndexBook;
import net.messer.mystical_index.util.LecternTracker;
import net.messer.mystical_index.util.ParticleSystem;
import net.minecraft.block.BlockState;
import net.minecraft.block.LecternBlock;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.block.entity.LecternBlockEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public class IndexLecternBlockEntity extends LecternBlockEntity implements PolymerObject {
    private static final int LECTERN_CIRCLE_PERIOD = 200;
    private static final int LECTERN_CIRCLE_INTERVAL = 2;

    public IndexLecternBlockEntity(BlockPos pos, BlockState state) {
        super(pos, state);
    }

    public static void serverTick(World world, BlockPos pos, BlockState state, LecternBlockEntity be) {
        if (world instanceof ServerWorld serverWorld) {
            if (state.get(LecternBlock.HAS_BOOK)) {
                if (
                        serverWorld.getServer().getTicks() % LECTERN_CIRCLE_INTERVAL == 0 &&
                        serverWorld.getClosestPlayer(
                                pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                                CustomIndexBook.LECTERN_PICKUP_RADIUS, false
                        ) != null
                ) { // TODO add extra particles to clearly indicate activation and constant sound, humming?
                    Vec3d centerPos = Vec3d.ofCenter(pos, 0.5);
                    ParticleSystem.drawParticleCircle(
                            serverWorld, centerPos, LECTERN_CIRCLE_PERIOD,
                            0, CustomIndexBook.LECTERN_PICKUP_RADIUS
                    );
                    ParticleSystem.drawParticleCircle(
                            serverWorld, centerPos, -LECTERN_CIRCLE_PERIOD,
                            0, CustomIndexBook.LECTERN_PICKUP_RADIUS
                    );
                    ParticleSystem.drawParticleCircle(
                            serverWorld, centerPos, LECTERN_CIRCLE_PERIOD,
                            LECTERN_CIRCLE_PERIOD / 2, CustomIndexBook.LECTERN_PICKUP_RADIUS
                    );
                    ParticleSystem.drawParticleCircle(
                            serverWorld, centerPos, -LECTERN_CIRCLE_PERIOD,
                            LECTERN_CIRCLE_PERIOD / 2, CustomIndexBook.LECTERN_PICKUP_RADIUS
                    );
                    serverWorld.spawnParticles( // TODO this bad, godo fix
                            ParticleTypes.END_ROD, centerPos.getX(), centerPos.getY(), centerPos.getZ(),
                            LECTERN_CIRCLE_INTERVAL, 1, 0.3, 1, 0);
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
}
