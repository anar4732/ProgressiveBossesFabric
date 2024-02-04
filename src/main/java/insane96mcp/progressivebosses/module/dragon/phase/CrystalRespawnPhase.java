package insane96mcp.progressivebosses.module.dragon.phase;

import insane96mcp.progressivebosses.module.dragon.feature.CrystalFeature;
import insane96mcp.progressivebosses.utils.LogHelper;
import net.minecraft.entity.boss.dragon.EnderDragonEntity;
import net.minecraft.entity.boss.dragon.phase.AbstractPhase;
import net.minecraft.entity.boss.dragon.phase.PhaseType;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.gen.feature.EndSpikeFeature;

import org.jetbrains.annotations.Nullable;
import java.util.ArrayList;

public class CrystalRespawnPhase extends AbstractPhase {
	private static PhaseType<CrystalRespawnPhase> CRYSTAL_RESPAWN;

	public Vec3d targetLocation;
	private int tick = 0;
	private boolean respawning = false;
	private final ArrayList<EndSpikeFeature.Spike> spikesToRespawn = new ArrayList<>();

	private final int TICK_RESPAWN_CRYSTAL = 50;

	public CrystalRespawnPhase(EnderDragonEntity dragonIn) {
		super(dragonIn);
	}

	/**
	 * Gives the phase a chance to update its status.
	 * Called by dragon's onLivingUpdate. Only used when !worldObj.isClientSide.
	 */
	public void serverTick() {
		if (this.targetLocation == null) {
			if (spikesToRespawn.isEmpty()) {
				dragon.getPhaseManager().setPhase(PhaseType.TAKEOFF);
				LogHelper.warn("Canceling Crystal respawn phase because no spikes to respawn were found");
				return;
			}
			this.targetLocation = new Vec3d(spikesToRespawn.get(0).getCenterX() + 0.5, spikesToRespawn.get(0).getHeight() + 5.5, spikesToRespawn.get(0).getCenterZ() + 0.5);
		}
		if (!respawning) {
			double d0 = this.targetLocation.squaredDistanceTo(dragon.getX(), dragon.getY(), dragon.getZ());
			if (d0 < 9d) { //sqrt = 3
				dragon.setVelocity(Vec3d.ZERO);
				respawning = true;
			}
		}
		else {
			tick++;
			dragon.setVelocity(Vec3d.ZERO);
			if (tick <= 25)
				dragon.playSound(SoundEvents.ENTITY_ENDER_DRAGON_GROWL, 4F, 1.0F);
			if (tick >= TICK_RESPAWN_CRYSTAL) {
				double x = spikesToRespawn.get(0).getCenterX();
				double y = spikesToRespawn.get(0).getHeight();
				double z = spikesToRespawn.get(0).getCenterZ();
				EndCrystalEntity crystal = new EndCrystalEntity(dragon.getWorld(), x + 0.5, y + 1, z + 0.5);
				crystal.setShowBottom(true);
				crystal.getWorld().createExplosion(dragon, x + 0.5, y + 1.5, z + 0.5, 5f, World.ExplosionSourceType.MOB);
				dragon.getWorld().spawnEntity(crystal);
				CrystalFeature.generateCage(crystal.getWorld(), crystal.getBlockPos());
				spikesToRespawn.remove(0);
				if (this.spikesToRespawn.size() == 0)
				LogHelper.info("No more crystals to respawn left");
				tick = 0;
				respawning = false;
				this.targetLocation = null;
			}
		}
	}

	public boolean isSittingOrHovering() {
		return respawning;
	}

	/**
	 * Called when this phase is set to active
	 */
	public void beginPhase() {
		this.targetLocation = null;
		this.spikesToRespawn.clear();
	}

	/**
	 * Returns the maximum amount dragon may rise or fall during this phase
	 */
	public float getMaxYAcceleration() {
		return 24F;
	}

	/**
	 * Returns the location the dragon is flying toward
	 */
	@Nullable
	public Vec3d getPathTarget() {
		return this.targetLocation;
	}

	public void addCrystalRespawn(EndSpikeFeature.Spike spike) {
		if (!this.spikesToRespawn.contains(spike))
			this.spikesToRespawn.add(spike);
	}

	@Override
	public float modifyDamageTaken(DamageSource source, float amount) {
		if (source.isOf(DamageTypes.EXPLOSION) && !source.getName().equals("fireworks"))
			return amount;

		return amount * 1.33f;
	}

	public PhaseType<CrystalRespawnPhase> getType() {
		return CRYSTAL_RESPAWN;
	}

	public static PhaseType<CrystalRespawnPhase> getPhaseType() {
		return CRYSTAL_RESPAWN;
	}

	public static void init() {
		CRYSTAL_RESPAWN = PhaseType.register(CrystalRespawnPhase.class, "CrystalRespawn");
	}
}