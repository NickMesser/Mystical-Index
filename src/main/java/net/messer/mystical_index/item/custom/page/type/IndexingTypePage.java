package net.messer.mystical_index.item.custom.page.type;

import net.messer.mystical_index.block.ModBlocks;
import net.messer.mystical_index.block.custom.IndexLecternBlock;
import net.messer.mystical_index.item.custom.page.AttributePageItem;
import net.messer.mystical_index.item.custom.page.TypePageItem;
import net.messer.mystical_index.util.Colors;
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
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtInt;
import net.minecraft.nbt.NbtList;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.screen.slot.Slot;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.ActionResult;
import net.minecraft.util.ClickType;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.List;

import static net.messer.mystical_index.item.ModItems.INDEXING_TYPE_PAGE;

public class IndexingTypePage extends TypePageItem {
    public static final String MAX_RANGE_TAG = "max_range";
    public static final String MAX_LINKS_TAG = "max_links";
    public static final String MAX_RANGE_LINKED_TAG = "max_range_linked";

    @Override
    public int getColor() {
        return 0x888800;
    }

    @Override
    public Text getTypeDisplayName() {
        return new TranslatableText("item.mystical_index.page.tooltip.type.indexing").formatted(Formatting.DARK_PURPLE);
    }

    public static final String LINKED_BLOCKS_TAG = "linked_blocks";

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

    public int getMaxRange(ItemStack book, boolean linked) {
        return book.getOrCreateNbt().getInt(linked ? MAX_RANGE_LINKED_TAG : MAX_RANGE_TAG);
    }

    public int getMaxLinks(ItemStack book) {
        return book.getOrCreateNbt().getInt(MAX_LINKS_TAG);
    }

    public int getLinks(ItemStack book) {
        return book.getOrCreateNbt().getList(LINKED_BLOCKS_TAG, NbtElement.LIST_TYPE).size();
    }

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
    public void onCraftToBook(ItemStack page, ItemStack book) {
        super.onCraftToBook(page, book);

        NbtCompound attributes = getAttributes(book);

        attributes.putInt(MAX_RANGE_TAG, 2);
        attributes.putInt(MAX_LINKS_TAG, 2);
        attributes.putInt(MAX_RANGE_LINKED_TAG, 20);
    }

    @Override
    public boolean book$onStackClicked(ItemStack book, Slot slot, ClickType clickType, PlayerEntity player) {
        if (clickType != ClickType.RIGHT || !slot.hasStack()) {
            return false;
        }
        tryInsertItemStack(slot.getStack(), player);
        return true;
    }

    @Override
    public boolean book$onClicked(ItemStack book, ItemStack cursorStack, Slot slot, ClickType clickType, PlayerEntity player, StackReference cursorStackReference) {
        if (clickType != ClickType.RIGHT || cursorStack.isEmpty() || !slot.canTakePartial(player)) {
            return false;
        }
        tryInsertItemStack(cursorStack, player);
        return true;
    }

    @Override
    public ActionResult book$useOnBlock(ItemUsageContext context) {
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
                if (librariesList.size() >= getMaxLinks(book)) {
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
    public void book$inventoryTick(ItemStack book, World world, Entity entity, int slot, boolean selected) {
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

    public void tryInsertItemStack(ItemStack itemStack, PlayerEntity player) {
        LibraryIndex index = LibraryIndex.fromRange(player.getWorld(), player.getBlockPos(), LibraryIndex.ITEM_SEARCH_RANGE);

        InsertionRequest request = new InsertionRequest(itemStack);
        request.setSourcePosition(player.getPos());
        request.setBlockAffectedCallback(WorldEffects::insertionParticles);

        index.insertStack(request);
    }

    @Override
    public void book$appendPropertiesTooltip(ItemStack book, @Nullable World world, List<Text> properties, TooltipContext context) {
        var linksUsed = getLinks(book);
        var linksMax = getMaxLinks(book);
        double linksUsedRatio = (double) linksUsed / linksMax;

        properties.add(new TranslatableText("item.mystical_index.mystical_book.tooltip.type.indexing.range",
                getMaxRange(book, true))
                .formatted(Formatting.YELLOW));
        properties.add(new TranslatableText("item.mystical_index.mystical_book.tooltip.type.indexing.links",
                linksUsed, linksMax)
                .formatted(Colors.colorByRatio(linksUsedRatio)));
        properties.add(new TranslatableText("item.mystical_index.mystical_book.tooltip.type.indexing.linked_range",
                getMaxRange(book, false))
                .formatted(Formatting.YELLOW));
    }

    @Override
    public boolean book$hasGlint(ItemStack book) {
        return getLinks(book) > 0;
    }

    public static abstract class IndexingAttributePage extends AttributePageItem {
        @Override
        public List<TypePageItem> getCompatibleTypes(ItemStack page) {
            return List.of(INDEXING_TYPE_PAGE);
        }

        public int getRangeIncrease(ItemStack page, boolean linked) {
            return 0;
        }

        public int getLinksIncrease(ItemStack page) {
            return 0;
        }

        @Override
        public void appendAttributes(ItemStack page, NbtCompound nbt) {
            increaseIntAttribute(nbt, MAX_RANGE_TAG, getRangeIncrease(page, false));
            increaseIntAttribute(nbt, MAX_LINKS_TAG, getLinksIncrease(page));
            increaseIntAttribute(nbt, MAX_RANGE_LINKED_TAG, getRangeIncrease(page, true));
        }
    }
}
