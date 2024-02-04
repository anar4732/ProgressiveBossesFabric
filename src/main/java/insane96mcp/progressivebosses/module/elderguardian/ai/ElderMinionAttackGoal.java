package insane96mcp.progressivebosses.module.elderguardian.ai;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSources;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.entity.mob.ElderGuardianEntity;
import net.minecraft.entity.mob.GuardianEntity;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.world.Difficulty;
import java.util.EnumSet;

public class ElderMinionAttackGoal extends Goal {
	private final GuardianEntity guardian;
	private int attackTime;
	private final boolean elder;

	public ElderMinionAttackGoal(GuardianEntity p_i45833_1_) {
		this.guardian = p_i45833_1_;
		this.elder = p_i45833_1_ instanceof ElderGuardianEntity;
		this.setControls(EnumSet.of(Goal.Control.MOVE, Goal.Control.LOOK));
	}

	/**
	 * Returns whether execution should begin. You can also read and cache any state necessary for execution in this
	 * method as well.
	 */
	public boolean canStart() {
		LivingEntity livingentity = this.guardian.getTarget();
		return livingentity != null && livingentity.isAlive();
	}

	/**
	 * Returns whether an in-progress EntityAIBase should continue executing
	 */
	public boolean shouldContinue() {
		return super.shouldContinue() && (this.elder || this.guardian.squaredDistanceTo(this.guardian.getTarget()) > 9.0D);
	}

	/**
	 * Execute a one shot task or start executing a continuous task
	 */
	public void start() {
		this.attackTime = -10;
		this.guardian.getNavigation().stop();
		this.guardian.getLookControl().lookAt(this.guardian.getTarget(), 90.0F, 90.0F);
		this.guardian.velocityDirty = true;
	}

	/**
	 * Reset the task's internal state. Called when this task is interrupted by another one
	 */
	public void stop() {
		this.guardian.setBeamTarget(0);
		this.guardian.setTarget(null);
	}

	/**
	 * Keep ticking a continuous task that has already been started
	 */
	public void tick() {
		LivingEntity livingentity = this.guardian.getTarget();
		this.guardian.getNavigation().stop();
		this.guardian.getLookControl().lookAt(livingentity, 90.0F, 90.0F);
		++this.attackTime;
		if (this.attackTime == 0) {
			this.guardian.setBeamTarget(this.guardian.getTarget().getId());
			if (!this.guardian.isSilent()) {
				this.guardian.getWorld().sendEntityStatus(this.guardian, (byte)21);
			}
		} else if (this.attackTime >= this.guardian.getWarmupTime()) {
			float f = 1.0F;
			if (this.guardian.getWorld().getDifficulty() == Difficulty.HARD) {
				f += 2.0F;
			}

			if (this.elder) {
				f += 2.0F;
			}

			livingentity.damage(new DamageSource(RegistryEntry.of(this.guardian.getWorld().getDamageSources().registry.get(DamageTypes.MAGIC)),this.guardian, this.guardian), f);
			livingentity.damage(new DamageSource(RegistryEntry.of(this.guardian.getWorld().getDamageSources().registry.get(DamageTypes.MOB_ATTACK)),this.guardian), (float)this.guardian.getAttributeValue(EntityAttributes.GENERIC_ATTACK_DAMAGE));
			this.guardian.setTarget(null);
		}

		super.tick();
	}
}