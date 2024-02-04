package insane96mcp.progressivebosses.mixin;

import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.entity.boss.dragon.EnderDragonFight;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.world.explosion.Explosion;
import java.util.List;

@Mixin(EnderDragonFight.class)
public class EndDragonFightMixin {

	@Shadow @Final private ServerWorld world;

	@Shadow @Nullable private BlockPos exitPortalLocation;

	@Inject(at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/boss/dragon/EnderDragonFight;generateEndPortal(Z)V"), method = "respawnDragon(Ljava/util/List;)V")
	private void respawnDragon(List<EndCrystalEntity> p_64092_, CallbackInfo callback) {
		List<EndCrystalEntity> endCrystals = this.world.getEntitiesByClass(EndCrystalEntity.class, new Box(this.exitPortalLocation).expand(48d), EndCrystalEntity::shouldShowBottom);
		for (EndCrystalEntity endCrystal : endCrystals) {
			endCrystal.getWorld().createExplosion(endCrystal, endCrystal.getX(), endCrystal.getY(), endCrystal.getZ(), 6.0F, World.ExplosionSourceType.MOB);
			endCrystal.discard();
		}
	}
}