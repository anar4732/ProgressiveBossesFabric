package insane96mcp.progressivebosses.mixin;

import insane96mcp.progressivebosses.module.Modules;
import net.minecraft.entity.ai.pathing.Path;
import net.minecraft.entity.boss.dragon.phase.HoldingPatternPhase;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HoldingPatternPhase.class)
public class HoldingPatternPhaseMixin {

	@Shadow
	public Path path;

	@Inject(at = @At("HEAD"), method = "tickInRange()V", cancellable = true)
	private void findNewTarget(CallbackInfo callback) {
		if (this.path == null || !this.path.isFinished())
			return;

		if (Modules.dragon.attack.onPhaseEnd(((HoldingPatternPhase)(Object)this).dragon))
			callback.cancel();
	}
}
