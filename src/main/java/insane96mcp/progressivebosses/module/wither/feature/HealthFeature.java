package insane96mcp.progressivebosses.module.wither.feature;

import insane96mcp.progressivebosses.utils.DummyEvent;
import insane96mcp.progressivebosses.utils.IEntityExtraData;
import insane96mcp.progressivebosses.utils.Label;
import insane96mcp.progressivebosses.utils.LabelConfigGroup;
import insane96mcp.progressivebosses.utils.LivingEntityEvents;
import insane96mcp.progressivebosses.utils.MCUtils;
import insane96mcp.progressivebosses.utils.Strings;
import me.lortseam.completeconfig.api.ConfigEntries;
import me.lortseam.completeconfig.api.ConfigEntry;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.boss.WitherEntity;
import net.minecraft.nbt.NbtCompound;

@ConfigEntries(includeAll = true)
@Label(name = "Health", description = "Bonus Health and Bonus regeneration. The feature even fixes the Wither health bar not updating on spawn.")
public class HealthFeature implements LabelConfigGroup {

	@ConfigEntry(nameKey = "Health Bonus per Difficulty", comment = "Increase Wither's Health by this value per difficulty")
	@ConfigEntry.BoundedDouble(min = 0d, max = Double.MAX_VALUE)
	public double bonusPerDifficulty = 90d;

	@ConfigEntry(nameKey = "Maximum Bonus Regeneration", comment = "Maximum bonus regeneration per second given by \"Bonus Regeneration per Difficulty\". Set to 0 to disable bonus health regeneration. This doesn't affect the natural regeneration of the Wither (1 Health per Second). Note that the health regen is disabled when Wither's health is between 49% and 50% to prevent making it impossible to approach when half health.")
	@ConfigEntry.BoundedDouble(min = 0d, max = Double.MAX_VALUE)
	public double maxBonusRegen = 2d;

	@ConfigEntry(nameKey = "Bonus Regeneration per Difficulty", comment = "How many half hearts will the Wither regen per difficulty. This is added to the natural regeneration of the Wither (1 Health per Second).")
	@ConfigEntry.BoundedDouble(min = 0d, max = Double.MAX_VALUE)
	public double bonusRegenPerDifficulty = 0.3d;

	public HealthFeature(LabelConfigGroup parent) {
		parent.addConfigContainer(this);
		ServerEntityEvents.ENTITY_LOAD.register((entity, world) -> this.onSpawn(new DummyEvent(world, entity)));
		LivingEntityEvents.TICK.register((entity) -> this.onUpdate(new DummyEvent(entity.getWorld(), entity)));
	}

	public void onSpawn(DummyEvent event) {
		if (event.getWorld().isClient)
			return;

		if (this.bonusPerDifficulty == 0d)
			return;

		if (!(event.getEntity() instanceof WitherEntity wither))
			return;

		if (wither.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH).getModifier(Strings.AttributeModifiers.BONUS_HEALTH_UUID) != null)
			return;

		NbtCompound witherTags = ((IEntityExtraData) wither).getPersistentData();
		double difficulty = witherTags.getFloat(Strings.Tags.DIFFICULTY);
		MCUtils.applyModifier(wither, EntityAttributes.GENERIC_MAX_HEALTH, Strings.AttributeModifiers.BONUS_HEALTH_UUID, Strings.AttributeModifiers.BONUS_HEALTH, difficulty * this.bonusPerDifficulty, EntityAttributeModifier.Operation.ADDITION);

		boolean hasInvulTicks = wither.getInvulnerableTimer() > 0;

		if (hasInvulTicks)
			wither.setHealth(Math.max(1, wither.getMaxHealth() - 200));
	}

	public void onUpdate(DummyEvent event) {
		if (event.getEntity().getWorld().isClient)
			return;

		if (!(event.getEntity() instanceof WitherEntity wither))
			return;

		if (this.bonusRegenPerDifficulty == 0d || this.maxBonusRegen == 0d)
			return;

		//fixInvulBossBar(wither);

		if (wither.getInvulnerableTimer() > 0 || !wither.isAlive())
			return;

		//Disable bonus health regen when health between 49% and 50%
		if (wither.getHealth() / wither.getMaxHealth() > 0.49f && wither.getHealth() / wither.getMaxHealth() < 0.50f)
			return;

		NbtCompound tags = ((IEntityExtraData) wither).getPersistentData();

		float difficulty = tags.getFloat(Strings.Tags.DIFFICULTY);

		if (difficulty <= 0)
			return;

		float heal = (float) Math.min(difficulty * this.bonusRegenPerDifficulty, this.maxBonusRegen);

		heal /= 20f;

		wither.heal(heal);
	}

	private void fixInvulBossBar(WitherEntity wither) {
		if (wither.getInvulnerableTimer() == 0)
			return;

		wither.bossBar.setPercent(wither.getHealth() / wither.getMaxHealth());
	}
}
