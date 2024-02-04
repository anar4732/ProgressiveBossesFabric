package insane96mcp.progressivebosses.module.dragon.entity;

import net.minecraft.client.model.ModelData;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.model.ModelPartBuilder;
import net.minecraft.client.model.ModelPartData;
import net.minecraft.client.model.ModelTransform;
import net.minecraft.client.model.TexturedModelData;
import net.minecraft.client.render.entity.model.SinglePartEntityModel;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;

public class LarvaModel<T extends Entity> extends SinglePartEntityModel<T> {
	private static final int BODY_COUNT = 4;
	private static final int[][] BODY_SIZES = new int[][]{{4, 3, 2}, {6, 4, 5}, {3, 3, 1}, {1, 2, 1}};
	private static final int[][] BODY_TEXS = new int[][]{{0, 0}, {0, 5}, {0, 14}, {0, 18}};
	private final ModelPart root;
	private final ModelPart[] bodyParts;

	public LarvaModel(ModelPart part) {
		this.root = part;
		this.bodyParts = new ModelPart[4];

		for(int i = 0; i < 4; ++i) {
			this.bodyParts[i] = part.getChild(createSegmentName(i));
		}
	}

	private static String createSegmentName(int p_170548_) {
		return "segment" + p_170548_;
	}

	public static TexturedModelData createBodyLayer() {
		ModelData meshdefinition = new ModelData();
		ModelPartData partdefinition = meshdefinition.getRoot();
		float f = -3.5F;

		for(int i = 0; i < 4; ++i) {
			partdefinition.addChild(createSegmentName(i), ModelPartBuilder.create().uv(BODY_TEXS[i][0], BODY_TEXS[i][1]).cuboid((float)BODY_SIZES[i][0] * -0.5F, 0.0F, (float)BODY_SIZES[i][2] * -0.5F, (float)BODY_SIZES[i][0], (float)BODY_SIZES[i][1], (float)BODY_SIZES[i][2]), ModelTransform.pivot(0.0F, (float)(24 - BODY_SIZES[i][1]), f));
			if (i < 3) {
				f += (float)(BODY_SIZES[i][2] + BODY_SIZES[i + 1][2]) * 0.5F;
			}
		}

		return TexturedModelData.of(meshdefinition, 64, 32);
	}

	public ModelPart getPart() {
		return this.root;
	}

	public void setAngles(T p_102602_, float p_102603_, float p_102604_, float p_102605_, float p_102606_, float p_102607_) {
		for(int i = 0; i < this.bodyParts.length; ++i) {
			this.bodyParts[i].yaw = MathHelper.cos(p_102605_ * 0.9F + (float)i * 0.15F * (float)Math.PI) * (float)Math.PI * 0.01F * (float)(1 + Math.abs(i - 2));
			this.bodyParts[i].pivotX = MathHelper.sin(p_102605_ * 0.9F + (float)i * 0.15F * (float)Math.PI) * (float)Math.PI * 0.1F * (float)Math.abs(i - 2);
		}

	}
}
