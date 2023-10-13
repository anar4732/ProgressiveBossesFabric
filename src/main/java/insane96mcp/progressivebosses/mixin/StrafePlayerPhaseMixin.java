package insane96mcp.progressivebosses.mixin;

import insane96mcp.progressivebosses.module.Modules;
import insane96mcp.progressivebosses.utils.LogHelper;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.enderdragon.phases.AbstractDragonPhaseInstance;
import net.minecraft.world.entity.boss.enderdragon.phases.DragonPhaseInstance;
import net.minecraft.world.entity.boss.enderdragon.phases.DragonStrafePlayerPhase;
import net.minecraft.world.entity.boss.enderdragon.phases.EnderDragonPhase;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(DragonStrafePlayerPhase.class)
public abstract class StrafePlayerPhaseMixin extends AbstractDragonPhaseInstance {
	@Shadow
	private int seenTargetTimes;
	@Shadow
	private Path path;
	@Shadow
	private Vec3 pathTarget;
	@Shadow
	private LivingEntity target;

	public StrafePlayerPhaseMixin(EnderDragon dragonIn) {
		super(dragonIn);
	}

	@Override
	public void doServerTick() {
		if (this.target == null) {
			LogHelper.warn("Skipping player strafe phase because no player was found");
			this.dragon.getPhaseManager().setPhase(EnderDragonPhase.HOLDING_PATTERN);
		}
		else if (this.target.distanceToSqr(this.dragon) < 256d) {
			LogHelper.warn("Skipping player strafe phase because too near the target");
			this.dragon.getPhaseManager().setPhase(EnderDragonPhase.HOLDING_PATTERN);
		}
		else {
			if (this.path != null && this.path.isDone()) {
				double d0 = this.target.getX();
				double d1 = this.target.getZ();
				double d2 = d0 - this.dragon.getX();
				double d3 = d1 - this.dragon.getZ();
				double d4 = Mth.sqrt((float) (d2 * d2 + d3 * d3));
				double d5 = Math.min((double)0.4F + d4 / 80.0D - 1.0D, 10.0D);
				this.pathTarget = new Vec3(d0, this.target.getY() + d5, d1);
			}

			double d12 = this.pathTarget == null ? 0.0D : this.pathTarget.distanceToSqr(this.dragon.getX(), this.dragon.getY(), this.dragon.getZ());
			if (d12 < 100.0D || d12 > 22500.0D) {
				this.updatePath();
			}

			if (this.target.distanceToSqr(this.dragon) < 9216d) {
				if (this.dragon.hasLineOfSight(this.target)) {
					++this.seenTargetTimes;
					Vec3 vector3d1 = (new Vec3(this.target.getX() - this.dragon.getX(), 0.0D, this.target.getZ() - this.dragon.getZ())).normalize();
					Vec3 vector3d = (new Vec3(Mth.sin(this.dragon.getRotationVector().y * ((float)Math.PI / 180F)), 0.0D, -Mth.cos(this.dragon.getRotationVector().y * ((float)Math.PI / 180F)))).normalize();
					float f1 = (float)vector3d.dot(vector3d1);
					float f = (float)(Math.acos(f1) * (double)(180F / (float)Math.PI));
					f = f + 0.5F;
					if (this.seenTargetTimes >= 5 && f >= 0.0F && f < 10.0F) {
						Modules.dragon.attack.fireFireball(this.dragon, this.target);
						this.seenTargetTimes = 0;
						if (this.path != null) {
							while(!this.path.isDone()) {
								this.path.advance();
							}
						}

						//If must not charge or fireball then go back to holding pattern
						if (!Modules.dragon.attack.onPhaseEnd(this.dragon))
							this.dragon.getPhaseManager().setPhase(EnderDragonPhase.HOLDING_PATTERN);
						//Otherwise reset the phase, in case she fireballs again
						else
							//Can't use initPhase() otherwise the target is reset. Also making the dragon fire slower when chaining fireballs
							this.seenTargetTimes = -10;
					}
				}
				else if (this.seenTargetTimes > 0) {
					--this.seenTargetTimes;
				}
			}
			else if (this.seenTargetTimes > 0) {
				--this.seenTargetTimes;
			}

		}
	}

	@Shadow
	private void updatePath() {}

	@Shadow public abstract void begin();

	@Override
	public EnderDragonPhase<? extends DragonPhaseInstance> getPhase() { return EnderDragonPhase.STRAFE_PLAYER; }
}