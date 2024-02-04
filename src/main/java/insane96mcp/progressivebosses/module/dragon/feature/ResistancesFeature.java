package insane96mcp.progressivebosses.module.dragon.feature;

import java.util.Arrays;
import java.util.List;

import insane96mcp.progressivebosses.utils.IEntityExtraData;
import insane96mcp.progressivebosses.utils.Label;
import insane96mcp.progressivebosses.utils.LabelConfigGroup;
import insane96mcp.progressivebosses.utils.LivingEntityEvents;
import insane96mcp.progressivebosses.utils.LivingEntityEvents.OnLivingHurtEvent;
import insane96mcp.progressivebosses.utils.Strings;
import me.lortseam.completeconfig.api.ConfigEntries;
import me.lortseam.completeconfig.api.ConfigEntry;
import net.minecraft.entity.boss.dragon.EnderDragonEntity;
import net.minecraft.entity.boss.dragon.phase.Phase;
import net.minecraft.entity.boss.dragon.phase.PhaseType;
import net.minecraft.entity.damage.DamageType;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;

@ConfigEntries(includeAll = true)
@Label(name = "Resistances & Vulnerabilities", description = "Handles the Damage Resistances and Vulnerabilities")
public class ResistancesFeature implements LabelConfigGroup {

	@ConfigEntry(nameKey = "Melee Damage reduction while at the center", comment = "Melee Damage reduction per difficulty while the Ender Dragon is at the center.")
	@ConfigEntry.BoundedDouble(min = 0d, max = Double.MAX_VALUE)
	public double damageRedutionWhenSitting = 0.08d;

	@ConfigEntry(nameKey = "Explosion Damage reduction", comment = "Damage reduction when hit by explosions (firework rockets excluded).")
	@ConfigEntry.BoundedDouble(min = 0d, max = Double.MAX_VALUE)
	public double explosionDamageReduction = 0.667d;

	public ResistancesFeature(LabelConfigGroup parent) {
		parent.addConfigContainer(this);
		LivingEntityEvents.HURT.register((event) -> this.onDragonDamage(event));
	}

	public void onDragonDamage(OnLivingHurtEvent event) {
		if (!(event.getEntity() instanceof EnderDragonEntity dragon))
			return;

		meleeDamageReduction(event, dragon);
		explosionDamageReduction(event, dragon);
	}

	@ConfigEntries.Exclude
	private static final List<PhaseType<? extends Phase>> sittingPhases = Arrays.asList(PhaseType.SITTING_SCANNING, PhaseType.SITTING_ATTACKING, PhaseType.SITTING_FLAMING, PhaseType.TAKEOFF);

	private void meleeDamageReduction(OnLivingHurtEvent event, EnderDragonEntity dragon) {
		if (this.damageRedutionWhenSitting == 0d)
			return;

		NbtCompound compoundNBT = ((IEntityExtraData) dragon).getPersistentData();
		float difficulty = compoundNBT.getFloat(Strings.Tags.DIFFICULTY);

		if (sittingPhases.contains(dragon.getPhaseManager().getCurrent().getType()) && event.getSource().getSource() instanceof PlayerEntity) {
			event.setAmount((event.getAmount() - (float) (event.getAmount() * (this.damageRedutionWhenSitting * difficulty))));
		}
	}

	private void explosionDamageReduction(OnLivingHurtEvent event, EnderDragonEntity dragon) {
		if (this.explosionDamageReduction == 0d)
			return;

		if (event.getSource().isOf(DamageTypes.EXPLOSION) && !event.getSource().getName().equals("fireworks")) {
			event.setAmount((event.getAmount() - (float) (event.getAmount() * this.explosionDamageReduction)));
		}
	}
}
