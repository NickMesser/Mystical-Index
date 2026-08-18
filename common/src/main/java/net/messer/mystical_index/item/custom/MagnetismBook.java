package net.messer.mystical_index.item.custom;

import net.messer.mystical_index.item.inventory.MagnetFilterData;
import net.messer.mystical_index.screen.MagnetismScreenHandler;
import net.messer.util.SelfUpdatingBook;
import net.minecraft.core.registries.Registries;
import net.messer.config.ModConfig;
import net.messer.util.MysticalUtil;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionHand;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.Minecraft;

import net.minecraft.server.level.ServerLevel;

import net.minecraft.world.entity.EquipmentSlot;

import net.minecraft.world.item.component.TooltipDisplay;

import java.util.function.Consumer;

public class MagnetismBook extends Item implements SelfUpdatingBook {
    public List<Item> itemFilters = new ArrayList<>();
    public MagnetismBook(Item.Properties settings) {
        super(settings);
    }

    @Override
    public InteractionResult use(Level world, Player user, InteractionHand hand) {
        if(world.isClientSide())
            return super.use(world, user, hand);

        ItemStack stack = user.getItemInHand(hand);
        this.readNbt(stack);

        if(user.isShiftKeyDown()){
            HitResult hitResult = user.pick(10, 0, false);
            // Aimed at nothing: the sneak click is the quick on/off instead. The existing
            // aim-at-an-item-to-filter-it gesture is kept rather than replaced - it is the only
            // way to fill the filter without opening anything, and players already know it.
            if (hitResult.getType() == HitResult.Type.MISS)
                return toggleMagnet(stack, user, world, hand);

            AABB box = AABB.ofSize(hitResult.getLocation(), (.5) * 2, (.5) * 2, (.5) * 2);
            for(Entity e : world.getEntitiesOfClass(ItemEntity.class, box)){
                ItemEntity item = (ItemEntity) e;
                Item hitItem = item.getItem().getItem();

                if(itemFilters.contains(hitItem))
                    return super.use(world, user, hand);

                // The grid is the filter now. If it is full the gesture does nothing at all -
                // including not growing the legacy list behind the player's back, which would
                // otherwise keep affecting matching with no square to show for it.
                if (!new MagnetFilterData(stack).addToFirstEmpty(hitItem)) {
                    user.sendOverlayMessage(Component.translatable("message.mystical_index.magnetism_full"));
                    return super.use(world, user, hand);
                }

                itemFilters.add(hitItem);
                this.markDirty(stack);
                user.sendOverlayMessage(Component.translatable("message.mystical_index.magnetism_added", hitItem.getName(hitItem.getDefaultInstance())));
                return super.use(world, user, hand);
            }

            // Aimed at a block or a mob rather than a dropped item - nothing to filter, so treat it
            // the same as aiming at nothing.
            return toggleMagnet(stack, user, world, hand);
        }

        // Plain right-click opens the filter screen. Same both-sides hand resolution the menu uses.
        if (user instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
            serverPlayer.openMenu(new net.minecraft.world.SimpleMenuProvider(
                    (syncId, inventory, player) -> new MagnetismScreenHandler(syncId, inventory),
                    Component.translatable("container.mystical_index.magnetism")));
        }
        return InteractionResult.CONSUME;
    }

    /** Sneak shortcut: flip the magnet off, or back to whatever it was doing before. */
    private InteractionResult toggleMagnet(ItemStack stack, Player user, Level world, InteractionHand hand) {
        var mode = MagnetFilterData.toggleDisabled(stack);
        user.sendOverlayMessage(Component.translatable(
                mode == MagnetFilterData.Mode.NONE
                        ? "message.mystical_index.magnetism_disabled"
                        : "message.mystical_index.magnetism_enabled",
                Component.translatable(mode.translationKey())));
        return super.use(world, user, hand);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if(context.getLevel().isClientSide() || context.getPlayer() == null || !context.getPlayer().isShiftKeyDown())
            return super.useOn(context);

        Block hitBlock = context.getLevel().getBlockState(context.getClickedPos()).getBlock();
        if(hitBlock != Blocks.COAL_BLOCK)
            return super.useOn(context);

        Player player = context.getPlayer();
        ItemStack itemStack = context.getItemInHand();
        this.readNbt(itemStack);
        itemFilters.clear();
        this.markDirty(itemStack);
        if(player != null)
            player.sendOverlayMessage(Component.translatable("message.mystical_index.magnetism_cleared"));
        return super.useOn(context);
    }

