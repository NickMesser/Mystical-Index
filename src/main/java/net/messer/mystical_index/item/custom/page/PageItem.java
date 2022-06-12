package net.messer.mystical_index.item.custom.page;

import net.fabricmc.fabric.api.item.v1.FabricItemSettings;
import net.messer.mystical_index.block.entity.MysticalLecternBlockEntity;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Formatting;
import net.minecraft.util.Rarity;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.List;

import static net.messer.mystical_index.item.custom.page.AttributePageItem.ATTRIBUTES_TAG;

public abstract class PageItem extends Item {
    public PageItem() {
        super(new FabricItemSettings().group(ItemGroup.TOOLS));
    }

    public void onCraftToBook(ItemStack page, ItemStack book) {
    }

    public void book$inventoryTick(ItemStack book, World world, Entity entity, int slot, boolean selected) {
    }

    public void book$appendTooltip(ItemStack book, @Nullable World world, List<Text> properties, TooltipContext context) {
    }

    public void book$appendPropertiesTooltip(ItemStack book, @Nullable World world, List<Text> properties, TooltipContext context) {
    }

    /**
     * The actual handling of intercepted chat messages should happen here.
     * This is run on the main server thread.
     */
    public void book$onInterceptedChatMessage(ItemStack book, ServerPlayerEntity player, String message) {
    }

    /**
     * The actual handling of intercepted chat messages should happen here.
     * This is run on the main server thread.
     */
    public void lectern$onInterceptedChatMessage(MysticalLecternBlockEntity lectern, ServerPlayerEntity player, String message) {
    }

    @Override
    public Rarity getRarity(ItemStack page) {
        return Rarity.UNCOMMON;
    }

    public abstract int getColor();

    public NbtCompound getAttributes(ItemStack book) {
        return book.getOrCreateSubNbt(ATTRIBUTES_TAG);
    }

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
    }
}
