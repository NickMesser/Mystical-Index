package net.messer.mystical_index.item.custom.page.type;

import com.google.common.collect.ImmutableList;
import net.messer.mystical_index.MysticalIndex;
import net.messer.mystical_index.item.custom.page.AttributePageItem;
import net.messer.mystical_index.item.custom.page.TypePageItem;
import net.messer.mystical_index.util.BigStack;
import net.messer.mystical_index.util.ContentsIndex;
import net.messer.mystical_index.util.request.ExtractionRequest;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.StackReference;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.screen.slot.Slot;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.ClickType;
import net.minecraft.util.Formatting;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

import static net.messer.mystical_index.item.ModItems.ITEM_STORAGE_TYPE_PAGE;

public class ItemStorageTypePage extends TypePageItem {
    public static final String MAX_STACKS_TAG = "max_stacks";
    public static final String MAX_TYPES_TAG = "max_types";

    public ItemStorageTypePage(String id) {
        super(id);
    }

    @Override
    public int getColor() {
        return 0x88ff88;
    }

    @Override
    public MutableText getTypeDisplayName() {
        return super.getTypeDisplayName().formatted(Formatting.DARK_AQUA);
    }

    public static final String OCCUPIED_STACKS_TAG = "occupied_stacks";
    public static final String OCCUPIED_TYPES_TAG = "occupied_types";

    public static final String FILTERS_TAG = "filters";
    public static final String ITEM_FILTERS_TAG = "item";
    public static final String TAG_FILTERS_TAG = "tag";

    @Override
    public void onCraftToBook(ItemStack page, ItemStack book) {
        super.onCraftToBook(page, book);

        NbtCompound attributes = getAttributes(book);

        attributes.putInt(MAX_STACKS_TAG, 1);
        attributes.putInt(MAX_TYPES_TAG, 2);
    }

    public int getMaxTypes(ItemStack book) {
        return getAttributes(book).getInt(MAX_TYPES_TAG);
    }

    public int getMaxStack(ItemStack book) {
        return getAttributes(book).getInt(MAX_STACKS_TAG);
    }

    public ContentsIndex getContents(ItemStack book) {
        NbtCompound nbtCompound = book.getNbt();
        ContentsIndex result = new ContentsIndex();
        if (nbtCompound != null) {
            NbtList nbtList = nbtCompound.getList("Items", 10);
            Stream<NbtElement> nbtStream = nbtList.stream();
            Objects.requireNonNull(NbtCompound.class);
            nbtStream.map(NbtCompound.class::cast).forEach(
                    nbt -> result.add(ItemStack.fromNbt(nbt.getCompound("Item")), nbt.getInt("Count")));
        }
        return result;
    }

    protected int getFullness(ItemStack book) {
        int result = 0;
        for (BigStack bigStack : getContents(book).getAll()) {
            result += bigStack.getAmount() * getItemOccupancy(bigStack.getItem());
        }
        return result;
    }

    private int getItemOccupancy(Item item) {
        return 64 / item.getMaxCount();
    }

    private Optional<NbtCompound> canMergeStack(ItemStack stack, NbtList items) {
        return items.stream()
                .filter(NbtCompound.class::isInstance)
                .map(NbtCompound.class::cast)
                .filter(item -> ItemStack.canCombine(ItemStack.fromNbt(item.getCompound("Item")), stack))
                .findFirst();
    }

    private boolean isFiltered(ItemStack book) {
        return !book.getOrCreateNbt().getCompound(FILTERS_TAG).isEmpty();
    }

    protected boolean canInsert(ItemStack book, Item item) {
        if (!item.canBeNested()) return false;

        var filters = book.getOrCreateNbt().getCompound(FILTERS_TAG);
        if (filters.isEmpty()) return true;

        var itemFilter = filters.getList(ITEM_FILTERS_TAG, NbtElement.STRING_TYPE);
        if (itemFilter.contains(NbtString.of(item.toString()))) return false;

        return true;
    }