    @Override
    public void inventoryTick(ItemStack stack, ServerLevel world, Entity entity, EquipmentSlot slot) {
        if(world.isClientSide())
            return;

        // Gated on the MODE, not on "has custom data". The old gate came from the design where the
        // book was inert until the gesture bound something, and it meant a book set to Magnet All
        // through the UI alone could still be skipped entirely. NONE is the only mode that does
        // nothing, so it is the only one that returns early.
        if (MagnetFilterData.modeOf(stack) == MagnetFilterData.Mode.NONE)
            return;

        // One filter object per tick, not per candidate entity: the set behind it is built once
        // on load and the mode is a single string read, so the per-entity question below is a hash
        // lookup rather than an NBT walk.
        var filter = new MagnetFilterData(stack);

        Vec3 pos = entity.position();
        Vec3 target = pos.add(.05, .05, .05);
        AABB box = AABB.ofSize(target, (ModConfig.MagnetismRange) * 2, (ModConfig.MagnetismRange) * 2, (ModConfig.MagnetismRange) * 2);

        for(ItemEntity e : world.getEntitiesOfClass(ItemEntity.class, box)){
            if(e.hasPickUpDelay() || !filter.allows(e.getItem().getItem()))
                continue;

            Vec3 itemVector = e.position();
            e.push(pos.subtract(itemVector).scale(0.25));
        }
    }

    public void markDirty(ItemStack stack){
        writeNbt(stack);
    }

    public void readNbt(ItemStack stack){
        itemFilters.clear();
        var compound = MysticalUtil.getOrCreateCustomData(stack);
        if (!compound.contains("Filtered Items")){
            compound.put("Filtered Items", new ListTag());
            MysticalUtil.setCustomData(stack, compound);
        }
        ListTag filteredItems = compound.getListOrEmpty("Filtered Items");
        for (int i = 0; i < filteredItems.size(); i++){
            CompoundTag entry = filteredItems.getCompoundOrEmpty(i);
            String itemName = entry.getStringOr("ItemName", "");
            Item item = BuiltInRegistries.ITEM.getValue(Identifier.tryParse(itemName));
            itemFilters.add(item);
        }

    }

    public void writeNbt(ItemStack stack){
        ListTag nbtList = new ListTag();

        for (Item item : itemFilters) {
            CompoundTag nbtCompound = new CompoundTag();
            // Item.toString() returns only the path, so modded items round-tripped back as
            // minecraft:<path> and resolved to air. Store the full identifier.
            nbtCompound.putString("ItemName", BuiltInRegistries.ITEM.getKey(item).toString());
            nbtList.add(nbtCompound);
        }

        MysticalUtil.editCustomData(stack, compound -> compound.put("Filtered Items", nbtList));
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag type) {
        var mode = MagnetFilterData.modeOf(stack);
        tooltip.accept(Component.translatable("tooltip.mystical_index.magnetism_book.mode",
                Component.translatable(mode.translationKey()),
                new MagnetFilterData(stack).filledCount()));

        if(MysticalUtil.hasCustomData(stack)){
            this.readNbt(stack);
            if(!itemFilters.isEmpty()){
                StringBuilder stringBuilder = new StringBuilder();
                for (Item item: itemFilters) {
                    stringBuilder.append(item.getName(item.getDefaultInstance()).getString()).append(", ");
                }
                stringBuilder.setLength(stringBuilder.length() - 2);

                tooltip.accept(Component.translatable("tooltip.mystical_index.magnetism_book.filtering", stringBuilder.toString()));
                tooltip.accept(Component.literal(""));
            }
        }

        if(Minecraft.getInstance().hasShiftDown()){
            tooltip.accept(Component.translatable("tooltip.mystical_index.magnetism_book_shift0"));
            tooltip.accept(Component.translatable("tooltip.mystical_index.magnetism_book_shift1"));
            tooltip.accept(Component.translatable("tooltip.mystical_index.magnetism_book_shift2"));
            tooltip.accept(Component.translatable("tooltip.mystical_index.magnetism_book_shift3"));
        } else {
            tooltip.accept(Component.translatable("tooltip.mystical_index.magnetism_book"));
        }
        super.appendHoverText(stack, context, display, tooltip, type);
    }
}
