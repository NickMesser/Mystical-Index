package net.messer.mystical_index.item.custom;

import net.minecraft.core.registries.Registries;
import net.messer.util.MysticalUtil;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.Level;

public class EntityPaper extends Item {
    public EntityPaper(Item.Properties settings) {
        super(settings);
    }

    @Override
    public void onCraftedBy(ItemStack stack, Player player) {
        var nbt = MysticalUtil.getOrCreateCustomData(stack);

        // Paper crafted without a bound entity has no usable id here.
        var entityId = nbt.getStringOr("entity", "");
        var entityType = EntityType.byString(entityId).orElse(null);
        if (entityType != null)
            stack.set(DataComponents.CUSTOM_NAME, Component.translatable("item.mystical_index.entity_paper.named", entityType.getDescription()));

        super.onCraftedBy(stack, player);
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player user, LivingEntity entity, InteractionHand hand) {
        if(user.level().isClientSide())
            return super.interactLivingEntity(stack, user, entity, hand);

        var entityId = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).toString();
        stack.set(DataComponents.CUSTOM_NAME, Component.translatable("item.mystical_index.entity_paper.named", entity.getType().getDescription()));
        MysticalUtil.editCustomData(stack, compound -> compound.putString("entity", entityId));
        return super.interactLivingEntity(stack, user, entity, hand);
    }
}
