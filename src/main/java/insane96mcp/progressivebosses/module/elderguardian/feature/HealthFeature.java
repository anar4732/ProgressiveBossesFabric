package insane96mcp.progressivebosses.module.elderguardian.feature;

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
import net.minecraft.entity.mob.ElderGuardianEntity;
import net.minecraft.nbt.NbtCompound;

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
		LivingEntityEvents.TICK.register((entity) -> this.onUpdate(new DummyEvent(entity.getWorld(), entity)));

	}

	public void onSpawn(DummyEvent event) {
		if (event.getWorld().isClient)
			return;

		if (this.bonusHealth == 0d && this.absorptionHealth == 0d)
			return;

		if (!(event.getEntity() instanceof ElderGuardianEntity))
			return;

		ElderGuardianEntity elderGuardian = (ElderGuardianEntity) event.getEntity();
		NbtCompound nbt = ((IEntityExtraData) elderGuardian).getPersistentData();
		if (nbt.getBoolean(Strings.Tags.DIFFICULTY))
			return;

		nbt.putBoolean(Strings.Tags.DIFFICULTY, true);

		if (this.bonusHealth > 0d) {
			if (elderGuardian.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH).getModifier(Strings.AttributeModifiers.BONUS_HEALTH_UUID) != null)
				return;
			MCUtils.applyModifier(elderGuardian, EntityAttributes.GENERIC_MAX_HEALTH, Strings.AttributeModifiers.BONUS_HEALTH_UUID, Strings.AttributeModifiers.BONUS_HEALTH, this.bonusHealth, EntityAttributeModifier.Operation.MULTIPLY_BASE);
		}

		if (this.absorptionHealth > 0d)
			elderGuardian.setAbsorptionAmount((float) this.absorptionHealth);
	}

	public void onUpdate(DummyEvent event) {
		if (event.getEntity().getWorld().isClient)
			return;

		if (!(event.getEntity() instanceof ElderGuardianEntity))
			return;

		if (this.healthRegen == 0d)
			return;

		ElderGuardianEntity elderGuardian = (ElderGuardianEntity) event.getEntity();

		if (!elderGuardian.isAlive())
			return;

		// divided by 20 because is the health regen per second and here I need per tick
		float heal = (float) this.healthRegen / 20f;
		elderGuardian.heal(heal);
	}
}
