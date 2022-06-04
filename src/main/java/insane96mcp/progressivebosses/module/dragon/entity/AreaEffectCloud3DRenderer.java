package insane96mcp.progressivebosses.module.dragon.entity;

import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.texture.SpriteAtlasTexture;
import net.minecraft.util.Identifier;

public class AreaEffectCloud3DRenderer extends EntityRenderer<AreaEffectCloud3DEntity> {
	public AreaEffectCloud3DRenderer(EntityRendererFactory.Context context) {
		super(context);
	}

	@Override
	public Identifier getTexture(AreaEffectCloud3DEntity entity) {
		return SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE;
	}
}