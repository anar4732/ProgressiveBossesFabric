package insane96mcp.progressivebosses.mixin;

import insane96mcp.progressivebosses.module.Modules;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.projectile.DragonFireballEntity;
import net.minecraft.entity.projectile.ExplosiveProjectileEntity;
import net.minecraft.util.hit.HitResult;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(DragonFireballEntity.class)
public class DragonFireballEntityMixin extends ExplosiveProjectileEntity {
	protected DragonFireballEntityMixin(EntityType<? extends ExplosiveProjectileEntity> p_i50173_1_, World p_i50173_2_) {
		super(p_i50173_1_, p_i50173_2_);
	}

	@Inject(at = @At("HEAD"), method = "onCollision", cancellable = true)
	private void onHit(HitResult result, CallbackInfo callback) {
		if (Modules.dragon.attack.onFireballImpact((DragonFireballEntity) (Object) this, this.getOwner(), result))
			callback.cancel();
	}
}
