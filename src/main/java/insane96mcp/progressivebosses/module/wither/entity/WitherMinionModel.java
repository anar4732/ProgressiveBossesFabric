package insane96mcp.progressivebosses.module.wither.entity;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.client.render.entity.model.CrossbowPosing;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.ai.RangedAttackMob;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Arm;
import net.minecraft.util.Hand;
import net.minecraft.util.math.MathHelper;

@Environment(value=EnvType.CLIENT)
public class WitherMinionModel<T extends MobEntity & RangedAttackMob> extends BipedEntityModel<T> {

	public WitherMinionModel(ModelPart part) {
		super(part);
		//if (!part.visible) {
		//	this.rightArm = new ModelPart(this, 40, 16);
		//	this.rightArm.setPos(-1.0F, -2.0F, -1.0F, 2.0F, 12.0F, 2.0F, modelSize);
		//	this.rightArm.setPos(-5.0F, 2.0F, 0.0F);
		//	this.leftArm = new ModelPart(this, 40, 16);
		//	this.leftArm.mirror = true;
		//	this.leftArm.addBox(-1.0F, -2.0F, -1.0F, 2.0F, 12.0F, 2.0F, modelSize);
		//	this.leftArm.setPos(5.0F, 2.0F, 0.0F);
		//	this.rightLeg = new ModelPart(this, 0, 16);
		//	this.rightLeg.addBox(-1.0F, 0.0F, -1.0F, 2.0F, 12.0F, 2.0F, modelSize);
		//	this.rightLeg.setPos(-2.0F, 12.0F, 0.0F);
		//	this.leftLeg = new ModelPart(this, 0, 16);
		//	this.leftLeg.mirror = true;
		//	this.leftLeg.addBox(-1.0F, 0.0F, -1.0F, 2.0F, 12.0F, 2.0F, modelSize);
		//	this.leftLeg.setPos(2.0F, 12.0F, 0.0F);
		//}

	}

	public void prepareMobModel(T entityIn, float limbSwing, float limbSwingAmount, float partialTick) {
		this.rightArmPose = BipedEntityModel.ArmPose.EMPTY;
		this.leftArmPose = BipedEntityModel.ArmPose.EMPTY;
		ItemStack itemstack = entityIn.getStackInHand(Hand.MAIN_HAND);
		if (itemstack.getItem() == Items.BOW && entityIn.isAttacking()) {
			if (entityIn.getMainArm() == Arm.RIGHT) {
				this.rightArmPose = BipedEntityModel.ArmPose.BOW_AND_ARROW;
			} else {
				this.leftArmPose = BipedEntityModel.ArmPose.BOW_AND_ARROW;
			}
		}

		super.animateModel(entityIn, limbSwing, limbSwingAmount, partialTick);
	}

	/**
	 * Sets this entity's model rotation angles
	 */
	public void setupAnim(T entityIn, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
		super.setAngles(entityIn, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
		ItemStack itemstack = entityIn.getMainHandStack();
		if (entityIn.isAttacking() && (itemstack.isEmpty() || itemstack.getItem() != Items.BOW)) {
			float f = MathHelper.sin(this.handSwingProgress * (float)Math.PI);
			float f1 = MathHelper.sin((1.0F - (1.0F - this.handSwingProgress) * (1.0F - this.handSwingProgress)) * (float)Math.PI);
			this.rightArm.roll = 0.0F;
			this.leftArm.roll = 0.0F;
			this.rightArm.yaw = -(0.1F - f * 0.6F);
			this.leftArm.yaw = 0.1F - f * 0.6F;
			this.rightArm.pitch = (-(float)Math.PI / 2F);
			this.leftArm.pitch = (-(float)Math.PI / 2F);
			this.rightArm.pitch -= f * 1.2F - f1 * 0.4F;
			this.leftArm.pitch -= f * 1.2F - f1 * 0.4F;
			CrossbowPosing.swingArms(this.rightArm, this.leftArm, ageInTicks);
		}

	}

	public void setArmAngle(Arm sideIn, MatrixStack matrixStackIn) {
		float f = sideIn == Arm.RIGHT ? 1.0F : -1.0F;
		ModelPart modelrenderer = this.getArm(sideIn);
		modelrenderer.pivotX += f;
		modelrenderer.rotate(matrixStackIn);
		modelrenderer.pivotX -= f;
	}
}
