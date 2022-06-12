package net.messer.mystical_index.block.entity;

import net.messer.mystical_index.block.ModBlockEntities;
import net.messer.mystical_index.item.custom.book.MysticalBookItem;
import net.messer.mystical_index.util.LecternTracker;
import net.messer.mystical_index.util.MathUtil;
import net.messer.mystical_index.util.state.PageLecternState;
import net.minecraft.block.BlockState;
import net.minecraft.block.LecternBlock;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.block.entity.LecternBlockEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;

import static net.minecraft.block.LecternBlock.HAS_BOOK;

public class MysticalLecternBlockEntity extends LecternBlockEntity { // TODO seperate IndexingBlockEntity
    public static final double LECTERN_PICKUP_RADIUS = 2d;

    public int tick = 0;
    public float bookRotation = 0;
    public float bookRotationTarget = 0;
    public PageLecternState typeState;
    public PageLecternState actionState;

    public MysticalLecternBlockEntity(BlockPos pos, BlockState state) {
        super(pos, state);
    }

    private void initState() {
        var typePage = ((MysticalBookItem) getBook().getItem()).getTypePage(getBook());
        var actionPage = ((MysticalBookItem) getBook().getItem()).getActionPage(getBook());

        if (typePage != null) typeState = typePage.lectern$getState(this);
        if (actionPage != null) actionState = actionPage.lectern$getState(this);
    }

    public static void serverTick(World world, BlockPos pos, BlockState state, MysticalLecternBlockEntity lectern) {
        if (!world.isClient()) {
            if (state.get(HAS_BOOK)) {
                if (lectern.tick == 0) {
                    lectern.initState();
                }

                LecternTracker.addIndexLectern(lectern);
            } else {
                LecternTracker.removeIndexLectern(lectern);
            }
        } else {
            if (state.get(HAS_BOOK)) {
                var closestPlayer = world.getClosestPlayer(
                        pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                        LECTERN_PICKUP_RADIUS, false
                );

                float rotationDelta = (lectern.bookRotationTarget - lectern.bookRotation) * 0.1f;
                lectern.bookRotation = lectern.bookRotation + rotationDelta;

                if (closestPlayer == null) {
                    lectern.bookRotationTarget = 0;
                } else {
                    double xOffset = closestPlayer.getX() - ((double) pos.getX() + 0.5);
                    double zOffset = closestPlayer.getZ() - ((double) pos.getZ() + 0.5);
                    float rotationTarget = (float) Math.atan2(zOffset, xOffset);
                    float rotationOffset = (float) Math.toRadians(state.get(LecternBlock.FACING).rotateYClockwise().asRotation());
                    lectern.bookRotationTarget = MathHelper.clamp(MathUtil.fixRadians(rotationTarget - rotationOffset), -0.4f, 0.4f);
                }
            }
        }

        lectern.tick++;

        ((MysticalBookItem) lectern.getBook().getItem()).lectern$serverTick(world, pos, state, lectern);
    }

    @Override
    public BlockEntityType<?> getType() {
        return ModBlockEntities.MYSTICAL_LECTERN_BLOCK_ENTITY;
    }
}
