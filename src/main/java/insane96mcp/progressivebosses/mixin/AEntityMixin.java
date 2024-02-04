package insane96mcp.progressivebosses.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import insane96mcp.progressivebosses.utils.IEntityExtraData;
import net.minecraft.entity.Entity;
import net.minecraft.nbt.NbtCompound;

@Mixin(Entity.class)
public abstract class AEntityMixin implements IEntityExtraData {
    private NbtCompound persistentData;

    @Inject(at = @At("RETURN"), method = "writeNbt", cancellable = true)
    public void writeNbt(NbtCompound nbt, CallbackInfoReturnable<NbtCompound> cir) {
        if (persistentData != null)
            nbt.put("persistentData", persistentData.copy());
        cir.setReturnValue(nbt);
    }

    @Inject(at = @At("RETURN"), method = "readNbt")
    public void readNbt(NbtCompound nbt, CallbackInfo ci) {
        if (nbt.contains("persistentData", 10))
            persistentData = nbt.getCompound("persistentData");
    }

    @Override
    public NbtCompound getPersistentData() {
        if (persistentData == null)
            persistentData = new NbtCompound();
        return persistentData;
    }
}