package insane96mcp.progressivebosses.module.dragon.feature;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.registry.Registries;
import net.minecraft.registry.entry.RegistryEntry;
import org.jetbrains.annotations.Nullable;

import insane96mcp.progressivebosses.module.Modules;
import insane96mcp.progressivebosses.module.dragon.entity.AreaEffectCloud3DEntity;
import insane96mcp.progressivebosses.utils.DummyEvent;
import insane96mcp.progressivebosses.utils.IEntityExtraData;
import insane96mcp.progressivebosses.utils.Label;
import insane96mcp.progressivebosses.utils.LabelConfigGroup;
import insane96mcp.progressivebosses.utils.LivingEntityEvents;
import insane96mcp.progressivebosses.utils.LivingEntityEvents.OnLivingHurtEvent;
import insane96mcp.progressivebosses.utils.RandomHelper;
import insane96mcp.progressivebosses.utils.Strings;
import insane96mcp.progressivebosses.utils.Utils;
import me.lortseam.completeconfig.api.ConfigEntries;
import me.lortseam.completeconfig.api.ConfigEntry;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.minecraft.entity.AreaEffectCloudEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.boss.dragon.EnderDragonEntity;
import net.minecraft.entity.boss.dragon.phase.PhaseType;
import net.minecraft.entity.damage.DamageSource;;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.DragonFireballEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.predicate.entity.EntityPredicates;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.Heightmap;
import net.minecraft.world.World;
import net.minecraft.world.gen.feature.EndPortalFeature;

@ConfigEntries(includeAll = true)
@Label(name = "Attack", description = "Makes the dragon hit harder in various different ways")
public class AttackFeature implements LabelConfigGroup {

	@ConfigEntry(nameKey = "Bonus Direct Damage", comment = "How much more damage per difficulty (percentage) does the Ender Dragon (directly) deal per difficulty?")
	@ConfigEntry.BoundedDouble(min = 0d, max = Double.MAX_VALUE)
	public double increasedDirectDamage = 0.3d;

	@ConfigEntry(nameKey = "Bonus Acid Pool Damage", comment = "How much more damage per difficulty (percentage) does the Ender Dragon's Acid Pool deal per difficulty?")
	@ConfigEntry.BoundedDouble(min = 0d, max = Double.MAX_VALUE)
	public double increasedAcidPoolDamage = 0.3d;

	@ConfigEntry(nameKey = "Charge Player Max Chance", comment = """
						Normally the Ender Dragon attacks only when leaving the center platform. With this active she has a chance when she has finished charging / fireballing or before checking if she should land in the center to charge the player.
						This is the chance to start a charge attack when the difficulty is at max. Otherwise it scales accordingly.
						The actual chance is: (this_value * (difficulty / max difficulty)).""")
	@ConfigEntry.BoundedDouble(min = 0d, max = Double.MAX_VALUE)
	public double chargePlayerMaxChance = 0.45d; //Chance at max difficulty

	@ConfigEntry(nameKey = "Fireball Max Chance", comment = """
						Normally the Ender Dragon spits fireballs when a Crystal is destroyed and rarely during the fight. With this active she has a chance when she has finished charging / fireballing or before checking if she should land in the center to spit a fireball.
						This is the chance to start a fireball attack when the difficulty is at max. Otherwise it scales accordingly.
						The actual chance is: (this_value * (difficulty / max difficulty)).""")
	@ConfigEntry.BoundedDouble(min = 0d, max = Double.MAX_VALUE)
	public double fireballMaxChance = 0.35d; //Chance at max difficulty

	@ConfigEntry(nameKey = "Increase Max Rise and Fall", comment = "Since around 1.13/1.14 the Ender Dragon can no longer dive for more than about 3 blocks so she takes a lot to rise / fall. With this active the dragon will be able to rise and fall many more blocks, making easier to hit the player and approach the center.")
	public boolean increaseMaxRiseAndFall = true;

