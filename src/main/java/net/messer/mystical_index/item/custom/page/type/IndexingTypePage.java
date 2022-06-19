package net.messer.mystical_index.item.custom.page.type;

import net.messer.mystical_index.block.ModBlocks;
import net.messer.mystical_index.block.entity.MysticalLecternBlockEntity;
import net.messer.mystical_index.item.custom.page.AttributePageItem;
import net.messer.mystical_index.item.custom.page.TypePageItem;
import net.messer.mystical_index.util.Colors;
import net.messer.mystical_index.util.WorldEffects;
import net.messer.mystical_index.util.request.ExtractionRequest;
import net.messer.mystical_index.util.request.IndexInteractable;
import net.messer.mystical_index.util.request.InsertionRequest;
import net.messer.mystical_index.util.request.LibraryIndex;
import net.messer.mystical_index.util.state.PageLecternState;
import net.minecraft.block.BlockState;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
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
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.ActionResult;
import net.minecraft.util.ClickType;
import net.minecraft.util.Formatting;
import net.minecraft.util.function.BooleanBiFunction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

import static net.messer.mystical_index.block.custom.MysticalLecternBlock.LECTERN_INPUT_AREA_SHAPE;
import static net.messer.mystical_index.block.entity.MysticalLecternBlockEntity.LECTERN_DETECTION_RADIUS;
import static net.messer.mystical_index.item.ModItems.INDEXING_TYPE_PAGE;

public class IndexingTypePage extends TypePageItem {
    public static final String MAX_RANGE_TAG = "max_range";
    public static final String MAX_LINKS_TAG = "max_links";
    public static final String MAX_RANGE_LINKED_TAG = "max_range_linked";

    public IndexingTypePage(String id) {
        super(id);
    }

    @Override
    public int getColor() {
        return 0xaa22aa;
    }

    @Override
    public MutableText getTypeDisplayName() {
        return super.getTypeDisplayName().formatted(Formatting.DARK_PURPLE);
    }

    public static final String LINKED_BLOCKS_TAG = "linked_blocks";

    private static final int CIRCLE_PERIOD = 200;
    private static final int CIRCLE_INTERVAL = 2;
    private static final int SOUND_INTERVAL = 24;

    public static final UUID EXTRACTED_DROP_UUID = UUID.randomUUID();

