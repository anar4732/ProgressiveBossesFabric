package insane96mcp.progressivebosses.utils;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.monster.Shulker;

public class DragonMinionHelper {
	public static boolean isBlindingMinion(Shulker shulker) {
		CompoundTag compound = shulker.saveWithoutId(new CompoundTag());
		return compound.getByte("Color") == 15;
	}

	public static void setMinionColor(Shulker shulker, boolean blinding) {
		CompoundTag compound = shulker.saveWithoutId(new CompoundTag());
		if (blinding)
			compound.putByte("Color", (byte) 15);
		else
			compound.putByte("Color", (byte) 10);
		shulker.load(compound);
	}
}
