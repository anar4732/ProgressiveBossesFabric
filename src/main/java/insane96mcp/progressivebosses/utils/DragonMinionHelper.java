package insane96mcp.progressivebosses.utils;

import net.minecraft.entity.mob.ShulkerEntity;
import net.minecraft.nbt.NbtCompound;

public class DragonMinionHelper {
	public static boolean isBlindingMinion(ShulkerEntity shulker) {
		NbtCompound compound = shulker.writeNbt(new NbtCompound());
		return compound.getByte("Color") == 15;
	}

	public static void setMinionColor(ShulkerEntity shulker, boolean blinding) {
		NbtCompound compound = shulker.writeNbt(new NbtCompound());
		if (blinding)
			compound.putByte("Color", (byte) 15);
		else
			compound.putByte("Color", (byte) 10);
		shulker.readNbt(compound);
	}
}
