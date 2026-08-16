package net.messer.mystical_index.recipe;

import net.messer.mystical_index.item.ModItems;
import net.messer.mystical_index.item.custom.TieredStorageBook;
import net.minecraft.inventory.RecipeInputInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.recipe.RecipeSerializer;
import net.minecraft.recipe.SpecialCraftingRecipe;
import net.minecraft.recipe.book.CraftingRecipeCategory;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;

public class HoldingBookUpgradeRecipe extends SpecialCraftingRecipe {

    public HoldingBookUpgradeRecipe(Identifier id, CraftingRecipeCategory category) {
        super(id, category);
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
    private static int findBookSlot(RecipeInputInventory inventory){
        int bookSlot = -1;
        for(int i = 0; i < inventory.size(); i++) {
            var stack = inventory.getStack(i);
            if(stack.getItem() instanceof TieredStorageBook book && book.getTier() < 4){
                if(bookSlot != -1)
                    return -1;

                bookSlot = i;
            }
        }
        return bookSlot;
    }

    @Override
    public boolean matches(RecipeInputInventory inventory, World world) {
        if (!this.fits(inventory.getWidth(), inventory.getHeight()))
            return false;

        var bookSlot = findBookSlot(inventory);
        if(bookSlot < 0)
            return false;

        var targetTier = ((TieredStorageBook) inventory.getStack(bookSlot).getItem()).getTier() + 1;
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

            var stack = inventory.getStack(i);
            if(stack.isEmpty())
                continue;

            if(stack.getItem() != material)
                return false;

            materialSlots++;
        }

        return materialSlots == required;
    }

    @Override
    public ItemStack craft(RecipeInputInventory inventory, DynamicRegistryManager registryManager) {
        var bookSlot = findBookSlot(inventory);
        if(bookSlot < 0)
            return ItemStack.EMPTY;

        var book = inventory.getStack(bookSlot);
        var resultItem = getUpgradeResult(((TieredStorageBook) book.getItem()).getTier() + 1);
        if(resultItem == null)
            return ItemStack.EMPTY;

        var result = new ItemStack(resultItem);
        // Carries contents, the bound item and the custom name onto the bigger book.
        if(book.hasNbt())
            result.setNbt(book.getNbt().copy());

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
