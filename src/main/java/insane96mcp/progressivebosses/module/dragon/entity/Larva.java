package insane96mcp.progressivebosses.module.dragon.entity;

import org.jetbrains.annotations.NotNull;

import insane96mcp.progressivebosses.module.Modules;
import insane96mcp.progressivebosses.module.dragon.ai.PBNearestAttackableTargetGoal;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityGroup;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.LookAroundGoal;
import net.minecraft.entity.ai.goal.LookAtEntityGoal;
import net.minecraft.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.entity.ai.goal.SwimGoal;
import net.minecraft.entity.ai.goal.WanderAroundFarGoal;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.boss.dragon.EnderDragonEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class Larva extends HostileEntity {
	public Larva(EntityType<? extends Larva> p_32591_, World p_32592_) {
		super(p_32591_, p_32592_);
		this.experiencePoints = 3;
	}

	protected void initGoals() {
		this.goalSelector.add(1, new SwimGoal(this));
		this.goalSelector.add(2, new MeleeAttackGoal(this, 1.0D, false));
		this.goalSelector.add(3, new WanderAroundFarGoal(this, 1.0D));
		this.goalSelector.add(7, new LookAtEntityGoal(this, PlayerEntity.class, 8.0F));
		this.goalSelector.add(8, new LookAroundGoal(this));
		this.targetSelector.add(1, new PBNearestAttackableTargetGoal(this));
	}

	protected float getActiveEyeHeight(EntityPose p_32604_, EntityDimensions p_32605_) {
		return 0.13F;
	}

	protected Entity.MoveEffect getMoveEffect() {
		return Entity.MoveEffect.EVENTS;
	}

	protected SoundEvent getAmbientSound() {
		return SoundEvents.ENTITY_ENDERMITE_AMBIENT;
	}

	protected SoundEvent getHurtSound(DamageSource p_32615_) {
		return SoundEvents.ENTITY_ENDERMITE_HURT;
	}

	protected SoundEvent getDeathSound() {
		return SoundEvents.ENTITY_ENDERMITE_DEATH;
	}

	protected void playStepSound(BlockPos p_32607_, BlockState p_32608_) {
		this.playSound(SoundEvents.ENTITY_ENDERMITE_STEP, 0.15F, 1.0F);
	}

	public void tick() {
		this.bodyYaw = this.getYaw();
		super.tick();
	}

	public void setBodyYaw(float p_32621_) {
		this.setYaw(p_32621_);
		super.setBodyYaw(p_32621_);
	}

	public double getHeightOffset() {
		return 0.1D;
	}

	@Override
	public boolean damage(DamageSource damageSource, float amount) {
		if (Modules.dragon.larva.isEnabled() && Modules.dragon.larva.reducedDragonDamage && damageSource.getAttacker() instanceof EnderDragonEntity)
			return super.damage(damageSource, amount * 0.1f);
		return super.damage(damageSource, amount);
	}

	public @NotNull EntityGroup getGroup() {
		return EntityGroup.ARTHROPOD;
	}

	public static DefaultAttributeContainer.Builder prepareAttributes() {
		return LivingEntity.createLivingAttributes()
				.add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 2.0d)
				.add(EntityAttributes.GENERIC_MAX_HEALTH, 6.0d)
				.add(EntityAttributes.GENERIC_FOLLOW_RANGE, 64.0d)
				.add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.44d)
				.add(EntityAttributes.GENERIC_ATTACK_KNOCKBACK, 1.5d);
	}
}
