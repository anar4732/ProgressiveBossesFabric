package insane96mcp.progressivebosses.module.dragon.feature;

import insane96mcp.progressivebosses.utils.*;
import me.lortseam.completeconfig.api.ConfigEntries;
import me.lortseam.completeconfig.api.ConfigEntry;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.enderdragon.phases.EnderDragonPhase;

@ConfigEntries(includeAll = true)
@Label(name = "Health", description = "Bonus Health and Bonus regeneration.")
public class HealthFeature implements LabelConfigGroup {

	@ConfigEntry(nameKey = "Health Bonus per Difficulty", comment = "Increase Ender Dragon's Health by this value per difficulty")
	@ConfigEntry.BoundedDouble(min = 0d, max = Double.MAX_VALUE)
	public double bonusPerDifficulty = 25d;

	@ConfigEntry(nameKey = "Maximum Bonus Regeneration", comment = "Maximum bonus regeneration per second given by \"Bonus Regeneration per Difficulty\". Set to 0 to disable bonus health regeneration. This doesn't affect the crystal regeneration of the Ender Dragon.")
	@ConfigEntry.BoundedDouble(min = 0d, max = Double.MAX_VALUE)
	public double maxBonusRegen = 1.0d;

	@ConfigEntry(nameKey = "Bonus Regeneration per Difficulty", comment = "How much health will the Ender Dragon regen per difficulty. This is added to the noaml Crystal regeneration.")
	@ConfigEntry.BoundedDouble(min = 0d, max = Double.MAX_VALUE)
	public double bonusRegenPerDifficulty = 0.125d;

	@ConfigEntry(nameKey = "Bonus Crystal Regeneration", comment = "How much health (when missing 100% health) will the Ender Dragon regen per difficulty each second whenever she's attached to a Crystal. So if she's missing 30% health, this will be 30% effective. This is added to the normal Crystal regen.")
	@ConfigEntry.BoundedDouble(min = 0d, max = Double.MAX_VALUE)
	public double bonusCrystalRegen = 0d;

	public HealthFeature(LabelConfigGroup parent) {
		parent.addConfigContainer(this);
		ServerEntityEvents.ENTITY_LOAD.register((entity, world) -> this.onSpawn(new DummyEvent(world, entity)));
		LivingEntityEvents.TICK.register((entity) -> this.onUpdate(new DummyEvent(entity.level, entity)));
	}

	public void onSpawn(DummyEvent event) {
		if (event.getWorld().isClientSide)
			return;

		if (this.bonusPerDifficulty == 0d)
			return;

		if (!(event.getEntity() instanceof EnderDragon enderDragon))
			return;

		if (enderDragon.getAttribute(Attributes.MAX_HEALTH).getModifier(Strings.AttributeModifiers.BONUS_HEALTH_UUID) != null)
			return;

		CompoundTag dragonTags = ((IEntityExtraData) enderDragon).getPersistentData();
		double difficulty = dragonTags.getFloat(Strings.Tags.DIFFICULTY);
		MCUtils.applyModifier(enderDragon, Attributes.MAX_HEALTH, Strings.AttributeModifiers.BONUS_HEALTH_UUID, Strings.AttributeModifiers.BONUS_HEALTH, difficulty * this.bonusPerDifficulty, AttributeModifier.Operation.ADDITION);
	}

	public void onUpdate(DummyEvent event) {
		if (event.getEntity().level.isClientSide)
			return;

		if (!(event.getEntity() instanceof EnderDragon enderDragon))
			return;

		if (!enderDragon.isAlive() || enderDragon.getPhaseManager().getCurrentPhase().getPhase() == EnderDragonPhase.DYING)
			return;

		CompoundTag tags = ((IEntityExtraData) enderDragon).getPersistentData();

		float difficulty = tags.getFloat(Strings.Tags.DIFFICULTY);

		if (difficulty <= 0)
			return;

		float flatBonusHeal = getFlatBonusHeal(difficulty);
		float crystalBonusHeal = getCrystalBonusHeal(enderDragon, difficulty);

		float heal = flatBonusHeal + crystalBonusHeal;
		if (heal == 0f)
			return;

		heal /= 20f;

		enderDragon.heal(heal);
	}

	private float getFlatBonusHeal(float difficulty) {
		if (this.bonusRegenPerDifficulty == 0d || this.maxBonusRegen == 0d)
			return 0f;
		return (float) Math.min(difficulty * this.bonusRegenPerDifficulty, this.maxBonusRegen);
	}

	private float getCrystalBonusHeal(EnderDragon enderDragon, float difficulty) {
		if (this.bonusCrystalRegen == 0d)
			return 0f;

		if (enderDragon.nearestCrystal == null || !enderDragon.nearestCrystal.isAlive())
			return 0f;

		double currHealthPerc = 1 - (enderDragon.getHealth() / enderDragon.getMaxHealth());
		return (float) (this.bonusCrystalRegen * difficulty * currHealthPerc);
	}
}