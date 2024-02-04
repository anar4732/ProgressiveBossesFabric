package insane96mcp.progressivebosses.module.wither.feature;

import insane96mcp.progressivebosses.utils.*;
import insane96mcp.progressivebosses.utils.LivingEntityEvents.OnLivingHurtEvent;
import me.lortseam.completeconfig.api.ConfigEntries;
import me.lortseam.completeconfig.api.ConfigEntry;
import net.minecraft.entity.boss.WitherEntity;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.nbt.NbtCompound;

@ConfigEntries(includeAll = true)
@Label(name = "Resistances & Vulnerabilities", description = "Handles the Damage Resistances and Vulnerabilities")
public class ResistancesFeature implements LabelConfigGroup {

	@ConfigEntry(nameKey = "Melee Damage reduction per Difficulty above half health", comment = "Percentage Melee Damage Reduction (per difficulty) while the Wither is above half health.")
	@ConfigEntry.BoundedDouble(min = 0d, max = 1d)
	public double meleeDamageReductionBeforeHalfHealth = 0.03d;

	@ConfigEntry(nameKey = "Max Melee Damage reduction per Difficulty before half health", comment = "Cap for 'Melee Damage reduction per Difficulty above half health'")
	@ConfigEntry.BoundedDouble(min = 0d, max = 1d)
	public double maxMeleeDamageReductionBeforeHalfHealth = 0.24d;

	@ConfigEntry(nameKey = "Melee Damage reduction per Difficulty below half health", comment = "Percentage Melee Damage Reduction (per difficulty) as the Wither drops below half health.")
	@ConfigEntry.BoundedDouble(min = 0d, max = 1d)
	public double meleeDamageReductionOnHalfHealth = 0.06d;

	@ConfigEntry(nameKey = "Max Melee Damage reduction per Difficulty below half health", comment = "Cap for 'Melee Damage Reduction per Difficulty below half health'")
	@ConfigEntry.BoundedDouble(min = 0d, max = 1d)
	public double maxDamageReductionOnHalfHealth = 0.48d;

	@ConfigEntry(nameKey = "Magic Damage Bonus", comment = "Bonus magic damage based off missing health. 150 means that every 150 missing health the damage will be amplified by 100%. E.g. The difficulty = 0 Wither (with 300 max health) is at half health (so it's missing 150hp), on magic damage he will receive 'magic_damage * (missing_health / magic_damage_bonus + 1)' = 'magic_damage * (150 / 150 + 1)' = 'magic_damage * 2'.")
	@ConfigEntry.BoundedDouble(min = 0d, max = 1024f)
	public double magicDamageBonus = 250d;

	public ResistancesFeature(LabelConfigGroup parent) {
		parent.addConfigContainer(this);
		LivingEntityEvents.HURT.register((event) -> this.onWitherDamage(event));
	}

	public void onWitherDamage(OnLivingHurtEvent event) {
		if (!(event.getEntity() instanceof WitherEntity wither))
			return;

		if ((this.meleeDamageReductionOnHalfHealth == 0d || this.maxDamageReductionOnHalfHealth == 0d)
				&& (this.meleeDamageReductionBeforeHalfHealth == 0d || this.maxMeleeDamageReductionBeforeHalfHealth == 0d)
				&& this.magicDamageBonus == 0d)
			return;

		//Handle Magic Damage
		if ((event.getSource().isOf(DamageTypes.MAGIC) || event.getSource().isOf(DamageTypes.INDIRECT_MAGIC)) && this.magicDamageBonus > 0d) {
			double missingHealth = wither.getMaxHealth() - wither.getHealth();
			event.setAmount((event.getAmount() * (float) (missingHealth / (this.magicDamageBonus) + 1)));
		}

		if (event.getSource().getAttacker() != event.getSource().getSource())
			return;

		NbtCompound tags = ((IEntityExtraData) wither).getPersistentData();
		float difficulty = tags.getFloat(Strings.Tags.DIFFICULTY);
		//Handle Damage Reduction
		float damageReduction;
		if (!wither.shouldRenderOverlay())
			damageReduction = (float) Math.min(this.maxMeleeDamageReductionBeforeHalfHealth, difficulty * this.meleeDamageReductionBeforeHalfHealth);
		else
			damageReduction = (float) Math.min(this.maxDamageReductionOnHalfHealth, difficulty * this.meleeDamageReductionOnHalfHealth);

		if (damageReduction == 0d)
			return;

		event.setAmount(event.getAmount() * (1f - damageReduction));
	}
}
