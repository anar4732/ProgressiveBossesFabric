package insane96mcp.progressivebosses.module.elderguardian.feature;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import insane96mcp.progressivebosses.utils.Drop;
import insane96mcp.progressivebosses.utils.DummyEvent;
import insane96mcp.progressivebosses.utils.Label;
import insane96mcp.progressivebosses.utils.LabelConfigGroup;
import insane96mcp.progressivebosses.utils.LivingEntityEvents;
import insane96mcp.progressivebosses.utils.LivingEntityEvents.OnLivingDeathEvent;
import me.lortseam.completeconfig.api.ConfigEntries;
import me.lortseam.completeconfig.api.ConfigEntry;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.minecraft.entity.mob.ElderGuardianEntity;

@ConfigEntries(includeAll = true)
@Label(name = "Rewards", description = "Bonus Experience and Dragon Egg per player")
public class RewardFeature implements LabelConfigGroup {

	@ConfigEntry(nameKey = "Base Experience", comment = "How much experience will an Elder Guardian drop.")
	@ConfigEntry.BoundedInteger(min = 0, max = 1024)
	public int baseExperience = 40;

	@ConfigEntry(nameKey = "Bonus Experience", comment = "How much more experience (percentage) will Elder Guardian drop per killed Elder Guardian. The percentage is additive (e.g. with this set to 100%, the last Elder will drop 200% more experience)")
	@ConfigEntry.BoundedDouble(min = 0.0, max = Double.MAX_VALUE)
	public double bonusExperience = 1.0d;

	@ConfigEntry(nameKey = "Drops", comment = "A list of bonus drops for the Elder Guardian. Entry format: item,amount,missing_guardians,chance,mode,chance_mode\n" +
			"item: item id\n" +
			"amount: amount\n" +
			"missing_guardians: the amount of missing guardians required for the item to drop, works differently based on mode\n" +
			"chance: chance for the drop to happen, between 0 and 1\n" +
			"mode:\n" +
			"* MINIMUM: will try to drop the item when the missing_guardians matches or is higher\n" +
			"* PER_DIFFICULTY: will try to drop the item one more time per missing_guardians\n" +
			"chance_mode:\n" +
			"* FLAT: chance is the percentage chance for the item to drop if the difficulty criteria matches\n" +
			"* SCALING: each point of difficulty >= 'difficulty to drop the item' will be multiplied by the chance (e.g. chance 2% and difficulty 10, difficulty required 5, chance to drop the item will be chance * (difficulty - difficulty_required + 1) = 2% * (10 - 5 + 1) = 12%)\n")
	private static List<String> dropsListConfig = Arrays.asList("minecraft:wet_sponge,1,0,1,MINIMUM,FLAT", "minecraft:wet_sponge,2,1,1,MINIMUM,FLAT", "minecraft:wet_sponge,2,2,1,MINIMUM,FLAT", "progressivebosses:elder_guardian_spike,1,0,1,MINIMUM,FLAT");
	
	@ConfigEntries.Exclude
	public ArrayList<Drop> dropsList;
	
	public RewardFeature(LabelConfigGroup parent) {
		parent.addConfigContainer(this);
		this.dropsList = Drop.parseDropsList(dropsListConfig);
		LivingEntityEvents.DEATH.register((event) -> this.onDeath(event));
		ServerEntityEvents.ENTITY_LOAD.register((entity, world) -> this.onSpawn(new DummyEvent(world, entity)));

	}

	public void onSpawn(DummyEvent event) {
		if (event.getWorld().isClient)
			return;

		if (this.baseExperience == 0d)
			return;

		if (!(event.getEntity() instanceof ElderGuardianEntity))
			return;

		ElderGuardianEntity elderGuardian = (ElderGuardianEntity) event.getEntity();

		elderGuardian.experiencePoints = (int) (this.baseExperience * this.bonusExperience);
	}

	// public void onExperienceDrop(LivingExperienceDropEvent event) {
	// 	if (this.bonusExperience == 0d)
	// 		return;

	// 	if (!(event.getEntity() instanceof ElderGuardianEntity))
	// 		return;

	// 	int bonusExperience = (int) (event.getOriginalExperience() * (this.bonusExperience));
	// 	event.setDroppedExperience(event.getOriginalExperience() + bonusExperience);
	// }

	public void onDeath(OnLivingDeathEvent event) {
		if (this.dropsList.isEmpty())
			return;

		if (!(event.getEntity() instanceof ElderGuardianEntity))
			return;

		ElderGuardianEntity elderGuardian = (ElderGuardianEntity) event.getEntity();

		for (Drop drop : this.dropsList) {
			drop.drop(elderGuardian.getWorld(), elderGuardian.getPos(), BaseFeature.getDeadElderGuardians(elderGuardian));
		}
	}
}