	@ConfigEntry(nameKey = "Fireball Explosion Magic Damage", comment = "On impact the Acid Fireball will deal magic damage in an area.")
	public boolean fireballExplosionDamages = true;

	@ConfigEntry(nameKey = "Fireball 3D Area Effect Cloud", comment = "On impact the Acid Fireball will generate a 3D area of effect cloud instead of a normal flat one. The 3D cloud lasts for half the time")
	public boolean fireball3DEffectCloud = true;

	@ConfigEntry(nameKey = "Fireball Velocity Multiplier", comment = "Speed multiplier for the Dragon Fireball.")
	@ConfigEntry.BoundedDouble(min = 0d, max = Double.MAX_VALUE)
	public double fireballVelocityMultiplier = 2.5d;

	@ConfigEntry(nameKey = "Bonus Fireballs", comment = "The dragon will fire (up to) this more fireballs per difficulty. A decimal number dictates the chance to shot 1 more fireball, e.g. at difficulty 2 this value is 1.4, meaning that the dragon will can shot up to 2 fireballs and has 40% chance to shot up to 3. The bonus fireballs aren't directly aimed at the player but have.")
	@ConfigEntry.BoundedDouble(min = 0d, max = Double.MAX_VALUE)
	public double maxBonusFireball = 2d;

	public AttackFeature(LabelConfigGroup parent) {
		parent.addConfigContainer(this);
		ServerEntityEvents.ENTITY_LOAD.register((entity, world) -> this.onSpawn(new DummyEvent(world, entity)));
		LivingEntityEvents.HURT.register((event) -> this.onDamageDealt(event));
	}

	public void onSpawn(DummyEvent event) {
		fireballSpeed(event.getEntity());
	}

	private void fireballSpeed(Entity entity) {
		if (!(entity instanceof DragonFireballEntity fireball))
			return;

		if (!this.isEnabled() || this.fireballVelocityMultiplier == 0d)
			return;

		if (Math.abs(fireball.powerX) > 10 || Math.abs(fireball.powerY) > 10 || Math.abs(fireball.powerZ) > 10) {
			entity.kill();
			return;
		}

		fireball.powerX *= this.fireballVelocityMultiplier;
		fireball.powerY *= this.fireballVelocityMultiplier;
		fireball.powerZ *= this.fireballVelocityMultiplier;
	}

	public void onDamageDealt(OnLivingHurtEvent event) {
		if (event.getEntity().getWorld().isClient)
			return;

		onDirectDamage(event);
		onAcidDamage(event);
	}

	private void onDirectDamage(OnLivingHurtEvent event) {
		if (!(event.getSource().getSource() instanceof EnderDragonEntity dragon) || event.getEntity() instanceof EnderDragonEntity)
			return;

		NbtCompound compoundNBT = ((IEntityExtraData) dragon).getPersistentData();
		float difficulty = compoundNBT.getFloat(Strings.Tags.DIFFICULTY);

		if (difficulty == 0f)
			return;

		event.setAmount(event.getAmount() * (float)(1d + (this.increasedDirectDamage * difficulty)));
	}

	private void onAcidDamage(OnLivingHurtEvent event) {
		if (!(event.getSource().getSource() instanceof EnderDragonEntity dragon) || !(event.getSource().getAttacker() instanceof AreaEffectCloudEntity))
			return;

		NbtCompound compoundNBT = ((IEntityExtraData) dragon).getPersistentData();
		float difficulty = compoundNBT.getFloat(Strings.Tags.DIFFICULTY);

		if (difficulty == 0f)
			return;

		event.setAmount(event.getAmount() * (float)(1d + (this.increasedAcidPoolDamage * difficulty)));
	}

	public boolean onPhaseEnd(EnderDragonEntity dragon) {
		boolean chargePlayer = shouldChargePlayer(dragon);
		boolean fireballPlayer = shouldFireballPlayer(dragon);

		if (chargePlayer && fireballPlayer)
			if (dragon.getRandom().nextFloat() < 0.5f)
				chargePlayer(dragon);
			else
				fireballPlayer(dragon);
		else if (chargePlayer)
			chargePlayer(dragon);
		else if (fireballPlayer)
			fireballPlayer(dragon);

		return chargePlayer || fireballPlayer;
	}

