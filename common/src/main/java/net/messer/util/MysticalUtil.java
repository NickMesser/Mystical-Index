package net.messer.util;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.loot.LootTable;
import net.minecraft.loot.context.LootContextParameterSet;
import net.minecraft.loot.context.LootContextParameters;
import net.minecraft.loot.context.LootContextTypes;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class MysticalUtil {

    // Item stacks nested inside a book now serialize through a registry lookup, and the inventory
    // classes are built from places that have no world in reach (glint checks, tooltip data). The
    // side that owns one installs it here; the static registries stand in until then so a read
    // before any world exists degrades instead of throwing.
    private static final RegistryWrapper.WrapperLookup FALLBACK_LOOKUP = DynamicRegistryManager.of(Registries.REGISTRIES);

    private static RegistryWrapper.WrapperLookup registryLookup;

    public static void setRegistryLookup(RegistryWrapper.WrapperLookup lookup) {
        if (lookup != null)
            registryLookup = lookup;
    }

    public static RegistryWrapper.WrapperLookup registryLookup() {
        return registryLookup != null ? registryLookup : FALLBACK_LOOKUP;
    }

    // appendTooltip lost its World parameter in 1.21. The client entrypoint installs this so the
    // books that show a countdown can still read the world time, without dragging a client-only
    // class into anything a dedicated server loads. Null outside a world, exactly like the old
    // nullable parameter.
    private static Supplier<World> tooltipWorld = () -> null;

    public static void setTooltipWorldSupplier(Supplier<World> supplier) {
        tooltipWorld = supplier;
    }

    public static World tooltipWorld() {
        return tooltipWorld.get();
    }

    // A stack's loose NBT lives in the custom_data component now. Components are immutable, so
    // every read hands back a copy and a mutation only lands once it is written back.
    public static NbtCompound getCustomData(ItemStack stack) {
        var component = stack.get(DataComponentTypes.CUSTOM_DATA);
        return component == null ? null : component.copyNbt();
    }

    // Mirrors the old getOrCreateNbt(): the compound is attached to the stack even when nothing is
    // written into it, which is what several books read back as "this book has been used".
    public static NbtCompound getOrCreateCustomData(ItemStack stack) {
        var compound = stack.getOrDefault(DataComponentTypes.CUSTOM_DATA, NbtComponent.DEFAULT).copyNbt();
        setCustomData(stack, compound);
        return compound;
    }

    public static NbtCompound copyCustomData(ItemStack stack) {
        return stack.getOrDefault(DataComponentTypes.CUSTOM_DATA, NbtComponent.DEFAULT).copyNbt();
    }

    public static void setCustomData(ItemStack stack, NbtCompound compound) {
        stack.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(compound));
    }

    public static boolean hasCustomData(ItemStack stack) {
        return stack.contains(DataComponentTypes.CUSTOM_DATA);
    }

    // Read, mutate, store: the only safe shape for editing a component in place.
    public static void editCustomData(ItemStack stack, Consumer<NbtCompound> edit) {
        NbtComponent.set(DataComponentTypes.CUSTOM_DATA, stack, edit);
    }

    public static List<ItemStack> generateEntityLoot(PlayerEntity player, Entity entity, Identifier storedEntityLootTable){
        World world = player.getEntityWorld();
        var fakeSword = new ItemStack(Items.DIAMOND_SWORD);
        player.setStackInHand(Hand.MAIN_HAND, fakeSword);
        var source = player.getDamageSources().playerAttack(player);

        LootContextParameterSet context = new LootContextParameterSet.Builder((ServerWorld) world)
                .add(LootContextParameters.THIS_ENTITY, entity)
                .add(LootContextParameters.ORIGIN, player.getPos())
                .add(LootContextParameters.DAMAGE_SOURCE, source)
                .add(LootContextParameters.ATTACKING_ENTITY, player)
                .add(LootContextParameters.DIRECT_ATTACKING_ENTITY, player)
                .add(LootContextParameters.LAST_DAMAGE_PLAYER, player)
                .build(LootContextTypes.ENTITY);

        fakeSword.decrement(1);
        // Books persist the loot table as a plain id string, so the key is rebuilt from it here
        // rather than changing what is written to disk.
        RegistryKey<LootTable> lootTableKey = RegistryKey.of(RegistryKeys.LOOT_TABLE, storedEntityLootTable);
        LootTable lootTable = world.getServer().getReloadableRegistries().getLootTable(lootTableKey);
        return lootTable.generateLoot(context);
    }
}
