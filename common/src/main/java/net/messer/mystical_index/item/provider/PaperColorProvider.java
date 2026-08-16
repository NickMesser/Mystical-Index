package net.messer.mystical_index.item.provider;

import dev.architectury.registry.client.rendering.ColorHandlerRegistry;
import net.messer.mystical_index.item.ModItems;
import net.messer.util.MysticalUtil;
import net.minecraft.entity.EntityType;
import net.minecraft.item.ItemStack;
import net.minecraft.item.SpawnEggItem;

public class PaperColorProvider {

    public static void register() {
        ColorHandlerRegistry.registerItemColors(PaperColorProvider::getColor, ModItems.ENTITY_PAPER);
    }

    private static int getColor(ItemStack stack, int tintIndex) {
        var compound = MysticalUtil.getCustomData(stack);
        if(compound == null) return 0xFFFFFF;

        var entityId = compound.getString("entity");
        // A missing/blank/unresolvable id (removed mod, anvil rename) leaves the Optional empty;
        // .get() there crashed rendering every frame. Fall back to a default colour instead.
        var entity = EntityType.get(entityId).orElse(null);
        if(entity == null) return 0xFFFFFF;

        var spawnEgg = SpawnEggItem.forEntity(entity);
        if(spawnEgg == null) return 0xFFFFFF;

        return spawnEgg.getColor(tintIndex);
    }
}