	private boolean shouldChargePlayer(EnderDragonEntity dragon) {
		if (this.chargePlayerMaxChance == 0f)
			return false;

		if (dragon.getFight() == null)
			return false;

		NbtCompound tags = ((IEntityExtraData) dragon).getPersistentData();
		float difficulty = tags.getFloat(Strings.Tags.DIFFICULTY);

		double chance = this.chargePlayerMaxChance * (difficulty / Modules.dragon.difficulty.maxDifficulty);

		BlockPos centerPodium = dragon.getWorld().getTopPosition(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, EndPortalFeature.offsetOrigin(BlockPos.ORIGIN));
		Box boundingBox = new Box(centerPodium).expand(64d);
		List<PlayerEntity> players = dragon.getWorld().getEntitiesByClass(PlayerEntity.class, boundingBox, EntityPredicates.EXCEPT_CREATIVE_OR_SPECTATOR);

		for (PlayerEntity player : players) {
			List<EndCrystalEntity> endCrystals = player.getWorld().getNonSpectatingEntities(EndCrystalEntity.class, player.getBoundingBox().expand(10d));
			if (endCrystals.size() > 0) {
				chance *= 2d;
				break;
			}
		}

		double rng = dragon.getRandom().nextDouble();

		return rng < chance;
	}

	private void chargePlayer(EnderDragonEntity dragon) {
		BlockPos centerPodium = dragon.getWorld().getTopPosition(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, EndPortalFeature.offsetOrigin(BlockPos.ORIGIN));
		Box bb = new Box(centerPodium).expand(64d);
		ServerPlayerEntity player = (ServerPlayerEntity) getRandomPlayerNearCrystal(dragon.getWorld(), bb);

		if (player == null)
			return;

		dragon.getPhaseManager().setPhase(PhaseType.CHARGING_PLAYER);
		Vec3d targetPos = player.getPos();
		if (targetPos.y < dragon.getY())
			targetPos = targetPos.add(0d, -5d, 0d);
		else
			targetPos = targetPos.add(0d, 6d, 0d);
		dragon.getPhaseManager().create(PhaseType.CHARGING_PLAYER).setPathTarget(targetPos);
	}

	private boolean shouldFireballPlayer(EnderDragonEntity dragon) {
		if (this.fireballMaxChance == 0f)
			return false;

		if (dragon.getFight() == null)
			return false;

		NbtCompound tags = ((IEntityExtraData) dragon).getPersistentData();
		float difficulty = tags.getFloat(Strings.Tags.DIFFICULTY);

		if (difficulty == 0f)
			return false;

		double chance = this.fireballMaxChance * (difficulty / Modules.dragon.difficulty.maxDifficulty);

		double rng = dragon.getRandom().nextDouble();

		return rng < chance;
	}

	private void fireballPlayer(EnderDragonEntity dragon) {
		BlockPos centerPodium = dragon.getWorld().getTopPosition(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, EndPortalFeature.offsetOrigin(BlockPos.ORIGIN));
		Box bb = new Box(centerPodium).expand(64d);
		ServerPlayerEntity player = (ServerPlayerEntity) getRandomPlayer(dragon.getWorld(), bb);

		if (player == null)
			return;

		dragon.getPhaseManager().setPhase(PhaseType.STRAFE_PLAYER);
		dragon.getPhaseManager().create(PhaseType.STRAFE_PLAYER).setTargetEntity(player);
	}


	public boolean onFireballImpact(DragonFireballEntity fireball, @Nullable Entity shooter, HitResult result) {
		if (!this.isEnabled())
			return false;

		onImpactExplosion(fireball, shooter, result);
		return onImpact3DCloud(fireball, result);
	}

