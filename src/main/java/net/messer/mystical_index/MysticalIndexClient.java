package net.messer.mystical_index;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.rendering.v1.BlockEntityRendererRegistry;
import net.fabricmc.fabric.api.event.client.ClientSpriteRegistryCallback;
import net.messer.mystical_index.block.ModBlockEntities;
import net.messer.mystical_index.client.IndexLecternBlockEntityRenderer;
import net.messer.mystical_index.client.ModelProviders;
import net.messer.mystical_index.client.NetworkListeners;
import net.minecraft.client.texture.SpriteAtlasTexture;

@Environment(value = EnvType.CLIENT)
public class MysticalIndexClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        NetworkListeners.registerListeners();

        ClientSpriteRegistryCallback.event(SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE).register((atlasTexture, registry) -> {
            registry.register(IndexLecternBlockEntityRenderer.BOOK_TEXTURE.getTextureId());
        });
        BlockEntityRendererRegistry.register(ModBlockEntities.INDEX_LECTERN_BLOCK_ENTITY, IndexLecternBlockEntityRenderer::new);

        ModelProviders.register();
    }
}
