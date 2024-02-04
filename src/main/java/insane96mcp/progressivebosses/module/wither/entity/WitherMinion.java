package insane96mcp.progressivebosses.module.wither.entity;

import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;


import insane96mcp.progressivebosses.module.wither.ai.minion.MinionNearestAttackableTargetGoal;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityData;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityGroup;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.ai.goal.ActiveTargetGoal;
import net.minecraft.entity.ai.goal.LookAroundGoal;
import net.minecraft.entity.ai.goal.LookAtEntityGoal;
import net.minecraft.entity.ai.goal.RevengeGoal;
import net.minecraft.entity.ai.goal.SwimGoal;
import net.minecraft.entity.ai.goal.WanderAroundFarGoal;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.boss.WitherEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.AbstractSkeletonEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ArrowEntity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.potion.PotionUtil;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.world.LocalDifficulty;
import net.minecraft.world.ServerWorldAccess;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

public class WitherMinion extends AbstractSkeletonEntity {

	private static final Predicate<LivingEntity> NOT_UNDEAD = livingEntity -> livingEntity != null && livingEntity.getGroup() != EntityGroup.UNDEAD && livingEntity.isMobOrPlayer();

	public WitherMinion(EntityType<? extends AbstractSkeletonEntity> type, World worldIn) {
		super(type, worldIn);
	}

	@Override
	public SoundEvent getStepSound() {
		return SoundEvents.ENTITY_WITHER_SKELETON_STEP;
	}

	@Override
	protected void initGoals() {
		this.goalSelector.add(1, new SwimGoal(this));
		this.goalSelector.add(5, new WanderAroundFarGoal(this, 1.0D));
		this.goalSelector.add(6, new LookAtEntityGoal(this, PlayerEntity.class, 8.0F));
		this.goalSelector.add(6, new LookAroundGoal(this));

		this.targetSelector.add(1, new RevengeGoal(this, WitherEntity.class, WitherMinion.class));
		this.targetSelector.add(2, new MinionNearestAttackableTargetGoal(this, PlayerEntity.class, 0, false, false, null));
		this.targetSelector.add(3, new ActiveTargetGoal<>(this, MobEntity.class, 0, false, false, NOT_UNDEAD));
	}

	protected SoundEvent getAmbientSound() {
		return SoundEvents.ENTITY_WITHER_SKELETON_AMBIENT;
	}

	protected SoundEvent getHurtSound(DamageSource damageSourceIn) {
		return SoundEvents.ENTITY_WITHER_SKELETON_HURT;
	}

	protected SoundEvent getDeathSound() {
		return SoundEvents.ENTITY_WITHER_SKELETON_DEATH;
	}

	protected float getActiveEyeHeight(EntityPose poseIn, EntityDimensions sizeIn) {
		return 1.3F;
	}

	/**
	 * Gets the pitch of living sounds in living entities.
	 */
	public float getSoundPitch() {
		return (this.random.nextFloat() - this.random.nextFloat()) * 0.2F + 1.33F;
	}

	/**
	 * Gives armor or weapon for entity based on given DifficultyInstance
	 */
	protected void initEquipment(LocalDifficulty difficulty) {
		this.equipStack(EquipmentSlot.MAINHAND, new ItemStack(Items.BOW));
	}

	@Nullable
	public EntityData initialize(ServerWorldAccess worldIn, LocalDifficulty difficultyIn, SpawnReason reason, @Nullable EntityData spawnDataIn, @Nullable NbtCompound dataTag) {
		EntityData ilivingentitydata = super.initialize(worldIn, difficultyIn, reason, spawnDataIn, dataTag);
		this.getAttributeInstance(EntityAttributes.GENERIC_ATTACK_DAMAGE).setBaseValue(4.0D);
		this.updateAttackType();
		return ilivingentitydata;
	}

	public boolean tryAttack(Entity entityIn) {
		if (!super.tryAttack(entityIn)) {
			return false;
		} else {
			if (entityIn instanceof LivingEntity) {
				((LivingEntity)entityIn).addStatusEffect(new StatusEffectInstance(StatusEffects.WITHER, 200));
			}

			return true;
		}
	}

	public boolean damage(DamageSource source, float amount) {
		if (source.getAttacker() instanceof WitherMinion)
			amount *= 0.2f;
		return !this.isInvulnerableTo(source) && super.damage(source, amount);
	}

	public boolean canHaveStatusEffect(StatusEffectInstance potioneffectIn) {
		return potioneffectIn.getEffectType() != StatusEffects.WITHER && super.canHaveStatusEffect(potioneffectIn);
	}

	private static final List<StatusEffectInstance> ARROW_EFFECTS = Arrays.asList(new StatusEffectInstance(StatusEffects.WITHER, 200));

	/**
	 * Fires an arrow
	 */
	protected PersistentProjectileEntity createArrowProjectile(ItemStack arrowStack, float distanceFactor) {
		PersistentProjectileEntity abstractarrowentity = super.createArrowProjectile(arrowStack, distanceFactor);
		if (abstractarrowentity instanceof ArrowEntity) {
			ItemStack witherArrow = new ItemStack(Items.TIPPED_ARROW, 1);
			PotionUtil.setCustomPotionEffects(witherArrow, ARROW_EFFECTS);
			((ArrowEntity)abstractarrowentity).initFromStack(witherArrow);
		}
		return abstractarrowentity;
	}

	//Do not generate Wither Roses
	protected void onKilledBy(@Nullable LivingEntity entitySource) {
	}

	public static DefaultAttributeContainer.Builder prepareAttributes() {
		return LivingEntity.createLivingAttributes()
				.add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 3.0d)
				.add(EntityAttributes.GENERIC_MAX_HEALTH, 20.0d)
				.add(EntityAttributes.GENERIC_FOLLOW_RANGE, 40.0d)
				.add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.25d)
				.add(EntityAttributes.GENERIC_ATTACK_KNOCKBACK, 1.5d);
	}
}