	private void onImpactExplosion(DragonFireballEntity fireball, @Nullable Entity shooter, HitResult result) {
		if (!this.fireballExplosionDamages)
			return;

		float difficulty = 0;
		if (shooter != null) {
			NbtCompound compoundNBT = ((IEntityExtraData) shooter).getPersistentData();
			difficulty = compoundNBT.getFloat(Strings.Tags.DIFFICULTY);
		}

		float damage = 6 * (1f + (float) (this.increasedAcidPoolDamage * difficulty));

		Box axisAlignedBB = new Box(result.getPos(), result.getPos()).expand(4d);
		List<LivingEntity> livingEntities = fireball.getWorld().getNonSpectatingEntities(LivingEntity.class, axisAlignedBB);
		for (LivingEntity livingEntity : livingEntities) {
			if (livingEntity.squaredDistanceTo(fireball.getPos()) < 20.25d)
			{
				//(new DamageSource(Strings.Translatable.DRAGON_FIREBALL, fireball, shooter)).setBypassesArmor().setProjectile().setUsesMagic(), damage
				var damageType = RegistryEntry.of(livingEntity.getWorld().getDamageSources().registry.get(DamageTypes.DRAGON_BREATH));
				var damageSource = new DamageSource(damageType,fireball,shooter);
				livingEntity.damage(damageSource,damage);
			}
		}
	}

	private boolean onImpact3DCloud(DragonFireballEntity fireball, HitResult result) {
		if (!this.isEnabled())
			return false;

		if (!this.fireball3DEffectCloud)
			return false;

		HitResult.Type raytraceresult$type = result.getType();
		if (raytraceresult$type == HitResult.Type.ENTITY) {
			fireball.onEntityHit((EntityHitResult)result);
		}
		else if (raytraceresult$type == HitResult.Type.BLOCK) {
			fireball.onBlockHit((BlockHitResult)result);
		}
		Entity entity = fireball.getOwner();
		if (result.getType() != HitResult.Type.ENTITY || !((EntityHitResult)result).getEntity().isPartOf(entity)) {
			if (!fireball.getWorld().isClient) {
				List<LivingEntity> list = fireball.getWorld().getNonSpectatingEntities(LivingEntity.class, fireball.getBoundingBox().expand(4.0D, 2.0D, 4.0D));
				AreaEffectCloud3DEntity areaeffectcloudentity = new AreaEffectCloud3DEntity(fireball.getWorld(), fireball.getX(), fireball.getY(), fireball.getZ());
				if (entity instanceof LivingEntity) {
					areaeffectcloudentity.setOwner((LivingEntity)entity);
				}

				areaeffectcloudentity.setParticleType(ParticleTypes.DRAGON_BREATH);
				areaeffectcloudentity.setRadius(3.0F);
				areaeffectcloudentity.setDuration(300);
				areaeffectcloudentity.setWaitTime(10);
				areaeffectcloudentity.setRadiusGrowth((7.0F - areaeffectcloudentity.getRadius()) / (float)areaeffectcloudentity.getDuration());
				areaeffectcloudentity.addEffect(new StatusEffectInstance(StatusEffects.INSTANT_DAMAGE, 1, 1));
				if (!list.isEmpty()) {
					for(LivingEntity livingentity : list) {
						double d0 = fireball.squaredDistanceTo(livingentity);
						if (d0 < 16.0D) {
							areaeffectcloudentity.setPos(livingentity.getX(), livingentity.getY(), livingentity.getZ());
							break;
						}
					}
				}

				fireball.getWorld().syncWorldEvent(2006, fireball.getBlockPos(), fireball.isSilent() ? -1 : 1);
				fireball.getWorld().spawnEntity(areaeffectcloudentity);
				fireball.discard();
			}
		}

		return true;
	}