    @Override
    public void onCraftToBook(ItemStack page, ItemStack book) {
        super.onCraftToBook(page, book);

        NbtCompound attributes = getAttributes(book);

        attributes.putInt(MAX_RANGE_TAG, 1);
        attributes.putInt(MAX_LINKS_TAG, 2);
        attributes.putInt(MAX_RANGE_LINKED_TAG, 20);
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

    public int getMaxRange(ItemStack book, boolean linked) {
        return getAttributes(book).getInt(linked ? MAX_RANGE_LINKED_TAG : MAX_RANGE_TAG);
    }

    public int getMaxLinks(ItemStack book) {
        return getAttributes(book).getInt(MAX_LINKS_TAG);
    }

    public int getLinks(ItemStack book) {
        return book.getOrCreateNbt().getList(LINKED_BLOCKS_TAG, NbtElement.LIST_TYPE).size();
    }

    public boolean hasRangedLinking(ItemStack book) {
        return getLinks(book) == 0;
    }

    @SuppressWarnings("ConstantConditions")
    private LibraryIndex getLinkedIndex(ItemStack book, World world, BlockPos pos) {
        var index = new LibraryIndex();
        var nbtList = book.getOrCreateNbt().getList(LINKED_BLOCKS_TAG, NbtElement.LIST_TYPE);
        for (int i = 0; i < nbtList.size(); i++) {
            var posList = nbtList.getList(i);
            var interactablePos = blockPosFromList(posList);

            if (pos.isWithinDistance(interactablePos, getMaxRange(book, true)) &&
                    world.getBlockEntity(interactablePos) instanceof IndexInteractable interactable) {
                index.interactables.add(interactable);
            }
        }
        return index;
    }

    public LibraryIndex getIndex(ItemStack book, World world, BlockPos pos) {
        if (hasRangedLinking(book)) {
            // Get linked libraries from range if no specific links are set.
            return LibraryIndex.fromRange(world, pos, getMaxRange(book, false), true);
        } else {
            // Get linked libraries from specific links in the book.
            return getLinkedIndex(book, world, pos);
        }
    }

    public LibraryIndex getLecternIndex(MysticalLecternBlockEntity lectern) {
        return ((IndexingLecternState) lectern.typeState).getIndex();
    }

    public InsertionRequest tryInsertItemStack(LibraryIndex index, ItemStack itemStack, Vec3d pos) {
        InsertionRequest request = new InsertionRequest(itemStack);
        request.setSourcePosition(pos);
        request.setBlockAffectedCallback(WorldEffects::insertionParticles);

        request.apply(index);
        return request;
    }

    public ExtractionRequest tryExtractItemStacks(LibraryIndex index, String message, Vec3d pos) {
        ExtractionRequest request = ExtractionRequest.get(message);
        request.setSourcePosition(pos);
        request.setBlockAffectedCallback(WorldEffects::extractionParticles);

        request.apply(index);
        return request;
    }

    @Override
    public boolean book$onStackClicked(ItemStack book, Slot slot, ClickType clickType, PlayerEntity player) {
        if (clickType != ClickType.RIGHT || !slot.hasStack()) {
            return false;
        }

        var index = getIndex(book, player.world, player.getBlockPos());
        var request = tryInsertItemStack(index, slot.getStack(), player.getPos());
        if (request.hasAffected()) WorldEffects.lecternPlonk(player.world, player.getPos(), 0.6f, false);
        return true;
    }

    @Override
    public boolean book$onClicked(ItemStack book, ItemStack cursorStack, Slot slot, ClickType clickType, PlayerEntity player, StackReference cursorStackReference) {
        if (clickType != ClickType.RIGHT || cursorStack.isEmpty() || !slot.canTakePartial(player)) {
            return false;
        }

        var index = getIndex(book, player.world, player.getBlockPos());
        var request = tryInsertItemStack(index, cursorStack, player.getPos());
        if (request.hasAffected()) WorldEffects.lecternPlonk(player.world, player.getPos(), 0.6f, false);
        return true;
    }

    @Override
    public ActionResult book$useOnBlock(ItemUsageContext context) {
        BlockPos blockPos;
        World world = context.getWorld();
        BlockState blockState = world.getBlockState(blockPos = context.getBlockPos());
        var book = context.getStack();

        // Try linking library to book
        if (blockState.isOf(ModBlocks.LIBRARY) && context.getPlayer() != null && context.getPlayer().isSneaking()) {

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

    @Override
    public boolean book$interceptsChatMessage(ItemStack book, ServerPlayerEntity player, String message) {
        return true;
    }

    @Override
    public void book$onInterceptedChatMessage(ItemStack book, ServerPlayerEntity player, String message) {
        var index = getIndex(book, player.world, player.getBlockPos());
        var request = tryExtractItemStacks(index, message, player.getPos().add(0, 1, 0));

        for (ItemStack stack : request.getAffectedStacks()) {
            player.getInventory().offerOrDrop(stack);
        }

        if (request.hasAffected()) WorldEffects.lecternPlonk(player.world, player.getPos(), 1f, false);
        player.sendMessage(request.getMessage(), false);
    }

    @Override
    public PageLecternState lectern$getState(MysticalLecternBlockEntity lectern) {
        return new IndexingLecternState(lectern, this);
    }

    @Override
    public boolean lectern$interceptsChatMessage(MysticalLecternBlockEntity lectern, ServerPlayerEntity player, String message) {
        return true;
    }

    @Override
    public void lectern$onInterceptedChatMessage(MysticalLecternBlockEntity lectern, ServerPlayerEntity player, String message) {
        var world = player.getWorld();
        var blockPos = lectern.getPos();

        var index = getLecternIndex(lectern);
        var request = tryExtractItemStacks(index, message, Vec3d.ofCenter(blockPos));

        var itemPos = Vec3d.ofCenter(blockPos, 1);
        for (ItemStack stack : request.getAffectedStacks()) {
            ItemEntity itemEntity = new ItemEntity(world, itemPos.getX(), itemPos.getY(), itemPos.getZ(), stack);
            itemEntity.setToDefaultPickupDelay();
            itemEntity.setVelocity(Vec3d.ZERO);
            itemEntity.setThrower(EXTRACTED_DROP_UUID);
            world.spawnEntity(itemEntity);
        }

        if (request.hasAffected()) WorldEffects.lecternPlonk(world, itemPos, 1f, true);
        player.sendMessage(request.getMessage(), false);
    }

    @Override
    public void lectern$serverTick(World world, BlockPos pos, BlockState state, MysticalLecternBlockEntity lectern) {
        var centerPos = Vec3d.ofCenter(pos, 0.5);

        if (
                !world.isClient() &&
                lectern.tick % CIRCLE_INTERVAL == 0 &&
                world.getClosestPlayer(
                        pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                        LECTERN_DETECTION_RADIUS, false
                ) != null
        ) {
            WorldEffects.drawParticleCircle(
                    lectern.tick,
                    world, centerPos, CIRCLE_PERIOD,
                    0, LECTERN_DETECTION_RADIUS
            );
            WorldEffects.drawParticleCircle(
                    lectern.tick,
                    world, centerPos, -CIRCLE_PERIOD,
                    0, LECTERN_DETECTION_RADIUS
            );
            WorldEffects.drawParticleCircle(
                    lectern.tick,
                    world, centerPos, CIRCLE_PERIOD,
                    CIRCLE_PERIOD / 2, LECTERN_DETECTION_RADIUS
            );
            WorldEffects.drawParticleCircle(
                    lectern.tick,
                    world, centerPos, -CIRCLE_PERIOD,
                    CIRCLE_PERIOD / 2, LECTERN_DETECTION_RADIUS
            );

            if (lectern.tick % SOUND_INTERVAL == 0) {
                world.playSound(centerPos.getX(), centerPos.getY(), centerPos.getZ(),
                        SoundEvents.BLOCK_BEACON_AMBIENT, SoundCategory.BLOCKS,
                        0.3f, 1.4f + world.getRandom().nextFloat() * 0.4f, true);
            }
        }
    }

    @Override
    public void lectern$onEntityCollision(MysticalLecternBlockEntity lectern, BlockState state, World world, BlockPos pos, Entity entity) {
        if (
                !world.isClient() &&
                entity instanceof ItemEntity itemEntity &&
                !Objects.equals(itemEntity.getThrower(), EXTRACTED_DROP_UUID) &&
                VoxelShapes.matchesAnywhere(VoxelShapes.cuboid(
                                entity.getBoundingBox().offset(-pos.getX(), -pos.getY(), -pos.getZ())),
                        LECTERN_INPUT_AREA_SHAPE, BooleanBiFunction.AND)
        ) {

            var itemStack = itemEntity.getStack();
            var index = getLecternIndex(lectern);

            var request = tryInsertItemStack(index, itemStack, Vec3d.ofCenter(pos));
            if (request.hasAffected()) WorldEffects.lecternPlonk(world, entity.getPos(), 0.6f, true);
        }
    }

    @Override
    public void book$appendPropertiesTooltip(ItemStack book, @Nullable World world, List<Text> properties, TooltipContext context) {
        var linksUsed = getLinks(book);
        var linksMax = getMaxLinks(book);
        double linksUsedRatio = (double) linksUsed / linksMax;

        properties.add(new TranslatableText("item.mystical_index.mystical_book.tooltip.type.indexing.range",
                getMaxRange(book, false))
                .formatted(Formatting.YELLOW));
        properties.add(new TranslatableText("item.mystical_index.mystical_book.tooltip.type.indexing.links",
                linksUsed, linksMax)
                .formatted(Colors.colorByRatio(linksUsedRatio)));
        properties.add(new TranslatableText("item.mystical_index.mystical_book.tooltip.type.indexing.linked_range",
                getMaxRange(book, true))
                .formatted(Formatting.YELLOW));
    }

//    @Override
//    public boolean book$hasGlint(ItemStack book) {
//        return getLinks(book) > 0;
//    }

    public static class IndexingLecternState extends PageLecternState {
        private final LibraryIndex index;

        public IndexingLecternState(MysticalLecternBlockEntity lectern, IndexingTypePage page) {
            super(lectern);

            var book = lectern.getBook();
            var world = lectern.getWorld();
            var pos = lectern.getPos();

            index = page.getIndex(book, world, pos);
        }

        public LibraryIndex getIndex() {
            return index;
        }
    }

    public static abstract class IndexingAttributePage extends AttributePageItem {
        public IndexingAttributePage(String id) {
            super(id);
        }

        @Override
        public List<TypePageItem> getCompatibleTypes(ItemStack page) {
            return List.of(INDEXING_TYPE_PAGE);
        }

        public double getRangeMultiplier(ItemStack page, boolean linked) {
            return 1;
        }

        public double getLinksMultiplier(ItemStack page) {
            return 1;
        }

        @Override
        public void appendAttributes(ItemStack page, NbtCompound nbt) {
            multiplyIntAttribute(nbt, MAX_RANGE_TAG, getRangeMultiplier(page, false));
            multiplyIntAttribute(nbt, MAX_LINKS_TAG, getLinksMultiplier(page));
            multiplyIntAttribute(nbt, MAX_RANGE_LINKED_TAG, getRangeMultiplier(page, true));
        }

        @Override
        public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
            super.appendTooltip(stack, world, tooltip, context);

            var range = getRangeMultiplier(stack, false);
            var links = getLinksMultiplier(stack);
            var linkedRange = getRangeMultiplier(stack, true);

            if (range != 1) tooltip.add(new TranslatableText("item.mystical_index.page.tooltip.type.indexing.range", range)
                    .formatted(Formatting.DARK_GREEN));
            if (links != 1) tooltip.add(new TranslatableText("item.mystical_index.page.tooltip.type.indexing.links", links)
                    .formatted(Formatting.DARK_GREEN));
            if (linkedRange != 1) tooltip.add(new TranslatableText("item.mystical_index.page.tooltip.type.indexing.linked_range", linkedRange)
                    .formatted(Formatting.DARK_GREEN));
        }
    }
}
