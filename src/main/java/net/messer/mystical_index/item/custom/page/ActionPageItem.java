package net.messer.mystical_index.item.custom.page;

import net.minecraft.client.item.TooltipContext;
import net.minecraft.item.ItemStack;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public abstract class ActionPageItem extends PageItem implements TypeDependentPage, InteractingPage {
    public ActionPageItem(String id) {
        super(id);
    }

    public MutableText getActionDisplayName() {
        return new TranslatableText("item.mystical_index.page.tooltip.action." + id);
    }

    @Override
    public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
        super.appendTooltip(stack, world, tooltip, context);

        tooltip.add(getActionDisplayName());
    }

    @Override
    public void book$appendPropertiesTooltip(ItemStack book, @Nullable World world, List<Text> properties, TooltipContext context) {
        super.book$appendPropertiesTooltip(book, world, properties, context);

        properties.add(getActionDisplayName());
    }
}
