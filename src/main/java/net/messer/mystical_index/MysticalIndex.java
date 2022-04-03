package net.messer.mystical_index;

import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.serializer.JanksonConfigSerializer;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.fabricmc.fabric.api.item.v1.FabricItem;
import net.fabricmc.fabric.impl.event.interaction.InteractionEventsRouter;
import net.messer.config.MysticalConfig;
import net.messer.mystical_index.block.ModBlockEntities;
import net.messer.mystical_index.block.ModBlocks;
import net.messer.mystical_index.events.EventListeners;
import net.messer.mystical_index.item.ModItems;
import net.messer.mystical_index.item.ModRecipes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.s2c.play.PlaySoundIdS2CPacket;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.math.Vec3d;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class MysticalIndex implements ModInitializer {

	public static final Logger LOGGER = LogManager.getLogger("mystical_index");
	public static final String MOD_ID = "mystical_index";

	public static MysticalConfig CONFIG;

	@Override
	public void onInitialize() {
		AutoConfig.register(MysticalConfig.class, JanksonConfigSerializer::new);
		CONFIG = AutoConfig.getConfigHolder(MysticalConfig.class).getConfig();

		ModItems.registerModItems();
		ModBlocks.registerModBlocks();
		ModBlockEntities.registerBlockEntities();
		ModRecipes.registerModRecipes();

		EventListeners.register();
	}

	public static void playSoundOnServer(PlayerEntity player, SoundEvent sound, SoundCategory category, Vec3d pos) {
		playSoundOnServer(player, sound, category, pos, 0.8f);
	}

	public static void playSoundOnServer(PlayerEntity player, SoundEvent sound, SoundCategory category, Vec3d pos, float volume) {
		if (player instanceof ServerPlayerEntity serverPlayer)
			serverPlayer.networkHandler.sendPacket(new PlaySoundIdS2CPacket(
					sound.getId(), category, pos,
					volume, 0.8f + player.getWorld().getRandom().nextFloat() * 0.4f));
	}
}
