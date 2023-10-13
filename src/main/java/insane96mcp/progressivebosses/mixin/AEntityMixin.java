package insane96mcp.progressivebosses.mixin;

import insane96mcp.progressivebosses.utils.IEntityExtraData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public abstract class AEntityMixin implements IEntityExtraData {
    private CompoundTag persistentData;

    @Inject(at = @At("RETURN"), method = "writeNbt", cancellable = true)
    public void writeNbt(CompoundTag nbt, CallbackInfoReturnable<CompoundTag> cir) {
        if (persistentData != null)
            nbt.put("persistentData", persistentData.copy());
        cir.setReturnValue(nbt);
    }

    @Inject(at = @At("RETURN"), method = "readNbt")
    public void readNbt(CompoundTag nbt, CallbackInfo ci) {
        if (nbt.contains("persistentData", 10))
            persistentData = nbt.getCompound("persistentData");
    }

    @Override
    public CompoundTag getPersistentData() {
        if (persistentData == null)
            persistentData = new CompoundTag();
        return persistentData;
    }
}