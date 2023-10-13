package insane96mcp.progressivebosses.module.elderguardian.feature;

import insane96mcp.progressivebosses.utils.*;
import me.lortseam.completeconfig.api.ConfigEntries;
import me.lortseam.completeconfig.api.ConfigEntry;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.ElderGuardian;

@ConfigEntries(includeAll = true)
@Label(name = "Health", description = "Bonus Health and Health regeneration.")
public class HealthFeature implements LabelConfigGroup {

	@ConfigEntry(nameKey = "Health Bonus", comment = "Increase Elder Guardians' Health by this percentage (1 = +100% health)")
	@ConfigEntry.BoundedDouble(min = 0d, max = Double.MAX_VALUE)
	public double bonusHealth = 0.5d;

	@ConfigEntry(nameKey = "Absorption Health", comment = "Adds absorption health to Elder Guradians (health that doesn't regen)")
	@ConfigEntry.BoundedDouble(min = 0d, max = Double.MAX_VALUE)
	public double absorptionHealth = 40d;

	@ConfigEntry(nameKey = "Health Regen", comment = "Health Regen per second")
	@ConfigEntry.BoundedDouble(min = 0d, max = Double.MAX_VALUE)
	public double healthRegen = 0.5d;


	public HealthFeature(LabelConfigGroup parent) {
		parent.addConfigContainer(this);
		ServerEntityEvents.ENTITY_LOAD.register((entity, world) -> this.onSpawn(new DummyEvent(world, entity)));
		LivingEntityEvents.TICK.register((entity) -> this.onUpdate(new DummyEvent(entity.level, entity)));

	}

	public void onSpawn(DummyEvent event) {
		if (event.getWorld().isClientSide)
			return;

		if (this.bonusHealth == 0d && this.absorptionHealth == 0d)
			return;

		if (!(event.getEntity() instanceof ElderGuardian))
			return;

		ElderGuardian elderGuardian = (ElderGuardian) event.getEntity();
		CompoundTag nbt = ((IEntityExtraData) elderGuardian).getPersistentData();
		if (nbt.getBoolean(Strings.Tags.DIFFICULTY))
			return;

		nbt.putBoolean(Strings.Tags.DIFFICULTY, true);

		if (this.bonusHealth > 0d) {
			if (elderGuardian.getAttribute(Attributes.MAX_HEALTH).getModifier(Strings.AttributeModifiers.BONUS_HEALTH_UUID) != null)
				return;
			MCUtils.applyModifier(elderGuardian, Attributes.MAX_HEALTH, Strings.AttributeModifiers.BONUS_HEALTH_UUID, Strings.AttributeModifiers.BONUS_HEALTH, this.bonusHealth, AttributeModifier.Operation.MULTIPLY_BASE);
		}

		if (this.absorptionHealth > 0d)
			elderGuardian.setAbsorptionAmount((float) this.absorptionHealth);
	}

	public void onUpdate(DummyEvent event) {
		if (event.getEntity().level.isClientSide)
			return;

		if (!(event.getEntity() instanceof ElderGuardian))
			return;

		if (this.healthRegen == 0d)
			return;

		ElderGuardian elderGuardian = (ElderGuardian) event.getEntity();

		if (!elderGuardian.isAlive())
			return;

		// divided by 20 because is the health regen per second and here I need per tick
		float heal = (float) this.healthRegen / 20f;
		elderGuardian.heal(heal);
	}
}