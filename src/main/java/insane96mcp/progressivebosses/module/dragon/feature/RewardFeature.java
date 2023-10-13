package insane96mcp.progressivebosses.module.dragon.feature;

import insane96mcp.progressivebosses.utils.*;
import insane96mcp.progressivebosses.utils.LivingEntityEvents.OnLivingDeathEvent;
import me.lortseam.completeconfig.api.ConfigEntries;
import me.lortseam.completeconfig.api.ConfigEntry;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.level.block.Blocks;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@ConfigEntries(includeAll = true)
@Label(name = "Rewards", description = "Bonus Experience and Dragon Egg per player")
public class RewardFeature implements LabelConfigGroup {
	
	@ConfigEntry(nameKey = "Bonus Experience per Difficulty", comment = "How much more experience (percentage) will Dragon drop per Difficulty. The percentage is additive (e.g. with this set to 100%, 7 dragons killed = 700% more experience)")
	@ConfigEntry.BoundedDouble(min = 0d, max = Double.MAX_VALUE)
	public double bonusExperience = 4.5d;
	
	@ConfigEntry(nameKey = "Dragon Egg per Player", comment = "If true whenever a player, that has never killed the dragon, kills the dragon a Dragon Egg ìì will drop. E.g. If 2 players kill the Dragon for the first time, she will drop 2 Dragon Eggs")
	public boolean dragonEggPerPlayer = true;
	
	@ConfigEntry(nameKey = "Drops", comment = """
		A list of drops for the Dragons. Entry format: item,amount,difficulty_required,chance,difficulty_mode,chance_mode
		item: item id
		amount: amount
		difficulty_required: the amount of difficulty required for the item to drop, works differently based on mode
		chance: chance for the drop to happen, between 0 and 1
		difficulty_mode:
		* MINIMUM: will try to drop the item when the difficulty matches or is higher
		* PER_DIFFICULTY: will try to drop the item once per difficulty (e.g. at difficulty 10, difficulty required 3, the chance to drop the item is tried 7 times)
		chance_mode:
		* FLAT: chance is the percentage chance for the item to drop if the difficulty criteria matches
		* SCALING: each point of difficulty >= 'difficulty to drop the item' will be multiplied by the chance (e.g. difficulty 10, chance 2% and difficulty required 5, chance to drop the item will be chance * (difficulty - difficulty_required + 1) = 2% * (10 - 5 + 1) = 12%)""")
	private static final List<String> dropsListConfig = Arrays.asList("minecraft:enchanted_golden_apple,1,5,0.10,MINIMUM,SCALING");
	
	@ConfigEntries.Exclude
	public ArrayList<Drop> dropsList;

	public RewardFeature(LabelConfigGroup parent) {
		parent.addConfigContainer(this);
		this.dropsList = Drop.parseDropsList(dropsListConfig);
		ServerEntityEvents.ENTITY_LOAD.register((entity, world) -> this.onSpawn(new DummyEvent(world, entity)));
		LivingEntityEvents.DEATH.register((event) -> this.onDeath(event));
		LivingEntityEvents.TICK.register((entity) -> this.onUpdate(new DummyEvent(entity.level, entity)));
	}

	public void onSpawn(DummyEvent event) {
		if (event.getWorld().isClientSide)
			return;

		if (this.bonusExperience == 0d)
			return;

		if (!(event.getEntity() instanceof EnderDragon dragon))
			return;

		CompoundTag dragonTags = ((IEntityExtraData) dragon).getPersistentData();
		float difficulty = dragonTags.getFloat(Strings.Tags.DIFFICULTY);

		dragon.xpReward = (int) (dragon.xpReward * (this.bonusExperience * difficulty));
	}

	public void onUpdate(DummyEvent event) {
		if (!(event.getEntity() instanceof EnderDragon dragon))
			return;

		dropEgg(dragon);
	}

	// public void onExpDrop(LivingExperienceDropEvent event) {


	// 	if (!(event.getEntity() instanceof EnderDragonEntity dragon))
	// 		return;

	// 	if (this.bonusExperience == 0d)
	// 		return;

	// 	NbtCompound dragonTags = ((IEntityExtraData) dragon).getPersistentData();
	// 	float difficulty = dragonTags.getFloat(Strings.Tags.DIFFICULTY);
	// 	if (difficulty == 0d)
	// 		return;

	// 	event.setDroppedExperience((int) (event.getDroppedExperience() * this.bonusExperience * difficulty));
	// }

	private void dropEgg(EnderDragon dragon) {
		if (!this.dragonEggPerPlayer)
			return;

		if (dragon.dragonDeathTime != 100)
			return;

		CompoundTag tags = ((IEntityExtraData) dragon).getPersistentData();

		int eggsToDrop = tags.getInt(Strings.Tags.EGGS_TO_DROP);

		if (dragon.getDragonFight() != null && !dragon.getDragonFight().hasPreviouslyKilledDragon()) {
			eggsToDrop--;
		}

		for (int i = 0; i < eggsToDrop; i++) {
			dragon.level.setBlockAndUpdate(new BlockPos(0, 255 - i, 0), Blocks.DRAGON_EGG.defaultBlockState());
		}
	}

	public void onDeath(OnLivingDeathEvent event) {
		if (this.dropsList.isEmpty())
			return;

		if (!(event.getEntity() instanceof EnderDragon dragon))
			return;

		CompoundTag tags = ((IEntityExtraData) dragon).getPersistentData();
		float difficulty = tags.getFloat(Strings.Tags.DIFFICULTY);
		for (Drop drop : this.dropsList) {
			drop.drop(dragon.level, dragon.position(), difficulty);
		}
	}

}