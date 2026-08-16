package net.messer.mystical_index.item.custom;

import net.messer.util.MysticalUtil;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
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
    public void onCraftByPlayer(ItemStack stack, World world, PlayerEntity player) {
        var nbt = MysticalUtil.getOrCreateCustomData(stack);

        // Paper crafted without a bound entity has no usable id here.
        var entityId = nbt.getString("entity");
        var entityType = EntityType.get(entityId).orElse(null);
        if (entityType != null)
            stack.set(DataComponentTypes.CUSTOM_NAME, Text.of( entityType.getName().getString() + " Paper"));

        super.onCraftByPlayer(stack, world, player);
    }

    @Override
    public ActionResult useOnEntity(ItemStack stack, PlayerEntity user, LivingEntity entity, Hand hand) {
        if(user.getWorld().isClient)
            return super.useOnEntity(stack, user, entity, hand);

        var entityId = Registries.ENTITY_TYPE.getId(entity.getType()).toString();
        stack.set(DataComponentTypes.CUSTOM_NAME, entity.getType().getName());
        MysticalUtil.editCustomData(stack, compound -> compound.putString("entity", entityId));
        return super.useOnEntity(stack, user, entity, hand);
    }
}
