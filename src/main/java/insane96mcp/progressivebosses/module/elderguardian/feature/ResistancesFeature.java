package insane96mcp.progressivebosses.module.elderguardian.feature;

import insane96mcp.progressivebosses.utils.Label;
import insane96mcp.progressivebosses.utils.LabelConfigGroup;
import insane96mcp.progressivebosses.utils.LivingEntityEvents;
import insane96mcp.progressivebosses.utils.LivingEntityEvents.OnLivingHurtEvent;
import me.lortseam.completeconfig.api.ConfigEntries;
import me.lortseam.completeconfig.api.ConfigEntry;
import net.minecraft.world.entity.monster.ElderGuardian;

@ConfigEntries(includeAll = true)
@Label(name = "Resistances", description = "Handles the Damage Resistances")
public class ResistancesFeature implements LabelConfigGroup {

	@ConfigEntry(nameKey = "Damage Reduction per Elder Guardian Defeated", comment = "Percentage Damage Reduction for each Elder Guardian Defeated.")
	@ConfigEntry.BoundedDouble(min = 0d, max = 1d)
	public double resistancePerElderGuardianDefeated = 0.3d;

	public ResistancesFeature(LabelConfigGroup parent) {
		parent.addConfigContainer(this);
		LivingEntityEvents.HURT.register((event) -> this.onElderGuardianDamage(event));
	}

	public void onElderGuardianDamage(OnLivingHurtEvent event) {
		if (this.resistancePerElderGuardianDefeated == 0d)
			return;

		if (!(event.getEntity() instanceof ElderGuardian))
			return;

		ElderGuardian elderGuardian = (ElderGuardian) event.getEntity();

		float damageReduction = (float) (BaseFeature.getDeadElderGuardians(elderGuardian) * this.resistancePerElderGuardianDefeated);

		event.setAmount(event.getAmount() * (1f - damageReduction));
	}
}
