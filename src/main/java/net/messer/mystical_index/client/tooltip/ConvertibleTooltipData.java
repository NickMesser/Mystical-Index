package net.messer.mystical_index.client.tooltip;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.tooltip.TooltipComponent;
import net.minecraft.client.item.TooltipData;

@Environment(EnvType.CLIENT)
public interface ConvertibleTooltipData extends TooltipData {
    TooltipComponent getComponent();
}
