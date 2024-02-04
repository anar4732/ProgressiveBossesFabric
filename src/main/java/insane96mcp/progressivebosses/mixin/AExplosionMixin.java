package insane96mcp.progressivebosses.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import insane96mcp.progressivebosses.utils.ExplosionEvents;
import insane96mcp.progressivebosses.utils.ExplosionEvents.OnExplosionEvent;
import net.minecraft.world.explosion.Explosion;

@Mixin(Explosion.class)
public class AExplosionMixin {
    @Inject(at = @At("HEAD"), method = "affectWorld", cancellable = true)
    public void affectWorld(boolean particles, CallbackInfo ci) {
        OnExplosionEvent event = new OnExplosionEvent((Explosion) (Object) this);
        ExplosionEvents.EXPLODE.invoker().interact(event);
        if (event.isCancelled()) {
            ci.cancel();
        }
    }
}