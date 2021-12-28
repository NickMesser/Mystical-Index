package net.messer.firstmod;

import net.fabricmc.api.ModInitializer;
import net.messer.firstmod.block.ModBlocks;
import net.messer.firstmod.block.entity.LibraryBlockEntity;
import net.messer.firstmod.item.ModItems;
import net.minecraft.block.entity.BlockEntityType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class FirstMod implements ModInitializer {

	public static final Logger LOGGER = LogManager.getLogger("firstmod");
	public static final String MOD_ID = "firstmod";

	public static BlockEntityType<LibraryBlockEntity> LIBRARY_BLOCK_ENTITY;


	@Override
	public void onInitialize() {
		ModItems.registerModItems();
		ModBlocks.registerModBlocks();

		LOGGER.info("Hello Fabric world!");
	}
}
