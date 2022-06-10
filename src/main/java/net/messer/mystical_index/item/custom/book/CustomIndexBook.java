package net.messer.mystical_index.item.custom.book;

import net.messer.mystical_index.block.ModBlocks;
import net.messer.mystical_index.block.custom.IndexLecternBlock;
import net.messer.mystical_index.util.WorldEffects;
import net.messer.mystical_index.util.request.IndexInteractable;
import net.messer.mystical_index.util.request.InsertionRequest;
import net.messer.mystical_index.util.request.LibraryIndex;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.LecternBlock;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.StackReference;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtInt;
import net.minecraft.nbt.NbtList;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.screen.slot.Slot;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.ActionResult;
import net.minecraft.util.ClickType;
import net.minecraft.util.Formatting;
import net.minecraft.util.Rarity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;

public class CustomIndexBook extends CustomInventoryBook {
    public static final double LECTERN_PICKUP_RADIUS = 2d;
    public static final UUID EXTRACTED_DROP_UUID = UUID.randomUUID();

    public CustomIndexBook(Settings settings) {
        super(settings);
    }

    private static NbtList blockPosToList(BlockPos pos) {
        var list = new NbtList();
        list.add(0, NbtInt.of(pos.getX()));
        list.add(1, NbtInt.of(pos.getY()));
        list.add(2, NbtInt.of(pos.getZ()));
        return list;
    }

    private static BlockPos blockPosFromList(NbtList list) {
        return new BlockPos(list.getInt(0), list.getInt(1), list.getInt(2));
    }

    public int getMaxRange(ItemStack book, boolean autoIndexing) {
//        return book.getOrCreateNbt().getInt(MAX_RANGE_TAG);
        return book.getOrCreateSubNbt(autoIndexing ? AUTO_INDEXING_TAG : MANUAL_INDEXING_TAG).getInt(MAX_RANGE_TAG);
    }

    public int getMaxLinks(ItemStack book, boolean autoIndexing) {
//        return book.getOrCreateNbt().getInt(MAX_LINKS_TAG);
        return book.getOrCreateSubNbt(autoIndexing ? AUTO_INDEXING_TAG : MANUAL_INDEXING_TAG).getInt(MAX_LINKS_TAG);
    }

    public int getLinks(ItemStack book) {
        return book.getOrCreateNbt().getList(LINKED_BLOCKS_TAG, NbtElement.LIST_TYPE).size();
    }

//    public boolean getAutoIndexing(ItemStack book) {
//        return book.getOrCreateNbt().getString(INDEXING_TYPE_TAG).equals(INDEXING_TYPE_AUTO_TAG);
//    }

    @SuppressWarnings("ConstantConditions")
    public LibraryIndex getIndex(ItemStack book, World world, BlockPos pos) {
        var index = new LibraryIndex();
        var nbtList = book.getOrCreateNbt().getList(LINKED_BLOCKS_TAG, NbtElement.LIST_TYPE);
        for (int i = 0; i < nbtList.size(); i++) {
            var posList = nbtList.getList(i);
            var interactablePos = blockPosFromList(posList);

            if (pos.isWithinDistance(interactablePos, getMaxRange(book, false)) &&
                    world.getBlockEntity(interactablePos) instanceof IndexInteractable interactable) {
                index.interactables.add(interactable);
            }
        }
        return index;
    }

    @Override
    public boolean onStackClicked(ItemStack book, Slot slot, ClickType clickType, PlayerEntity player) {
        if (clickType != ClickType.RIGHT || !slot.hasStack()) {
            return false;
        }
        tryInsertItemStack(slot.getStack(), player);
        return true;
    }

    @Override
    public boolean onClicked(ItemStack book, ItemStack cursorStack, Slot slot, ClickType clickType, PlayerEntity player, StackReference cursorStackReference) {
        if (clickType != ClickType.RIGHT || cursorStack.isEmpty() || !slot.canTakePartial(player)) {
            return false;
        }
        tryInsertItemStack(cursorStack, player);
        return true;
    }

    public void tryInsertItemStack(ItemStack itemStack, PlayerEntity player) {
        LibraryIndex index = LibraryIndex.fromRange(player.getWorld(), player.getBlockPos(), LibraryIndex.ITEM_SEARCH_RANGE);

        InsertionRequest request = new InsertionRequest(itemStack);
        request.setSourcePosition(player.getPos());
        request.setBlockAffectedCallback(WorldEffects::insertionParticles);

        index.insertStack(request);
    }

