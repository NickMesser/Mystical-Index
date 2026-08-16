package net.messer.mystical_index.item.provider;

import net.fabricmc.fabric.api.client.rendering.v1.ColorProviderRegistry;
import net.messer.mystical_index.item.ModItems;
import net.minecraft.client.color.item.ItemColorProvider;
import net.minecraft.entity.EntityType;
import net.minecraft.item.ItemStack;
import net.minecraft.item.SpawnEggItem;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

public class PaperColorProvider {

    public static void register() {
        ColorProviderRegistry.ITEM.register(PaperColorProvider::getColor, ModItems.ENTITY_PAPER);
    }

    private static int getColor(ItemStack stack, int tintIndex) {
        var compound = stack.getNbt();
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
