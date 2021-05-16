package insane96mcp.progressivebosses.modules.wither.feature;

import insane96mcp.insanelib.base.Feature;
import insane96mcp.insanelib.base.Label;
import insane96mcp.insanelib.base.Module;
import insane96mcp.insanelib.utils.RandomHelper;
import insane96mcp.progressivebosses.base.Strings;
import insane96mcp.progressivebosses.modules.wither.classutils.Drop;
import insane96mcp.progressivebosses.setup.Config;
import net.minecraft.entity.boss.WitherEntity;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Label(name = "Rewards", description = "Bonus Experience and Drops")
public class RewardFeature extends Feature {

	private final ForgeConfigSpec.ConfigValue<Double> bonusExperienceConfig;
	private final ForgeConfigSpec.ConfigValue<List<? extends String>> dropsListConfig;

	private static final List<String> dropsListDefault = Arrays.asList("progressivebosses:nether_star_shard,1,2,2,MINIMUM,SCALING",
			"progressivebosses:nether_star_shard,2,4,4,MINIMUM,SCALING",
			"progressivebosses:nether_star_shard,4,8,8,MINIMUM,SCALING");

	public double bonusExperience = 20d;
	public ArrayList<Drop> dropsList;

	public RewardFeature(Module module) {
		super(Config.builder, module);
		Config.builder.comment(this.getDescription()).push(this.getName());
		bonusExperienceConfig = Config.builder
				.comment("How much more experience (percentage) will Wither drop per Difficulty. The percentage is additive (e.g. with this set to 10%, 7 withers spawned = 70% more experience)")
				.defineInRange("Bonus Experience per Difficulty", bonusExperience, 0.0, Double.MAX_VALUE);
		dropsListConfig = Config.builder
				.comment("A list of drops for the Withers. Entry format: item,amount,difficulty_required,chance,difficulty_mode,chance_mode\n" +
						"item: item id\n" +
						"amount: amount\n" +
						"difficulty_required: the amount of difficulty required for the item to drop, works differently based on mode\n" +
						"chance: chance for the drop to happen\n" +
						"difficulty_mode:\n" +
						"* MINIMUM: will try to drop the item when the difficulty matches or is higher\n" +
						"chance_mode:\n" +
						"* FLAT: chance is the percentage chance for the item to drop if the difficulty criteria matches\n" +
						"* SCALING: each point of difficulty >= 'difficulty to drop the item' will be multiplied by the chance (e.g. chance 2% and difficulty 10, difficulty required 5, chance to drop the item will be chance * (difficulty - difficulty_required + 1) = 2% * (10 - 5 + 1) = 12%)\n" +
						"By default Withers have 2% chance per difficulty >= 2 to drop 1 shard + 4% chance per difficulty >= 4 to drop 2 shards + 8% chance per difficulty >= 8 to drop 4 shards.")
				.defineList("Drops", dropsListDefault, o -> o instanceof String);
		Config.builder.pop();
	}

	@Override
	public void loadConfig() {
		super.loadConfig();
		this.bonusExperience = this.bonusExperienceConfig.get();
		this.dropsList = parseDropsList(this.dropsListConfig.get());
	}

	private static ArrayList<Drop> parseDropsList(List<? extends String> list) {
		ArrayList<Drop> drops = new ArrayList<>();
		for (String line : list) {
			Drop drop = Drop.parseLine(line);
			if (drop != null)
				drops.add(drop);
		}
		return drops;
	}

	@SubscribeEvent
	public void onSpawn(EntityJoinWorldEvent event) {
		if (!this.isEnabled())
			return;

		if (this.bonusExperience == 0d)
			return;

		if (!(event.getEntity() instanceof WitherEntity))
			return;

		WitherEntity wither = (WitherEntity) event.getEntity();

		CompoundNBT witherTags = wither.getPersistentData();
		float difficulty = witherTags.getFloat(Strings.Tags.DIFFICULTY);

		wither.experienceValue = 50 + (int) (50 * (this.bonusExperience * difficulty / 100f));
	}

	@SubscribeEvent
	public void setDrops(LivingDropsEvent event) {
		if (!this.isEnabled())
			return;

		if (this.dropsList.isEmpty())
			return;

		if (!(event.getEntityLiving() instanceof WitherEntity))
			return;

		WitherEntity wither = (WitherEntity) event.getEntityLiving();

		CompoundNBT tags = wither.getPersistentData();
		float difficulty = tags.getFloat(Strings.Tags.DIFFICULTY);
		for (Drop drop : this.dropsList) {
			if (drop.amount == 0)
				continue;
			if (difficulty < drop.difficultyRequired)
				continue;

			double chance = drop.chance / 100d;
			if (difficulty >= drop.difficultyRequired && drop.chanceMode == Drop.ChanceMode.SCALING)
				chance *= difficulty - drop.difficultyRequired + 1;

			if (RandomHelper.getDouble(wither.world.rand, 0d, 1d) >= chance)
				continue;

			ItemEntity itemEntity = new ItemEntity(wither.world, wither.getPositionVec().getX(), wither.getPositionVec().getY(), wither.getPositionVec().getZ(), new ItemStack(ForgeRegistries.ITEMS.getValue(drop.itemId), drop.amount));
			event.getDrops().add(itemEntity);
		}
	}
}