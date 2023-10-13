package insane96mcp.progressivebosses.module.dragon.feature;

import insane96mcp.progressivebosses.utils.*;
import insane96mcp.progressivebosses.utils.LivingEntityEvents.OnLivingHurtEvent;
import me.lortseam.completeconfig.api.ConfigEntries;
import me.lortseam.completeconfig.api.ConfigEntry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.enderdragon.phases.DragonPhaseInstance;
import net.minecraft.world.entity.boss.enderdragon.phases.EnderDragonPhase;
import net.minecraft.world.entity.player.Player;
import java.util.Arrays;
import java.util.List;

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
		if (!(event.getEntity() instanceof EnderDragon dragon))
			return;

		meleeDamageReduction(event, dragon);
		explosionDamageReduction(event, dragon);
	}

	@ConfigEntries.Exclude
	private static final List<EnderDragonPhase<? extends DragonPhaseInstance>> sittingPhases = Arrays.asList(EnderDragonPhase.SITTING_SCANNING, EnderDragonPhase.SITTING_ATTACKING, EnderDragonPhase.SITTING_FLAMING, EnderDragonPhase.TAKEOFF);

	private void meleeDamageReduction(OnLivingHurtEvent event, EnderDragon dragon) {
		if (this.damageRedutionWhenSitting == 0d)
			return;

		CompoundTag compoundNBT = ((IEntityExtraData) dragon).getPersistentData();
		float difficulty = compoundNBT.getFloat(Strings.Tags.DIFFICULTY);

		if (sittingPhases.contains(dragon.getPhaseManager().getCurrentPhase().getPhase()) && event.getSource().getDirectEntity() instanceof Player) {
			event.setAmount((event.getAmount() - (float) (event.getAmount() * (this.damageRedutionWhenSitting * difficulty))));
		}
	}

	private void explosionDamageReduction(OnLivingHurtEvent event, EnderDragon dragon) {
		if (this.explosionDamageReduction == 0d)
			return;

		if (event.getSource().isExplosive() && !event.getSource().getMsgId().equals("fireworks")) {
			event.setAmount((event.getAmount() - (float) (event.getAmount() * this.explosionDamageReduction)));
		}
	}
}