package insane96mcp.progressivebosses.module.elderguardian.feature;

import insane96mcp.progressivebosses.utils.Label;
import insane96mcp.progressivebosses.utils.LabelConfigGroup;
import insane96mcp.progressivebosses.utils.LivingEntityEvents;
import insane96mcp.progressivebosses.utils.LivingEntityEvents.OnLivingHurtEvent;
import me.lortseam.completeconfig.api.ConfigEntries;
import me.lortseam.completeconfig.api.ConfigEntry;
import net.minecraft.world.entity.monster.ElderGuardian;

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
		if (event.getEntity().level.isClientSide)
			return;

		if (this.bonusDamage == 0d)
			return;

		if (!(event.getSource().getDirectEntity() instanceof ElderGuardian))
			return;

		ElderGuardian elderGuardian = (ElderGuardian) event.getSource().getDirectEntity();

		float bonusDamage = (float) (this.bonusDamage * BaseFeature.getDeadElderGuardians(elderGuardian));

		event.setAmount(event.getAmount() * (1f + bonusDamage));
	}

	@ConfigEntries.Exclude
	private static final int BASE_ATTACK_DURATION = 60;

	public int getAttackDuration(ElderGuardian elderGuardian) {
		if (this.attackDurationReduction == 0)
			return BASE_ATTACK_DURATION;
		int elderGuardiansNearby = elderGuardian.level.getEntities(elderGuardian, elderGuardian.getBoundingBox().inflate(48d), entity -> entity instanceof ElderGuardian).size();
		if (elderGuardiansNearby == 2)
			return BASE_ATTACK_DURATION;

		return BASE_ATTACK_DURATION - ((2 - elderGuardiansNearby) * this.attackDurationReduction);
	}
}
