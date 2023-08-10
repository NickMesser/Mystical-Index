package net.messer.mystical_index.screen;

import io.github.cottonmc.cotton.gui.client.CottonInventoryScreen;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;

public class TestBlockScreen extends CottonInventoryScreen<TestBlockGuiDescription> {
    public TestBlockScreen(TestBlockGuiDescription description, PlayerEntity player, Text title) {
        super(description, player, title);
    }
}