    public int tryAddItem(ItemStack book, ItemStack stack) {
        if (stack.isEmpty() || !canInsert(book, stack.getItem())) {
            return 0;
        }
        NbtCompound bookNbt = book.getOrCreateNbt();
        if (!bookNbt.contains("Items")) {
            bookNbt.put("Items", new NbtList());
        }

        int maxFullness = getMaxStack(book) * 64;
        int fullnessLeft = maxFullness - getFullness(book);
        int canBeTakenAmount = Math.min(stack.getCount(), fullnessLeft / getItemOccupancy(stack.getItem()));
        if (canBeTakenAmount == 0) {
            return 0;
        }

        NbtList itemsList = bookNbt.getList("Items", 10);
        Optional<NbtCompound> mergeAbleStack = canMergeStack(stack, itemsList);
        if (mergeAbleStack.isPresent()) {
            NbtCompound mergeStack = mergeAbleStack.get();
            mergeStack.putInt("Count", mergeStack.getInt("Count") + canBeTakenAmount);
            itemsList.remove(mergeStack);
            itemsList.add(0, mergeStack);
        } else {
            if (itemsList.size() >= getMaxTypes(book)) {
                return 0;
            }

            ItemStack insertStack = stack.copy();
            insertStack.setCount(1);
            NbtCompound insertNbt = new NbtCompound();
            insertNbt.put("Item", insertStack.writeNbt(new NbtCompound()));
            insertNbt.putInt("Count", canBeTakenAmount);
            itemsList.add(0, insertNbt);
        }

        saveOccupancy(bookNbt,
                maxFullness - fullnessLeft + canBeTakenAmount * getItemOccupancy(stack.getItem()),
                itemsList.size());

        return canBeTakenAmount;
    }

    public Optional<ItemStack> removeFirstStack(ItemStack book) {
        return removeFirstStack(book, null);
    }

    public Optional<ItemStack> removeFirstStack(ItemStack book, Integer maxAmount) {
        NbtCompound bookNbt = book.getOrCreateNbt();
        if (!bookNbt.contains("Items")) {
            return Optional.empty();
        }
        NbtList itemsList = bookNbt.getList("Items", 10);
        if (itemsList.isEmpty()) {
            return Optional.empty();
        }
        NbtCompound firstItem = itemsList.getCompound(0);
        ItemStack itemStack = ItemStack.fromNbt(firstItem.getCompound("Item"));
        int itemCount = firstItem.getInt("Count");
        int takeCount = Math.min(itemCount, itemStack.getMaxCount());
        if (maxAmount != null) {
            takeCount = Math.min(takeCount, maxAmount);
        }

        itemStack.setCount(takeCount);

        if (takeCount >= itemCount) {
            itemsList.remove(0);
            if (itemsList.isEmpty()) {
                book.removeSubNbt("Items");
            }
        } else {
            firstItem.putInt("Count", itemCount - takeCount);
        }

        saveOccupancy(bookNbt, getFullness(book), itemsList.size());

        return Optional.of(itemStack);
    }

    public List<ItemStack> extractItems(ItemStack book, ExtractionRequest request, boolean apply) {
        if (request.isSatisfied())
            return Collections.emptyList();

        NbtCompound bookNbt = book.getOrCreateNbt();
        if (!bookNbt.contains("Items"))
            return Collections.emptyList();

        NbtList itemsList = bookNbt.getList("Items", 10);
        if (itemsList.isEmpty())
            return Collections.emptyList();

        ImmutableList.Builder<ItemStack> builder = ImmutableList.builder();

        for (int i = 0; i < itemsList.size(); i++) {
            NbtCompound nbtItem = itemsList.getCompound(i);
            ItemStack itemStack = ItemStack.fromNbt(nbtItem.getCompound("Item"));

            if (request.matches(itemStack.getItem())) {
                int itemCount = nbtItem.getInt("Count");
                int extractAmount = Math.min(itemCount, request.getAmountUnsatisfied());
                int stackSize = itemStack.getItem().getMaxCount();

                request.satisfy(extractAmount);
                if (apply) {
                    if (extractAmount >= itemCount) {
                        itemsList.remove(i);
                        i -= 1;
                    } else {
                        nbtItem.putInt("Count", itemCount - extractAmount);
                    }
                }

                while (extractAmount > 0) {
                    int extractAmountStack = Math.min(extractAmount, stackSize);

                    ItemStack extractStack = itemStack.copy();
                    extractStack.setCount(extractAmountStack);
                    builder.add(extractStack);

                    extractAmount -= extractAmountStack;
                }
            }
        }

        if (itemsList.isEmpty()) {
            book.removeSubNbt("Items");
        }

        saveOccupancy(bookNbt, getFullness(book), itemsList.size());

        return builder.build();
    }

    public void saveOccupancy(NbtCompound bookNbt, int stacks, int types) {
        bookNbt.putInt(OCCUPIED_STACKS_TAG, stacks);
        bookNbt.putInt(OCCUPIED_TYPES_TAG, types);
    }

    public void playRemoveOneSound(PlayerEntity player) {
        MysticalIndex.playUISound(
                player, SoundEvents.ITEM_BUNDLE_REMOVE_ONE, SoundCategory.PLAYERS, player.getEyePos());
        MysticalIndex.playUISound(
                player, SoundEvents.ITEM_BOOK_PAGE_TURN, SoundCategory.PLAYERS, player.getEyePos(), 0.4f);
    }

