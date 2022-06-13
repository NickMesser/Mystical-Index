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
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.network.Packet;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;

import static net.minecraft.block.LecternBlock.HAS_BOOK;

public class MysticalLecternBlockEntity extends LecternBlockEntity { // TODO seperate IndexingBlockEntity
    public static final double LECTERN_DETECTION_RADIUS = 2d;

    public ArrayList<ItemStack> items = new ArrayList<>();
    public int tick = 0;
    public float bookRotation = 0;
    public float bookRotationTarget = 0;
    public PageLecternState typeState;
    public PageLecternState actionState;

    public MysticalLecternBlockEntity(BlockPos pos, BlockState state) {
        super(pos, state);
    }

    @Override
    public void readNbt(NbtCompound nbt) {
        super.readNbt(nbt);
        items = nbt.getList("items", NbtElement.COMPOUND_TYPE).stream()
                .map(NbtCompound.class::cast)
                .map(ItemStack::fromNbt)
                .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
    }

    @Override
    protected void writeNbt(NbtCompound nbt) {
        super.writeNbt(nbt);
        var itemsNbt = items.stream()
                .map((stack) -> {
                    var compound = new NbtCompound();
                    stack.writeNbt(compound);
                    return compound;
                })
                .collect(NbtList::new, NbtList::add, NbtList::addAll);
        nbt.put("items", itemsNbt);
    }

    @Nullable
    @Override
    public Packet<ClientPlayPacketListener> toUpdatePacket() {
        return BlockEntityUpdateS2CPacket.create(this);
    }

    @Override
    public NbtCompound toInitialChunkDataNbt() {
        return createNbt();
    }

    private void initState() {
        var bookItem = ((MysticalBookItem) getBook().getItem());
        var typePage = bookItem.getTypePage(getBook());
        var actionPage = bookItem.getActionPage(getBook());

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
                        LECTERN_DETECTION_RADIUS, false
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

        if (state.get(HAS_BOOK) && lectern.getBook().getItem() instanceof MysticalBookItem book) {
            book.lectern$serverTick(world, pos, state, lectern);
        }
    }

    @Override
    public BlockEntityType<?> getType() {
        return ModBlockEntities.MYSTICAL_LECTERN_BLOCK_ENTITY;
    }
}
