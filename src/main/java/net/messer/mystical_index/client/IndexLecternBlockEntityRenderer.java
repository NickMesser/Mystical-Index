package net.messer.mystical_index.client;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.messer.mystical_index.MysticalIndex;
import net.messer.mystical_index.block.entity.IndexLecternBlockEntity;
import net.minecraft.block.BlockState;
import net.minecraft.block.LecternBlock;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory;
import net.minecraft.client.render.entity.model.BookModel;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.client.texture.SpriteAtlasTexture;
import net.minecraft.client.util.SpriteIdentifier;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3f;

@Environment(value = EnvType.CLIENT)
public class IndexLecternBlockEntityRenderer implements BlockEntityRenderer<IndexLecternBlockEntity> {
    public static final SpriteIdentifier BOOK_TEXTURE =
            new SpriteIdentifier(SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE,
                    MysticalIndex.id("entity/index_lectern_book"));

    private final BookModel book;

    public IndexLecternBlockEntityRenderer(BlockEntityRendererFactory.Context ctx) {
        this.book = new BookModel(ctx.getLayerModelPart(EntityModelLayers.BOOK));
    }

    @Override
    public void render(IndexLecternBlockEntity be, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, int overlay) {
        BlockState blockState = be.getCachedState();
        if (!blockState.get(LecternBlock.HAS_BOOK)) {
            return;
        }

        float anim = be.tick + tickDelta;
        float rotationDelta = (be.bookRotationTarget - be.bookRotation) * 0.1f;
        float bookRotation = be.bookRotation + rotationDelta * tickDelta;

        matrices.push();
        double bookHeightOffset = 0.06 + Math.sin(anim * 0.08) * 0.03;
        matrices.translate(0.5, 1.0625 + bookHeightOffset, 0.5);
        float g = blockState.get(LecternBlock.FACING).rotateYClockwise().asRotation();
        matrices.multiply(Vec3f.POSITIVE_Y.getDegreesQuaternion(-g));
        matrices.multiply(Vec3f.POSITIVE_Y.getRadialQuaternion(-bookRotation));
        matrices.multiply(Vec3f.POSITIVE_Z.getDegreesQuaternion(67.5f));
        matrices.translate(0.0, -0.125, 0.0);
        this.book.setPageAngles(0.0f, 0.1f, 0.9f, 1.2f);
        VertexConsumer vertexConsumer = BOOK_TEXTURE.getVertexConsumer(vertexConsumers, RenderLayer::getEntitySolid);
        this.book.renderBook(matrices, vertexConsumer, light, overlay, 1.0f, 1.0f, 1.0f, 1.0f);
        matrices.pop();
    }
}
