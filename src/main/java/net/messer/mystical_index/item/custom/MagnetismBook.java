package net.messer.mystical_index.item.custom;

import net.messer.config.ModConfig;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class MagnetismBook extends Item {
    public List<Item> itemFilters = new ArrayList<>();
    public MagnetismBook(Settings settings) {
        super(settings);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        if(world.isClient)
            return super.use(world, user, hand);

        ItemStack stack = user.getStackInHand(hand);
        this.readNbt(stack);

        if(user.isSneaking()){
            HitResult hitResult = user.raycast(10, 0, false);
            if (hitResult.getType() == HitResult.Type.MISS)
                return super.use(world, user, hand);

            Box box = Box.from(hitResult.getPos()).expand(.5);
            for(Entity e : world.getNonSpectatingEntities(ItemEntity.class, box)){
                ItemEntity item = (ItemEntity) e;
                Item hitItem = item.getStack().getItem();

                if(itemFilters.contains(hitItem))
                    return super.use(world, user, hand);

                itemFilters.add(hitItem);
                this.markDirty(stack);
                user.sendMessage(Text.literal("Added " + hitItem.getName().getString() + " to the filter."), true);
                return super.use(world, user, hand);
            }
        }
        return super.use(world, user, hand);
    }

    @Override
    public ActionResult useOnBlock(ItemUsageContext context) {
        if(context.getWorld().isClient || !context.getPlayer().isSneaking())
            return super.useOnBlock(context);

        Block hitBlock = context.getWorld().getBlockState(context.getBlockPos()).getBlock();
        if(hitBlock != Blocks.COAL_BLOCK)
            return super.useOnBlock(context);

        PlayerEntity player = context.getPlayer();
        ItemStack itemStack = context.getStack();
        this.readNbt(itemStack);
        itemFilters.clear();
        this.markDirty(itemStack);
        if(player != null)
            player.sendMessage(Text.literal("Cleared all items from the filter."), true);
        return super.useOnBlock(context);
    }

    @Override
    public void inventoryTick(ItemStack stack, World world, Entity entity, int slot, boolean selected) {
        if(world.isClient || !stack.hasNbt())
            return;

        this.readNbt(stack);
        if(itemFilters.isEmpty())
            return;

        Vec3d pos = entity.getPos();
        Vec3d target = pos.add(.05, .05, .05);
        Box box = Box.from(target).expand(ModConfig.MagnetismRange);

        for(ItemEntity e : world.getNonSpectatingEntities(ItemEntity.class, box)){
            if(e.cannotPickup() || !itemFilters.contains(e.getStack().getItem()))
                continue;

            Vec3d itemVector = e.getPos();
            e.move(null, pos.subtract(itemVector).multiply(0.25));
        }
    }

    public void markDirty(ItemStack stack){
        writeNbt(stack);
    }

    public void readNbt(ItemStack stack){
        itemFilters.clear();
        if (!stack.getOrCreateNbt().contains("Filtered Items")){
            stack.getNbt().put("Filtered Items", new NbtList());
        }
        NbtList filteredItems = stack.getNbt().getList("Filtered Items", 10  );
        for (int i = 0; i < filteredItems.size(); i++){
            NbtCompound compound = filteredItems.getCompound(i);
            String itemName = compound.getString("ItemName");
            Item item = Registries.ITEM.get(Identifier.tryParse(itemName));
            itemFilters.add(item);
        }

    }

    public void writeNbt(ItemStack stack){
        NbtList nbtList = new NbtList();

        for (Item item : itemFilters) {
            NbtCompound nbtCompound = new NbtCompound();
            nbtCompound.putString("ItemName", item.toString());
            nbtList.add(nbtCompound);
        }

        stack.getNbt().put("Filtered Items", nbtList);
    }

    @Override
    public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
        if(stack.hasNbt()){
            this.readNbt(stack);
            if(!itemFilters.isEmpty()){
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("§aFiltering: ");
                stringBuilder.append("§e");
                for (Item item: itemFilters) {
                    stringBuilder.append(item.getName().getString()).append(", ");
                }
                stringBuilder.setLength(stringBuilder.length() - 2);

                tooltip.add(Text.literal(stringBuilder.toString()));
                tooltip.add(Text.literal(""));
            }
        }

        if(Screen.hasShiftDown()){
            tooltip.add(Text.translatable("tooltip.mystical_index.magnetism_book_shift0"));
            tooltip.add(Text.translatable("tooltip.mystical_index.magnetism_book_shift1"));
            tooltip.add(Text.translatable("tooltip.mystical_index.magnetism_book_shift2"));
            tooltip.add(Text.translatable("tooltip.mystical_index.magnetism_book_shift3"));
        } else {
            tooltip.add(Text.translatable("tooltip.mystical_index.storage_book"));
        }
        super.appendTooltip(stack, world, tooltip, context);
    }
}