	public void fireFireball(EnderDragonEntity dragon, LivingEntity attackTarget) {
		Vec3d vector3d2 = dragon.getRotationVec(1.0F);
		double x = dragon.head.getX() - vector3d2.x;
		double y = dragon.head.getBodyY(0.5D) + 0.5D;
		double z = dragon.head.getZ() - vector3d2.z;
		double xPower = attackTarget.getX() - x;
		double yPower = attackTarget.getBodyY(0.5D) - y;
		double zPower = attackTarget.getZ() - z;
		if (!dragon.isSilent()) {
			dragon.getWorld().syncWorldEvent(null, 1017, dragon.getBlockPos(), 0);
		}

		DragonFireballEntity dragonfireballentity = new DragonFireballEntity(dragon.getWorld(), dragon, xPower, yPower, zPower);
		dragonfireballentity.refreshPositionAndAngles(x, y, z, 0.0F, 0.0F);
		dragon.getWorld().spawnEntity(dragonfireballentity);

		NbtCompound compoundNBT = ((IEntityExtraData) dragon).getPersistentData();
		float difficulty = compoundNBT.getFloat(Strings.Tags.DIFFICULTY);

		float fireballs = RandomHelper.getFloat(dragon.getRandom(), 0f, (float) (maxBonusFireball * difficulty));
		fireballs = Utils.getAmountWithDecimalChance(dragon.getRandom(), fireballs);
		if (fireballs == 0f)
			return;

		for (int i = 0; i < fireballs; i++) {
			x = dragon.head.getX() - vector3d2.x;
			y = dragon.head.getBodyY(0.5D) + 0.5D;
			z = dragon.head.getZ() - vector3d2.z;
			xPower = attackTarget.getX() + RandomHelper.getDouble(dragon.getRandom(), -(fireballs), fireballs) - x;
			yPower = attackTarget.getBodyY(0.5D) + RandomHelper.getDouble(dragon.getRandom(), -(fireballs), fireballs) - y;
			zPower = attackTarget.getZ() + RandomHelper.getDouble(dragon.getRandom(), -(fireballs), fireballs) - z;
			if (!dragon.isSilent()) {
				dragon.getWorld().syncWorldEvent(null, 1017, dragon.getBlockPos(), 0);
			}

			dragonfireballentity = new DragonFireballEntity(dragon.getWorld(), dragon, xPower, yPower, zPower);
			dragonfireballentity.refreshPositionAndAngles(x, y, z, 0.0F, 0.0F);
			dragon.getWorld().spawnEntity(dragonfireballentity);
		}
	}

	@Nullable
	public PlayerEntity getRandomPlayer(World world, Box boundingBox) {
		List<PlayerEntity> players = world.getEntitiesByClass(PlayerEntity.class, boundingBox, EntityPredicates.EXCEPT_CREATIVE_OR_SPECTATOR);
		if (players.isEmpty())
			return null;

		int r = RandomHelper.getInt(world.random, 0, players.size());
		return players.get(r);
	}

	//Returns a random player that is at least 10 blocks near a Crystal or a random player if no players are near crystals
	@Nullable
	public PlayerEntity getRandomPlayerNearCrystal(World world, Box boundingBox) {
		List<PlayerEntity> players = world.getNonSpectatingEntities(PlayerEntity.class, boundingBox);
		if (players.isEmpty())
			return null;

		List<PlayerEntity> playersNearCrystals = new ArrayList<>();

 		for (PlayerEntity player : players) {
			List<EndCrystalEntity> endCrystals = player.getWorld().getEntitiesByClass(EndCrystalEntity.class, player.getBoundingBox().expand(10d), EntityPredicates.EXCEPT_CREATIVE_OR_SPECTATOR);
			if (endCrystals.size() > 0)
				playersNearCrystals.add(player);
		}

 		int r;
 		if (playersNearCrystals.isEmpty()) {
			r = RandomHelper.getInt(world.random, 0, players.size());
			return players.get(r);
		}

		r = RandomHelper.getInt(world.random, 0, playersNearCrystals.size());
		return playersNearCrystals.get(r);
	}
}
