package net.messer.mystical_index.item.custom.page;

import net.fabricmc.fabric.api.item.v1.FabricItemSettings;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.Entity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Formatting;
import net.minecraft.util.Rarity;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public abstract class PageItem extends Item {
    public PageItem() {
        super(new FabricItemSettings().group(ItemGroup.TOOLS));
    }

    public void onCraftToBook(ItemStack page, ItemStack book) {
    }

    public void bookInventoryTick(ItemStack book, World world, Entity entity, int slot, boolean selected) {
    }

    public void bookAppendTooltip(ItemStack book, List<Text> properties) {
    }

    @Override
    public Rarity getRarity(ItemStack stack) {
        return Rarity.UNCOMMON;
    }

    public abstract int getColor();

    // TODO display color on item texture somehow
//    @Override
//    public ItemStack getPolymerItemStack(ItemStack itemStack, @Nullable ServerPlayerEntity player) {
//        var returnStack = PolymerItem.super.getPolymerItemStack(itemStack, player);
//        returnStack.getOrCreateSubNbt("display").putInt("MapColor", getColor());
//        return returnStack;
//    }

    @Override
    public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
        super.appendTooltip(stack, world, tooltip, context);

        tooltip.add(new LiteralText(""));
        tooltip.add(new TranslatableText("item.mystical_index.page.tooltip.when_applied").formatted(Formatting.GRAY));

        bookAppendTooltip(null, tooltip);
    }
}
