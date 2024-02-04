package insane96mcp.progressivebosses.module.wither.entity;

import insane96mcp.progressivebosses.ProgressiveBosses;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.entity.BipedEntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.feature.ArmorFeatureRenderer;
import net.minecraft.client.render.entity.model.EntityModelLayer;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;

@Environment(value=EnvType.CLIENT)
public class WitherMinionRenderer extends BipedEntityRenderer<WitherMinion, WitherMinionModel<WitherMinion>> {
	private static final Identifier MINION_TEXTURES = new Identifier(ProgressiveBosses.MODID, "textures/entity/wither_minion.png");

	public WitherMinionRenderer(EntityRendererFactory.Context context) {
		this(context, EntityModelLayers.SKELETON, EntityModelLayers.SKELETON_INNER_ARMOR, EntityModelLayers.SKELETON_OUTER_ARMOR);
	}

	public WitherMinionRenderer(EntityRendererFactory.Context p_174382_, EntityModelLayer p_174383_, EntityModelLayer p_174384_, EntityModelLayer p_174385_) {
		super(p_174382_, new WitherMinionModel<>(p_174382_.getPart(p_174383_)), 0.5F);
		this.addFeature(new ArmorFeatureRenderer<>(this, new WitherMinionModel<>(p_174382_.getPart(p_174384_)), new WitherMinionModel<>(p_174382_.getPart(p_174385_)), p_174382_.getModelManager()));
	}

	/**
	 * Returns the location of an entity's texture.
	 */
	public Identifier getTexture(WitherMinion entity) {
		return MINION_TEXTURES;
	}

	protected void scale(WitherMinion entitylivingbaseIn, MatrixStack matrixStackIn, float partialTickTime) {
		matrixStackIn.scale(0.75f, 0.75f, 0.75f);
	}
}
