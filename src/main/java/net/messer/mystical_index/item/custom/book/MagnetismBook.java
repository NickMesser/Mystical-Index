package net.messer.mystical_index.item.custom.book;

import net.messer.mystical_index.MysticalIndex;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.LiteralText;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class MagnetismBook extends BookItem {
    public List<Item> itemFilters = new ArrayList<>();
    public MagnetismBook(Settings settings) {
        super(settings);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        if(world.isClient)
            return super.use(world, user, hand);

        var stack = user.getStackInHand(hand);
        this.readNbt(stack);

        if(user.isSneaking()){
            var hitResult = user.raycast(10, 0, false);
            if (hitResult.getType() == HitResult.Type.MISS)
                return super.use(world, user, hand);

            var box = Box.from(hitResult.getPos()).expand(.5);
            for(Entity e : world.getNonSpectatingEntities(ItemEntity.class, box)){
                ItemEntity item = (ItemEntity) e;
                var hitItem = item.getStack().getItem();

                if(itemFilters.contains(hitItem))
                    return super.use(world, user, hand);

                itemFilters.add(hitItem);
                this.markDirty(stack);
                user.sendMessage(new LiteralText("Added " + hitItem.toString() + " to the filter."), true);
                return super.use(world, user, hand);
            }
        }
        return super.use(world, user, hand);
    }

    @Override
    public ActionResult useOnBlock(ItemUsageContext context) {
        if(context.getWorld().isClient || !context.getPlayer().isSneaking())
            return super.useOnBlock(context);

        var hitBlock = context.getWorld().getBlockState(context.getBlockPos()).getBlock();
        if(hitBlock != Blocks.COAL_BLOCK)
            return super.useOnBlock(context);

        var player = context.getPlayer();
        var itemStack = context.getStack();
        this.readNbt(itemStack);
        itemFilters.clear();
        this.markDirty(itemStack);
        if(player != null)
            player.sendMessage(new LiteralText("Cleared all items from the filter."), true);
        return super.useOnBlock(context);
    }

    @Override
    public void inventoryTick(ItemStack stack, World world, Entity entity, int slot, boolean selected) {
        if(world.isClient || !stack.hasNbt())
            return;

        this.readNbt(stack);
        if(itemFilters.isEmpty())
            return;

        var pos = entity.getPos();
        var target = pos.add(.05, .05, .05);
        var box = Box.from(target).expand(MysticalIndex.CONFIG.BookOfMangetism.Range);

        for(Entity e : world.getNonSpectatingEntities(ItemEntity.class, box)){
            ItemEntity item = (ItemEntity) e;
            if(item.cannotPickup() || !itemFilters.contains(item.getStack().getItem()))
                continue;

            var velocity = item.getPos().relativize(target).normalize().multiply(0.1);
            item.addVelocity(velocity.x, velocity.y, velocity.z);
        }
    }

    public void markDirty(ItemStack stack){
        writeNbt(stack);
    }

    public void readNbt(ItemStack stack){
        if (!stack.getOrCreateNbt().contains("Filtered Items")){
            stack.getNbt().put("Filtered Items", new NbtList());
        }
        var filteredItems = stack.getNbt().getList("Filtered Items", 10  );
        for (int i = 0; i < filteredItems.size(); i++){
            NbtCompound compound = filteredItems.getCompound(i);
            var itemName = compound.getString("ItemName");
            var item = Registry.ITEM.get(Identifier.tryParse(itemName));
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
}
