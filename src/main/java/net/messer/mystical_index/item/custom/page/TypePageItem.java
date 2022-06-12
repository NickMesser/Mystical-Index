package net.messer.mystical_index.item.custom.page;

import net.minecraft.client.item.TooltipContext;
import net.minecraft.item.ItemStack;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Rarity;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public abstract class TypePageItem extends PageItem implements InteractingPage {
    public final String id;

    public TypePageItem(String id) {
        this.id = id;
    }

    @Override
    public Rarity getRarity(ItemStack stack) {
        return Rarity.RARE;
    }

    public MutableText getTypeDisplayName() {
        return new TranslatableText("item.mystical_index.page.tooltip.type.indexing");
    }

    public Text getBookDisplayName() {
        return new TranslatableText("item.mystical_index.mystical_book.type." + id);
    }

    @Override
    public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
        super.appendTooltip(stack, world, tooltip, context);

        tooltip.add(getTypeDisplayName());
    }
}
