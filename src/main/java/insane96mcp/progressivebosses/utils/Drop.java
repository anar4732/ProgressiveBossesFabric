package insane96mcp.progressivebosses.utils;

import insane96mcp.progressivebosses.ProgressiveBosses;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.apache.commons.lang3.math.NumberUtils;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class Drop {

	public Identifier itemId;
	public int amount;
	public int difficultyRequired;
	public double chance;
	public DifficultyMode difficultyMode;
	public ChanceMode chanceMode;

	public Drop(Identifier itemId, int amount, int difficultyRequired, double chance, DifficultyMode difficultyMode, ChanceMode chanceMode) {
		this.itemId = itemId;
		this.amount = amount;
		this.difficultyRequired = difficultyRequired;
		this.chance = chance;
		this.difficultyMode = difficultyMode;
		this.chanceMode = chanceMode;
	}

	@Nullable
	public static Drop parseLine(String line) {
		String[] split = line.split(",");
		if (split.length != 6) {
			// LogHelper.warn("Invalid line \"%s\" for Drop", line);
			return null;
		}

		//Item
		Identifier item = Identifier.tryParse(split[0]);
		if (item == null) {
			// LogHelper.warn("%s item for Drop is not a valid Resource Location", split[0]);
			return null;
		}
		if (!Registries.ITEM.containsId(item)) {
			// LogHelper.warn("%s item for Drop seems to not exist", split[0]);
			return null;
		}

		//Amount
		if (!NumberUtils.isParsable(split[1])) {
			// LogHelper.warn(String.format("Invalid amount \"%s\" for Drop", line));
			return null;
		}
		int amount = Integer.parseInt(split[1]);

		//Difficulty required
		if (!NumberUtils.isParsable(split[2])) {
			// LogHelper.warn(String.format("Invalid difficulty_required \"%s\" for Drop", line));
			return null;
		}
		int difficultyRequired = Integer.parseInt(split[2]);

		//Chance
		if (!NumberUtils.isParsable(split[3])) {
			// LogHelper.warn(String.format("Invalid chance \"%s\" for Drop", line));
			return null;
		}
		double chance = Double.parseDouble(split[3]);

		//Difficulty Mode
		DifficultyMode difficultyMode;
		try {
			difficultyMode = DifficultyMode.valueOf(split[4]);
		}
		catch (IllegalArgumentException e) {
			// LogHelper.warn(String.format("Invalid difficulty_mode \"%s\" for Drop", line));
			return null;
		}

		//Chance Mode
		ChanceMode chanceMode;
		try {
			chanceMode = ChanceMode.valueOf(split[5]);
		}
		catch (IllegalArgumentException e) {
			// LogHelper.warn(String.format("Invalid chance_mode \"%s\" for Drop", line));
			return null;
		}

		return new Drop(item, amount, difficultyRequired, chance, difficultyMode, chanceMode);
	}

	public enum DifficultyMode {
		MINIMUM,
		PER_DIFFICULTY
	}

	public enum ChanceMode {
		FLAT,
		SCALING
	}

	public static ArrayList<Drop> parseDropsList(List<? extends String> list) {
		ArrayList<Drop> drops = new ArrayList<>();
		for (String line : list) {
			Drop drop = Drop.parseLine(line);
			if (drop != null)
				drops.add(drop);
		}
		return drops;
	}

	public void drop(World world, Vec3d pos, float difficulty) {
		if (this.amount == 0)
			return;
		if (difficulty < this.difficultyRequired)
			return;

		double chance = this.chance;
		if (difficulty >= this.difficultyRequired && this.chanceMode == Drop.ChanceMode.SCALING)
			chance *= difficulty - this.difficultyRequired + 1;

		if (this.difficultyMode == Drop.DifficultyMode.MINIMUM) {
			if (RandomHelper.getDouble(world.random, 0d, 1d) >= chance)
				return;
			world.spawnEntity(createDrop(world, pos, Registries.ITEM.get(this.itemId), this.amount));
		}
		else if (this.difficultyMode == Drop.DifficultyMode.PER_DIFFICULTY) {
			int tries = (int) (difficulty - this.difficultyRequired + 1);
			if (tries == 0)
				return;
			int dropped = 0;
			for (int i = 0; i < tries; i++) {
				if (RandomHelper.getDouble(world.random, 0d, 1d) >= chance)
					continue;
				dropped++;
				world.spawnEntity(createDrop(world, pos, Registries.ITEM.get(this.itemId), this.amount));
			}
			if (this.itemId.equals(Registries.ITEM.getId(ProgressiveBosses.NETHER_STAR_SHARD)) && dropped < difficulty * chance) {
				world.spawnEntity(createDrop(world, pos, Registries.ITEM.get(this.itemId), (int) (Math.round(difficulty * chance - dropped))));
			}
		}
	}

	private static ItemEntity createDrop(World world, Vec3d pos, Item item, int amount) {
		ItemEntity itemEntity = new ItemEntity(world, pos.x, pos.y, pos.z, new ItemStack(item, amount));
		//If it's a nether star shard set it as "invincible"
		if (Registries.ITEM.getId(item).equals(Registries.ITEM.getId(ProgressiveBosses.NETHER_STAR_SHARD))) {
			NbtCompound compoundNBT = new NbtCompound();
			itemEntity.writeCustomDataToNbt(compoundNBT);
			compoundNBT.putShort("Health", Short.MAX_VALUE);
			itemEntity.readCustomDataFromNbt(compoundNBT);
		}
		return itemEntity;
	}
}