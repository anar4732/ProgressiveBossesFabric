package insane96mcp.progressivebosses.module.dragon.entity;

import java.util.List;

import com.google.common.collect.Lists;

import insane96mcp.progressivebosses.ProgressiveBosses;
import insane96mcp.progressivebosses.utils.RandomHelper;
import net.minecraft.entity.AreaEffectCloudEntity;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;

public class AreaEffectCloud3DEntity extends AreaEffectCloudEntity {
	public AreaEffectCloud3DEntity(EntityType<? extends AreaEffectCloud3DEntity> cloud, World world) {
		super(cloud, world);
	}

	public AreaEffectCloud3DEntity(World worldIn, double x, double y, double z) {
		this(ProgressiveBosses.AREA_EFFECT_CLOUD_3D, worldIn);
		this.setPos(x, y, z);
	}

	public AreaEffectCloud3DEntity(AreaEffectCloudEntity areaEffectCloudEntity) {
		this(ProgressiveBosses.AREA_EFFECT_CLOUD_3D, areaEffectCloudEntity.getWorld());
		this.setPos(areaEffectCloudEntity.getX(), areaEffectCloudEntity.getY(), areaEffectCloudEntity.getZ());
		NbtCompound nbt = new NbtCompound();
		areaEffectCloudEntity.saveSelfNbt(nbt);
		this.readNbt(nbt);
	}

	@Override
	public void calculateDimensions() {
		super.calculateDimensions();
		double radius = (double)this.getDimensions(EntityPose.STANDING).width / 2.0D;
		this.setBoundingBox(new Box(this.getX() - radius, this.getY() - radius, this.getZ() - radius, this.getX() + radius, this.getY() + radius, this.getZ() + radius));
	}

	@Override
	public void tick() {
		boolean isWaiting = this.isWaiting();
		float radius = this.getRadius();
		if (this.getWorld().isClient) {
			ParticleEffect particleOptions = this.getParticleType();
			if (isWaiting) {
				if (this.random.nextBoolean()) {
					for (int i = 0; i < radius; ++i) {
						float f1 = this.random.nextFloat() * ((float)Math.PI * 2F);
						float f2 = MathHelper.sqrt(this.random.nextFloat()) * 0.2F;
						float x = MathHelper.cos(f1) * f2;
						float z = MathHelper.sin(f1) * f2;
						if (particleOptions.getType() == ParticleTypes.ENTITY_EFFECT) {
							int j = this.random.nextBoolean() ? 16777215 : this.getColor();
							int k = j >> 16 & 255;
							int l = j >> 8 & 255;
							int i1 = j & 255;
							this.getWorld().addParticle(particleOptions, this.getX() + (double)x, this.getY(), this.getZ() + (double)z, (double)((float)k / 255.0F), (double)((float)l / 255.0F), (double)((float)i1 / 255.0F));
						}
						else {
							this.getWorld().addParticle(particleOptions, this.getX() + (double)x, this.getY(), this.getZ() + (double)z, 0.0D, 0.0D, 0.0D);
						}
					}
				}
			}
			else {
				int particleAmount = (int) (Math.PI * radius * radius);

				for (int k1 = 0; k1 < particleAmount; ++k1) {
					float f6 = this.random.nextFloat() * ((float)Math.PI * 2F);
					float f7 = MathHelper.sqrt(this.random.nextFloat()) * radius;
					float x = RandomHelper.getFloat(this.random, -radius, radius);
					float y = RandomHelper.getFloat(this.random, -radius, radius);
					float z = RandomHelper.getFloat(this.random, -radius, radius);
					if ((x*x) + (y*y) + (z*z) > (radius*radius))
						continue;

					if (particleOptions.getType() == ParticleTypes.ENTITY_EFFECT) {
						int l1 = this.getColor();
						int i2 = l1 >> 16 & 255;
						int j2 = l1 >> 8 & 255;
						int j1 = l1 & 255;
						this.getWorld().addParticle(particleOptions, this.getX() + (double)x, this.getY() + (double)y, this.getZ() + (double)z, (float)i2 / 255.0F, (float)j2 / 255.0F, (float)j1 / 255.0F);
					} else {
						this.getWorld().addParticle(particleOptions, this.getX() + (double)x, this.getY() + (double)y, this.getZ() + (double)z, (0.5D - this.random.nextDouble()) * 0.15D, (double)0.01F, (0.5D - this.random.nextDouble()) * 0.15D);
					}
				}
			}
		}
		else {
			if (this.age >= this.waitTime + this.duration) {
				this.discard();
				return;
			}

			boolean flag1 = this.age < this.waitTime;
			if (isWaiting != flag1) {
				this.setWaiting(flag1);
			}

			if (flag1) {
				return;
			}

			if (this.radiusGrowth != 0.0F) {
				radius += this.radiusGrowth;
				if (radius < 0.5F) {
					this.discard();
					return;
				}

				this.setRadius(radius);
			}

			if (this.age % 5 == 0) {
				this.affectedEntities.entrySet().removeIf(entry -> this.age >= entry.getValue());

				List<StatusEffectInstance> list = Lists.newArrayList();

				for(StatusEffectInstance effectinstance1 : this.potion.getEffects()) {
					list.add(new StatusEffectInstance(effectinstance1.getEffectType(), effectinstance1.getDuration() / 4, effectinstance1.getAmplifier(), effectinstance1.isAmbient(), effectinstance1.shouldShowParticles()));
				}

				list.addAll(this.effects);
				if (list.isEmpty()) {
					this.affectedEntities.clear();
				} else {
					List<LivingEntity> list1 = this.getWorld().getNonSpectatingEntities(LivingEntity.class, this.getBoundingBox());
					if (!list1.isEmpty()) {
						for(LivingEntity livingentity : list1) {
							if (!this.affectedEntities.containsKey(livingentity) && livingentity.isAffectedBySplashPotions()) {
								this.affectedEntities.put(livingentity, this.age + this.reapplicationDelay);
								double x = livingentity.getX() - this.getX();
								double y = livingentity.getY() + (livingentity.getDimensions(livingentity.getPose()).height / 2) - (this.getY());
								double z = livingentity.getZ() - this.getZ();
								double d2 = x * x + y * y + z * z;
								if (d2 <= (double)(radius * radius)) {
									for (StatusEffectInstance effectinstance : list) {
										if (effectinstance.getEffectType().isInstant()) {
											effectinstance.getEffectType().applyInstantEffect(this, this.getOwner(), livingentity, effectinstance.getAmplifier(), 0.5D);
										}
										else {
											livingentity.addStatusEffect(new StatusEffectInstance(effectinstance));
										}
									}
									if (this.radiusOnUse != 0.0F) {
										radius += this.radiusOnUse;
										if (radius < 0.5F) {
											this.discard();
											return;
										}
										this.setRadius(radius);
									}
									if (this.durationOnUse != 0) {
										this.duration += this.durationOnUse;
										if (this.duration <= 0) {
											this.discard();
											return;
										}
									}
								}
							}
						}
					}
				}
			}
		}

	}

	@Override
	public EntityDimensions getDimensions(EntityPose poseIn) {
		return EntityDimensions.changing(this.getRadius() * 2.0F, this.getRadius() * 2.0F);
	}

	// @Override
	// public Packet<?> createSpawnPacket() {
	// 	return NetworkHooks.getEntitySpawningPacket(this);
	// }
}
