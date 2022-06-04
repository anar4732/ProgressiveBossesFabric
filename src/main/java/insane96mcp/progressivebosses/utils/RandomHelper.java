package insane96mcp.progressivebosses.utils;

import java.util.Random;

public class RandomHelper {
	public static int getInt(Random rand, int min, int max) {
		if (min == max)
			return min;
		return rand.nextInt(max - min) + min;
	}

	public static float getFloat(Random rand, float min, float max) {
		if (min == max)
			return min;
		return rand.nextFloat() * (max - min) + min;
	}

	public static double getDouble(Random rand, double min, double max) {
		if (min == max)
			return min;
		return rand.nextFloat() * (max - min) + min;
	}
}