package net.messer.mystical_index.item.custom;

import it.unimi.dsi.fastutil.objects.Object2IntArrayMap;
import net.messer.mystical_index.util.BigStack;
import net.messer.mystical_index.util.ContentsIndex;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.StackReference;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.screen.slot.Slot;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.ClickType;
import net.minecraft.util.Formatting;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

public abstract class InventoryBookItem extends BookItem {
    public InventoryBookItem(Settings settings) {
        super(settings);
    }

    @Override
    public boolean onStackClicked(ItemStack book, Slot slot, ClickType clickType, PlayerEntity player) {
        if (clickType != ClickType.RIGHT) {
            return false;
        }
        ItemStack itemStack = slot.getStack();
        if (itemStack.isEmpty()) {
            this.playRemoveOneSound(player);
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
    public boolean onClicked(ItemStack book, ItemStack cursorStack, Slot slot, ClickType clickType, PlayerEntity player, StackReference cursorStackReference) {
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

    public abstract int getMaxTypes();

    public abstract int getMaxStack();

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

    private int getFullness(ItemStack book) {
        int result = 0;
        for (BigStack bigStack : getContents(book).getAll()) {
            result += bigStack.getAmount() * getItemOccupancy(bigStack.getItem());
        }
        return result;
    }

    private static int getItemOccupancy(Item item) {
        return 64 / item.getMaxCount();
    }

    private static Optional<NbtCompound> canMergeStack(ItemStack stack, NbtList items) {
        return items.stream()
                .filter(NbtCompound.class::isInstance)
                .map(NbtCompound.class::cast)
                .filter(item -> ItemStack.canCombine(ItemStack.fromNbt(item.getCompound("Item")), stack))
                .findFirst();
    }

    protected boolean canInsert(Item item) {
        return item.canBeNested();
    }

    public int tryAddItem(ItemStack book, ItemStack stack) {
        if (stack.isEmpty() || !canInsert(stack.getItem())) {
            return 0;
        }
        NbtCompound nbtCompound = book.getOrCreateNbt();
        if (!nbtCompound.contains("Items")) {
            nbtCompound.put("Items", new NbtList());
        }

        int maxFullness = getMaxStack() * 64;
        int fullnessLeft = maxFullness - getFullness(book);
        int canBeTakenAmount = Math.min(stack.getCount(), fullnessLeft / getItemOccupancy(stack.getItem()));
        if (canBeTakenAmount == 0) {
            return 0;
        }

        NbtList nbtList = nbtCompound.getList("Items", 10);
        Optional<NbtCompound> mergeAbleStack = canMergeStack(stack, nbtList);
        if (mergeAbleStack.isPresent()) {
            NbtCompound mergeStack = mergeAbleStack.get();
            mergeStack.putInt("Count", mergeStack.getInt("Count") + canBeTakenAmount);
            nbtList.remove(mergeStack);
            nbtList.add(0, mergeStack);
        } else {
            if (nbtList.size() >= getMaxTypes()) {
                return 0;
            }

            ItemStack insertStack = stack.copy();
            insertStack.setCount(1);
            NbtCompound insertNbt = new NbtCompound();
            insertNbt.put("Item", insertStack.writeNbt(new NbtCompound()));
            insertNbt.putInt("Count", canBeTakenAmount);
            nbtList.add(0, insertNbt);
        }
        return canBeTakenAmount;
    }

    public static Optional<ItemStack> removeFirstStack(ItemStack book) {
        return removeFirstStack(book, null);
    }

    public static Optional<ItemStack> removeFirstStack(ItemStack book, Integer maxAmount) {
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

        return Optional.of(itemStack);
    }

    // TODO get better sounds
    private void playRemoveOneSound(Entity entity) {
        entity.playSound(SoundEvents.ITEM_BUNDLE_REMOVE_ONE, 0.8f, 0.8f + entity.getWorld().getRandom().nextFloat() * 0.4f);
    }

    private void playInsertSound(Entity entity) {
        entity.playSound(SoundEvents.ITEM_BUNDLE_INSERT, 0.8f, 0.8f + entity.getWorld().getRandom().nextFloat() * 0.4f);
    }

    public boolean isEmpty(ItemStack book) {
        return !book.getOrCreateNbt().contains("Items");
    }

    @Override
    public void appendTooltip(ItemStack book, @Nullable World world, List<Text> tooltip, TooltipContext context) {
        for (BigStack bigStack : getContents(book).getAll()) {
            MutableText tooltipEntry = bigStack.getItem().getName().shallowCopy();
            tooltipEntry.append(" x").append(String.valueOf(bigStack.getAmount()));
            tooltip.add(tooltipEntry.formatted(Formatting.GRAY).formatted(Formatting.ITALIC));
        }
        super.appendTooltip(book, world, tooltip, context); // TODO index.getTextList()
    }

    @Override
    public boolean hasGlint(ItemStack book) {
        return !isEmpty(book);
    }
}
