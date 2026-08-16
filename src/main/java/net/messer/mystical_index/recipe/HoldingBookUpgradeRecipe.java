package net.messer.mystical_index.recipe;

import net.messer.mystical_index.item.ModItems;
import net.messer.mystical_index.item.custom.TieredStorageBook;
import net.messer.util.MysticalUtil;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.recipe.RecipeSerializer;
import net.minecraft.recipe.SpecialCraftingRecipe;
import net.minecraft.recipe.book.CraftingRecipeCategory;
import net.minecraft.recipe.input.CraftingRecipeInput;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.world.World;

public class HoldingBookUpgradeRecipe extends SpecialCraftingRecipe {

    public HoldingBookUpgradeRecipe(CraftingRecipeCategory category) {
        super(category);
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
            case 2 -> ModItems.HOLDING_BOOK_TIER2;
            case 3 -> ModItems.HOLDING_BOOK_TIER3;
            case 4 -> ModItems.HOLDING_BOOK_TIER4;
            default -> null;
        };
    }

    // Slot index rather than the stack, so the material scan can skip the book by position.
    private static int findBookSlot(CraftingRecipeInput inventory){
        int bookSlot = -1;
        for(int i = 0; i < inventory.getSize(); i++) {
            var stack = inventory.getStackInSlot(i);
            if(stack.getItem() instanceof TieredStorageBook book && book.getTier() < 4){
                if(bookSlot != -1)
                    return -1;

                bookSlot = i;
            }
        }
        return bookSlot;
    }

    @Override
    public boolean matches(CraftingRecipeInput inventory, World world) {
        if (!this.fits(inventory.getWidth(), inventory.getHeight()))
            return false;

        var bookSlot = findBookSlot(inventory);
        if(bookSlot < 0)
            return false;

        var targetTier = ((TieredStorageBook) inventory.getStackInSlot(bookSlot).getItem()).getTier() + 1;
        var material = getUpgradeMaterial(targetTier);
        var required = getUpgradeCount(targetTier);
        if(material == null || required == 0)
            return false;

        // Crafting consumes one item per occupied slot, so the cost only matches the requirement
        // when the material is counted by slot instead of by stack size.
        int materialSlots = 0;
        for(int i = 0; i < inventory.getSize(); i++) {
            if(i == bookSlot)
                continue;

            var stack = inventory.getStackInSlot(i);
            if(stack.isEmpty())
                continue;

            if(stack.getItem() != material)
                return false;

            materialSlots++;
        }

        return materialSlots == required;
    }

    @Override
    public ItemStack craft(CraftingRecipeInput inventory, RegistryWrapper.WrapperLookup registryLookup) {
        var bookSlot = findBookSlot(inventory);
        if(bookSlot < 0)
            return ItemStack.EMPTY;

        var book = inventory.getStackInSlot(bookSlot);
        var resultItem = getUpgradeResult(((TieredStorageBook) book.getItem()).getTier() + 1);
        if(resultItem == null)
            return ItemStack.EMPTY;

        // Carries contents, the bound item and the custom name onto the bigger book.
        var result = book.copyComponentsToNewStack(resultItem, 1);

        return result;
    }

    @Override
    public boolean fits(int width, int height) {
        return width == 3 && height == 3;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipe.HOLDING_BOOK_UPGRADE;
    }
}
