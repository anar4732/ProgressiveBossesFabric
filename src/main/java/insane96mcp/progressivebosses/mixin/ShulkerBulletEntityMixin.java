package insane96mcp.progressivebosses.mixin;

import insane96mcp.progressivebosses.module.Modules;
import insane96mcp.progressivebosses.utils.IEntityExtraData;
import insane96mcp.progressivebosses.utils.Strings;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.entity.projectile.ShulkerBulletEntity;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ShulkerBulletEntity.class)
public abstract class ShulkerBulletEntityMixin extends ProjectileEntity {
	public ShulkerBulletEntityMixin(EntityType<? extends ProjectileEntity> entityType, World world) {
		super(entityType, world);
		this.noClip = true;
	}

	@ModifyArg(method = "onEntityHit", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/LivingEntity;addStatusEffect(Lnet/minecraft/entity/effect/StatusEffectInstance;Lnet/minecraft/entity/Entity;)Z"), index = 0)
	private StatusEffectInstance applyBlindness(StatusEffectInstance mobEffectInstance) {
		if (((IEntityExtraData) this).getPersistentData().getBoolean(Strings.Tags.BLINDNESS_BULLET))
			return new StatusEffectInstance(StatusEffects.BLINDNESS, Modules.dragon.minion.blindingDuration);
		else
			return mobEffectInstance;
	}

	/*@Redirect(at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;addEffect(Lnet/minecraft/world/effect/MobEffectInstance;Lnet/minecraft/world/entity/Entity;)Z"), method = "onHitEntity(Lnet/minecraft/world/phys/EntityHitResult;)V")
	private void onEntityHit(MobEffectInstance instance, Entity entity) {
		//ShulkerBullet $this = (ShulkerBullet) (Object) this;
		//if ($this.getPersistentData().getBoolean(Strings.Tags.BLINDNESS_BULLET))
		//	entity.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 150));
	}*/

	@Inject(at = @At("HEAD"), method = "tick()V")
	public void tick(CallbackInfo callback) {
		Modules.dragon.minion.onBulletTick((ShulkerBulletEntity) (Object) this);
	}
}
