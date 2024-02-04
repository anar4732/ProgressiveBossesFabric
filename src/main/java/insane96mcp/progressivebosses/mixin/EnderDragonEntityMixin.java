package insane96mcp.progressivebosses.mixin;

import insane96mcp.progressivebosses.module.dragon.phase.CrystalRespawnPhase;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.boss.dragon.EnderDragonEntity;
import net.minecraft.entity.boss.dragon.EnderDragonPart;
import net.minecraft.entity.boss.dragon.phase.PhaseType;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EnderDragonEntity.class)
public class EnderDragonEntityMixin extends MobEntity {
	protected EnderDragonEntityMixin(EntityType<? extends MobEntity> type, World worldIn) {
		super(type, worldIn);
	}

	@Inject(at = @At("HEAD"), method = "damage")
	private void hurt(DamageSource source, float amount, CallbackInfoReturnable<Boolean> callback) {
		EnderDragonEntity $this = (EnderDragonEntity) (Object) this;
		if (source instanceof DamageSource && !source.isOf(DamageTypes.THORNS)) {
			$this.damagePart($this.getBodyParts()[2], source, amount);
		}
	}

	@Inject(at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/boss/dragon/EnderDragonEntity;parentDamage(Lnet/minecraft/entity/damage/DamageSource;F)Z", shift = At.Shift.AFTER), method = "damagePart")
	private void onReallyHurt(EnderDragonPart part, DamageSource damageSource, float amount, CallbackInfoReturnable<Boolean> callbackInfo) {
		EnderDragonEntity $this = (EnderDragonEntity) (Object) this;
		if (this.isDead() && $this.getPhaseManager().getCurrent().getType().equals(CrystalRespawnPhase.getPhaseType())) {
			$this.setHealth(1.0F);
			$this.getPhaseManager().setPhase(PhaseType.DYING);
		}
	}
}
