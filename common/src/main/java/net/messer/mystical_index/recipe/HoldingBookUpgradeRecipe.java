package net.messer.mystical_index.recipe;

import net.messer.mystical_index.item.ModItems;
import net.messer.mystical_index.item.custom.TieredStorageBook;
import net.messer.util.MysticalUtil;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.level.Level;

public class HoldingBookUpgradeRecipe extends CustomRecipe {

    public HoldingBookUpgradeRecipe() {

    }

    private static Item getUpgradeMaterial(int targetTier){
        return switch (targetTier) {
            case 2 -> Items.GOLD_INGOT;
            case 3 -> Items.DIAMOND;
            case 4 -> Items.NETHERITE_INGOT;
            default -> null;
        };
    }

    private static int getUpgradeCount(int targetTier){
        return switch (targetTier) {
            case 2, 3 -> 8;
            case 4 -> 4;
            default -> 0;
        };
    }

    private static Item getUpgradeResult(int targetTier){
        return switch (targetTier) {
            case 2 -> ModItems.HOLDING_BOOK_TIER2.get();
            case 3 -> ModItems.HOLDING_BOOK_TIER3.get();
            case 4 -> ModItems.HOLDING_BOOK_TIER4.get();
            default -> null;
        };
    }

    // Slot index rather than the stack, so the material scan can skip the book by position.
    private static int findBookSlot(CraftingInput inventory){
        int bookSlot = -1;
        for(int i = 0; i < inventory.size(); i++) {
            var stack = inventory.getItem(i);
            if(stack.getItem() instanceof TieredStorageBook book && book.getTier() < 4){
                if(bookSlot != -1)
                    return -1;

                bookSlot = i;
            }
        }
        return bookSlot;
    }

    @Override
    public boolean matches(CraftingInput inventory, Level world) {
        if (!(inventory.width() == 3 && inventory.height() == 3))
            return false;

        var bookSlot = findBookSlot(inventory);
        if(bookSlot < 0)
            return false;

        var targetTier = ((TieredStorageBook) inventory.getItem(bookSlot).getItem()).getTier() + 1;
        var material = getUpgradeMaterial(targetTier);
        var required = getUpgradeCount(targetTier);
        if(material == null || required == 0)
            return false;

        // Crafting consumes one item per occupied slot, so the cost only matches the requirement
        // when the material is counted by slot instead of by stack size.
        int materialSlots = 0;
        for(int i = 0; i < inventory.size(); i++) {
            if(i == bookSlot)
                continue;

            var stack = inventory.getItem(i);
            if(stack.isEmpty())
                continue;

            if(stack.getItem() != material)
                return false;

            materialSlots++;
        }

        return materialSlots == required;
    }

    @Override
    public ItemStack assemble(CraftingInput inventory) {
        var bookSlot = findBookSlot(inventory);
        if(bookSlot < 0)
            return ItemStack.EMPTY;

        var book = inventory.getItem(bookSlot);
        var resultItem = getUpgradeResult(((TieredStorageBook) book.getItem()).getTier() + 1);
        if(resultItem == null)
            return ItemStack.EMPTY;

        // Carries contents, the bound item and the custom name onto the bigger book.
        var result = book.transmuteCopy(resultItem, 1);

        return result;
    }


    @Override
    public CraftingBookCategory category() {
        return CraftingBookCategory.MISC;
    }
    @Override
    public RecipeSerializer<HoldingBookUpgradeRecipe> getSerializer() {
        return ModRecipe.HOLDING_BOOK_UPGRADE.get();
    }
}