    @Override
    public ActionResult useOnBlock(ItemUsageContext context) {
        BlockPos blockPos;
        World world = context.getWorld();
        BlockState blockState = world.getBlockState(blockPos = context.getBlockPos());
        var book = context.getStack();

        // Try to put book on lectern
        if (blockState.isOf(Blocks.LECTERN) && !blockState.get(LecternBlock.HAS_BOOK)) {
            var newState = ModBlocks.INDEX_LECTERN.getStateWithProperties(blockState);

            world.setBlockState(blockPos, newState);
            ItemStack stack = context.getStack().copy();
            context.getStack().decrement(1);

            return IndexLecternBlock.putBookIfAbsent( // TODO add booming sound
                    context.getPlayer(), world,
                    blockPos, newState,
                    stack
            ) ? ActionResult.success(world.isClient) : ActionResult.PASS;
        }

        // Try linking library to book
        else if (blockState.isOf(ModBlocks.LIBRARY) && context.getPlayer() != null && context.getPlayer().isSneaking()) {

            var nbt = book.getOrCreateNbt();
            var librariesList = nbt.getList(LINKED_BLOCKS_TAG, NbtElement.LIST_TYPE);
            var serializedPos = blockPosToList(blockPos);
            var pos = Vec3d.ofCenter(blockPos);

            if (librariesList.contains(serializedPos)) {
                librariesList.remove(serializedPos);
                world.playSound(null, pos.getX(), pos.getY(), pos.getZ(),
                        SoundEvents.ENTITY_ENDER_EYE_DEATH, SoundCategory.BLOCKS,
                        0.5f, 0.2f + world.getRandom().nextFloat() * 0.4f);
                WorldEffects.blockParticles(world, blockPos, ParticleTypes.ENCHANTED_HIT);
            } else {
                if (librariesList.size() >= getMaxLinks(book, true)) {
                    world.playSound(null, pos.getX(), pos.getY(), pos.getZ(),
                            SoundEvents.ENTITY_ENDER_EYE_DEATH, SoundCategory.BLOCKS,
                            1f, 1.8f + world.getRandom().nextFloat() * 0.2f);
                    WorldEffects.blockParticles(world, blockPos, ParticleTypes.CRIT);

                    return ActionResult.success(world.isClient);
                }

                librariesList.add(serializedPos);
                world.playSound(null, pos.getX(), pos.getY(), pos.getZ(),
                        SoundEvents.BLOCK_AMETHYST_BLOCK_STEP, SoundCategory.BLOCKS,
                        1f, 0.4f + world.getRandom().nextFloat() * 0.4f);
                WorldEffects.blockParticles(world, blockPos, ParticleTypes.SOUL_FIRE_FLAME);
            }

            nbt.put(LINKED_BLOCKS_TAG, librariesList);

            return ActionResult.success(world.isClient);
        }
        return ActionResult.PASS;
    }

    @Override
    public void inventoryTick(ItemStack book, World world, Entity entity, int slot, boolean selected) {
        super.inventoryTick(book, world, entity, slot, selected);
        var server = world.getServer();

        if (!selected || server == null || server.getTicks() % 10 != 0) {
            return;
        }

        var positions = book.getOrCreateNbt().getList(LINKED_BLOCKS_TAG, NbtElement.LIST_TYPE);

        for (var pos : positions) {
            var blockPos = blockPosFromList((NbtList) pos);
            WorldEffects.blockParticles(world, blockPos, ParticleTypes.SOUL_FIRE_FLAME);
        }
    }

    @Override
    public Rarity getRarity(ItemStack stack) {
        return Rarity.RARE;
    }

    @Override
    public boolean hasGlint(ItemStack book) {
        return getLinks(book) > 0;
    }

    @Override
    public void appendTooltip(ItemStack book, @Nullable World world, List<Text> tooltip, TooltipContext context) {
        tooltip.add(new LiteralText(""));
        tooltip.add(new TranslatableText("item.mystical_index.custom_book.tooltip.capacity")
                .formatted(Formatting.GRAY));
        forEachPageType(book, pageItem -> pageItem.bookAppendTooltip(book, tooltip));
    }
}