    public void playInsertSound(PlayerEntity player) {
        MysticalIndex.playUISound(
                player, SoundEvents.ITEM_BUNDLE_INSERT, SoundCategory.PLAYERS, player.getEyePos());
        MysticalIndex.playUISound(
                player, SoundEvents.ITEM_BOOK_PAGE_TURN, SoundCategory.PLAYERS, player.getEyePos(), 0.4f);
    }

    public boolean isEmpty(ItemStack book) {
        return !book.getOrCreateNbt().contains("Items");
    }

    @Override
    public boolean book$onStackClicked(ItemStack book, Slot slot, ClickType clickType, PlayerEntity player) {
        if (clickType != ClickType.RIGHT) {
            return false;
        }
        ItemStack itemStack = slot.getStack();
        if (itemStack.isEmpty()) {
            playRemoveOneSound(player);
            removeFirstStack(book).ifPresent(removedStack -> tryAddItem(book, slot.insertStack(removedStack)));
        } else {
            int amount = tryAddItem(book, itemStack);
            if (amount > 0) {
                playInsertSound(player);
                itemStack.decrement(amount);
            }
        }
        return true;
    }

    @Override
    public boolean book$onClicked(ItemStack book, ItemStack cursorStack, Slot slot, ClickType clickType, PlayerEntity player, StackReference cursorStackReference) {
        if (clickType != ClickType.RIGHT || !slot.canTakePartial(player)) {
            return false;
        }
        if (cursorStack.isEmpty()) {
            removeFirstStack(book).ifPresent(itemStack -> {
                playRemoveOneSound(player);
                cursorStackReference.set(itemStack);
            });
        } else {
            int amount = tryAddItem(book, cursorStack);
            if (amount > 0) {
                playInsertSound(player);
                cursorStack.decrement(amount);
            }
        }
        return true;
    }

    @Override
    public void book$appendTooltip(ItemStack book, @Nullable World world, List<Text> tooltip, TooltipContext context) {
        for (Text text : getContents(book).getTextList()) {
            tooltip.add(text.copy().formatted(Formatting.GRAY));
        }
    }

    @Override
    public void book$appendPropertiesTooltip(ItemStack book, @Nullable World world, List<Text> tooltip, TooltipContext context) {
        var nbt = book.getOrCreateNbt();

        var stacksOccupied = nbt.getInt(OCCUPIED_STACKS_TAG);
        var stacksTotal = getMaxStack(book) * 64;
        double stacksFullRatio = (double) stacksOccupied / stacksTotal;
        var typesOccupied = nbt.getInt(OCCUPIED_TYPES_TAG);
        var typesTotal = getMaxTypes(book);
        double typesFullRatio = (double) typesOccupied / typesTotal;

        tooltip.add(new TranslatableText("item.mystical_index.mystical_book.tooltip.type.item_storage.stacks",
                stacksOccupied, stacksTotal)
                .formatted(stacksFullRatio < 0.75 ? Formatting.GREEN :
                        stacksFullRatio == 1 ? Formatting.RED : Formatting.GOLD));
        tooltip.add(new TranslatableText("item.mystical_index.mystical_book.tooltip.type.item_storage.types",
                typesOccupied, typesTotal)
                .formatted(typesFullRatio < 0.75 ? Formatting.GREEN :
                        typesFullRatio == 1 ? Formatting.RED : Formatting.GOLD));
    }

    @Override
    public boolean book$hasGlint(ItemStack book) {
        return !isEmpty(book);
    }

    public static abstract class ItemStorageAttributePage extends AttributePageItem {
        @Override
        public List<TypePageItem> getCompatibleTypes(ItemStack page) {
            return List.of(ITEM_STORAGE_TYPE_PAGE);
        }

        public double getStacksMultiplier(ItemStack page) {
            return 1;
        }

        public double getTypesMultiplier(ItemStack page) {
            return 1;
        }

        @Override
        public void appendAttributes(ItemStack page, NbtCompound nbt) {
            multiplyIntAttribute(nbt, MAX_STACKS_TAG, getStacksMultiplier(page));
            multiplyIntAttribute(nbt, MAX_TYPES_TAG, getTypesMultiplier(page));
        }

        @Override
        public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
            super.appendTooltip(stack, world, tooltip, context);

            var stacks = getStacksMultiplier(stack);
            var types = getTypesMultiplier(stack);

            if (stacks != 1) tooltip.add(new TranslatableText("item.mystical_index.page.tooltip.type.item_storage.stacks", stacks)
                    .formatted(Formatting.DARK_GREEN));
            if (types != 1) tooltip.add(new TranslatableText("item.mystical_index.page.tooltip.type.item_storage.types", types)
                    .formatted(Formatting.DARK_GREEN));
        }
    }
}
