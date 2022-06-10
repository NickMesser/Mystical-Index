package net.messer.mystical_index;

import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.serializer.JanksonConfigSerializer;
import net.fabricmc.api.ModInitializer;
import net.messer.config.MysticalConfig;
import net.messer.mystical_index.block.ModBlockEntities;
import net.messer.mystical_index.block.ModBlocks;
import net.messer.mystical_index.event.EventListeners;
import net.messer.mystical_index.item.ModItems;
import net.messer.mystical_index.item.ModRecipes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;
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

	public static Identifier id(String path) {
		return new Identifier(MOD_ID, path);
	}

	public static void playUISound(PlayerEntity player, SoundEvent sound, SoundCategory category, Vec3d pos) {
		playUISound(player, sound, category, pos, 0.8f);
	}

	public static void playUISound(PlayerEntity player, SoundEvent sound, SoundCategory category, Vec3d pos, float volume) {
		player.playSound(sound, category, volume, 0.8f + player.getWorld().getRandom().nextFloat() * 0.4f);
	}
}
