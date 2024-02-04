package insane96mcp.progressivebosses.module.dragon.entity;

import insane96mcp.progressivebosses.ProgressiveBosses;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;

public class LarvaRenderer extends MobEntityRenderer<Larva, LarvaModel<Larva>> {
	private static final Identifier LARVA_LOCATION = new Identifier(ProgressiveBosses.MODID, "textures/entity/larva.png");

	public LarvaRenderer(EntityRendererFactory.Context p_173994_) {
		super(p_173994_, new LarvaModel<>(p_173994_.getPart(EntityModelLayers.ENDERMITE)), 0.4F);
	}

	@Override
	public Identifier getTexture(Larva p_114482_) {
		return LARVA_LOCATION;
	}

	protected void scale(Larva entitylivingbaseIn, MatrixStack matrixStackIn, float partialTickTime) {
		matrixStackIn.scale(1.5f, 1.5f, 1.5f);
	}
}
