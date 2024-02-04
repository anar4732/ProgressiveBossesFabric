package insane96mcp.progressivebosses.module.elderguardian.feature;

import insane96mcp.progressivebosses.utils.Label;
import insane96mcp.progressivebosses.utils.LabelConfigGroup;
import insane96mcp.progressivebosses.utils.LivingEntityEvents;
import insane96mcp.progressivebosses.utils.LivingEntityEvents.OnLivingHurtEvent;
import me.lortseam.completeconfig.api.ConfigEntries;
import me.lortseam.completeconfig.api.ConfigEntry;
import net.minecraft.entity.mob.ElderGuardianEntity;

@ConfigEntries(includeAll = true)
@Label(name = "Attack", description = "More damage and attack speed based off Elder Guardians Defeated")
public class AttackFeature implements LabelConfigGroup {

	@ConfigEntry(nameKey = "Bonus Damage per Elder Guardian Defeated", comment = "Percentage Bonus damage per defeated Elder Guardian.")
	@ConfigEntry.BoundedDouble(min = 0d, max = 128d)
	public double bonusDamage = 0d;

	@ConfigEntry(nameKey = "Attack Duration Reduction per Elder Guardian Defeated", comment = "How many ticks faster will Elder Guardian attack (multiplied by defeated Elder Guardians). Vanilla Attack Duration is 60 ticks (3 secs)")
	@ConfigEntry.BoundedInteger(min = 0, max = 60)
	public int attackDurationReduction = 25;

	public AttackFeature(LabelConfigGroup parent) {
		parent.addConfigContainer(this);
		LivingEntityEvents.HURT.register((event) -> this.onDamageDealt(event));
		
	}

	public void onDamageDealt(OnLivingHurtEvent event) {
		if (event.getEntity().getWorld().isClient)
			return;

		if (this.bonusDamage == 0d)
			return;

		if (!(event.getSource().getSource() instanceof ElderGuardianEntity))
			return;

		ElderGuardianEntity elderGuardian = (ElderGuardianEntity) event.getSource().getSource();

		float bonusDamage = (float) (this.bonusDamage * BaseFeature.getDeadElderGuardians(elderGuardian));

		event.setAmount(event.getAmount() * (1f + bonusDamage));
	}

	@ConfigEntries.Exclude
	private static final int BASE_ATTACK_DURATION = 60;

	public int getAttackDuration(ElderGuardianEntity elderGuardian) {
		if (this.attackDurationReduction == 0)
			return BASE_ATTACK_DURATION;
		int elderGuardiansNearby = elderGuardian.getWorld().getOtherEntities(elderGuardian, elderGuardian.getBoundingBox().expand(48d), entity -> entity instanceof ElderGuardianEntity).size();
		if (elderGuardiansNearby == 2)
			return BASE_ATTACK_DURATION;

		return BASE_ATTACK_DURATION - ((2 - elderGuardiansNearby) * this.attackDurationReduction);
	}
}
