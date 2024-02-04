package insane96mcp.progressivebosses.module.wither.feature;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import insane96mcp.progressivebosses.utils.Drop;
import insane96mcp.progressivebosses.utils.DummyEvent;
import insane96mcp.progressivebosses.utils.IEntityExtraData;
import insane96mcp.progressivebosses.utils.Label;
import insane96mcp.progressivebosses.utils.LabelConfigGroup;
import insane96mcp.progressivebosses.utils.LivingEntityEvents;
import insane96mcp.progressivebosses.utils.Strings;
import insane96mcp.progressivebosses.utils.LivingEntityEvents.OnLivingDeathEvent;
import me.lortseam.completeconfig.api.ConfigEntries;
import me.lortseam.completeconfig.api.ConfigEntry;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.minecraft.entity.boss.WitherEntity;
import net.minecraft.nbt.NbtCompound;

@ConfigEntries(includeAll = true)
@Label(name = "Rewards", description = "Bonus Experience and Drops")
public class RewardFeature implements LabelConfigGroup {

	@ConfigEntry(nameKey = "Bonus Experience per Difficulty", comment = "How much more experience (percentage) will Wither drop per Difficulty. The percentage is additive (e.g. with this set to 200%, 7 withers spawned = 1400% more experience)")
	@ConfigEntry.BoundedDouble(min = 0d, max = Double.MAX_VALUE)
	public double bonusExperience = 7.5d;

	@ConfigEntry(nameKey = "Drops", comment = """
		A list of drops for the Withers. Entry format: item,amount,difficulty_required,chance,difficulty_mode,chance_mode
		item: item id
		amount: amount
		difficulty_required: the amount of difficulty required for the item to drop, works differently based on mode
		chance: chance for the drop to happen, between 0 and 1
		difficulty_mode:
		* MINIMUM: will try to drop the item when the difficulty matches or is higher
		* PER_DIFFICULTY: will try to drop the item once per difficulty (e.g. at difficulty 10, difficulty required 3, there is the chance to drop the item, trying 7 times)
		chance_mode:
		* FLAT: chance is the percentage chance for the item to drop if the difficulty criteria matches
		* SCALING: each point of difficulty >= 'difficulty to drop the item' will be multiplied by the chance (e.g. chance 2% and difficulty 10, difficulty required 5, chance to drop the item will be chance * (difficulty - difficulty_required + 1) = 2% * (10 - 5 + 1) = 12%)
		By default Withers have 75% chance per difficulty to drop 2 shard (So at difficulty 8, up to 16 shards can be dropped, 75% chance each 2).""")
	public static List<String> dropsListConfig = Arrays.asList("progressivebosses:nether_star_shard,2,1,0.75,PER_DIFFICULTY,FLAT", "minecraft:ancient_debris,2,1,0.75,PER_DIFFICULTY,FLAT");
	
	@ConfigEntries.Exclude
	public ArrayList<Drop> dropsList;

	public RewardFeature(LabelConfigGroup parent) {
		parent.addConfigContainer(this);
		this.dropsList = Drop.parseDropsList(dropsListConfig);
		ServerEntityEvents.ENTITY_LOAD.register((entity, world) -> this.onSpawn(new DummyEvent(world, entity)));
		LivingEntityEvents.DEATH.register((event) -> this.onDeath(event));
	}

	public void onSpawn(DummyEvent event) {
		if (event.getWorld().isClient)
			return;

		if (this.bonusExperience == 0d)
			return;

		if (!(event.getEntity() instanceof WitherEntity wither))
			return;

		NbtCompound witherTags = ((IEntityExtraData) wither).getPersistentData();
		float difficulty = witherTags.getFloat(Strings.Tags.DIFFICULTY);

		wither.experiencePoints = 50 + (int) (50 * (this.bonusExperience * difficulty));
	}

	public void onDeath(OnLivingDeathEvent event) {
		if (this.dropsList.isEmpty())
			return;

		if (!(event.getEntity() instanceof WitherEntity wither))
			return;

		NbtCompound tags = ((IEntityExtraData) wither).getPersistentData();
		float difficulty = tags.getFloat(Strings.Tags.DIFFICULTY);
		for (Drop drop : this.dropsList) {
			drop.drop(wither.getWorld(), wither.getPos(), difficulty);
		}
	}
}
