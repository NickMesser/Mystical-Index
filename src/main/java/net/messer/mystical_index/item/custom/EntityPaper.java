package net.messer.mystical_index.item.custom;

import net.messer.mystical_index.MysticalIndex;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.world.World;

public class EntityPaper extends Item {
    public EntityPaper(Settings settings) {
        super(settings);
    }

    @Override
    public void onCraft(ItemStack stack, World world, PlayerEntity player) {
        var nbt = stack.getNbt();
        if (nbt == null)
            nbt = stack.getOrCreateNbt();

        var entityId = nbt.getString("entity");
        var entityType = EntityType.get(entityId).get();
        stack.setCustomName(Text.of( entityType.getName().getString() + " Paper"));
        super.onCraft(stack, world, player);
    }

    @Override
    public ActionResult useOnEntity(ItemStack stack, PlayerEntity user, LivingEntity entity, Hand hand) {
        if(user.getWorld().isClient)
            return super.useOnEntity(stack, user, entity, hand);

        var compound = stack.getNbt();
        if (compound == null)
            compound = stack.getOrCreateNbt();

        var entityId = Registries.ENTITY_TYPE.getId(entity.getType()).toString();
        stack.setCustomName(entity.getType().getName());
        compound.putString("entity", entityId);
        return super.useOnEntity(stack, user, entity, hand);
    }
}
