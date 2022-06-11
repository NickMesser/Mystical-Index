package net.messer.mystical_index.item.custom.page;

import net.minecraft.client.item.TooltipContext;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Rarity;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public abstract class TypePageItem extends PageItem implements InteractingPage {
    @Override
    public Rarity getRarity(ItemStack stack) {
        return Rarity.RARE;
    }

    public abstract Text getTypeDisplayName();

    public abstract Text getBookDisplayName();

    @Override
    public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
        super.appendTooltip(stack, world, tooltip, context);

        tooltip.add(getTypeDisplayName());
    }
}
