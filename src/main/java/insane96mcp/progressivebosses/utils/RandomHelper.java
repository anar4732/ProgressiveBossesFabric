package insane96mcp.progressivebosses.utils;

import net.minecraft.util.RandomSource;

public class RandomHelper {
	public static int getInt(RandomSource rand, int min, int max) {
		if (min == max)
			return min;
		return rand.nextInt(max - min) + min;
	}

	public static float getFloat(RandomSource rand, float min, float max) {
		if (min == max)
			return min;
		return rand.nextFloat() * (max - min) + min;
	}

	public static double getDouble(RandomSource rand, double min, double max) {
		if (min == max)
			return min;
		return rand.nextFloat() * (max - min) + min;
	}
}