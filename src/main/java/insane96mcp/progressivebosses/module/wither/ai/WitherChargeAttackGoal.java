package insane96mcp.progressivebosses.module.wither.ai;

import java.util.EnumSet;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;

import com.mojang.datafixers.util.Pair;

import insane96mcp.progressivebosses.module.wither.feature.AttackFeature;
import insane96mcp.progressivebosses.utils.IEntityExtraData;
import insane96mcp.progressivebosses.utils.Strings;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.boss.WitherEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.item.ItemStack;
import net.minecraft.loot.context.LootContext;
import net.minecraft.loot.context.LootContextParameterSet;
import net.minecraft.loot.context.LootContextParameters;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;

public class WitherChargeAttackGoal extends Goal {
	private final WitherEntity wither;
	private LivingEntity target;
	private Vec3d targetPos;
	private double lastDistanceFromTarget = 0d;

	public WitherChargeAttackGoal(WitherEntity wither) {
		this.wither = wither;
		this.setControls(EnumSet.of(Goal.Control.MOVE, Goal.Control.JUMP, Goal.Control.LOOK, Control.TARGET));
	}

	/**
	 * Returns whether execution should begin. You can also read and cache any state necessary for execution in this
	 * method as well.
	 */
	public boolean canStart() {
		byte chargeTick = ((IEntityExtraData) wither).getPersistentData().getByte(Strings.Tags.CHARGE_ATTACK);
		return chargeTick > 0;
	}

	public void start() {
		this.wither.getNavigation().stop();
		for (int h = 0; h < 3; h++)
			this.wither.setTrackedEntityId(h, 0);

		this.wither.getWorld().playSound(null, this.wither.getBlockPos(), SoundEvents.ENTITY_WITHER_DEATH, SoundCategory.HOSTILE, 5.0f, 2.0f);
	}

	/**
	 * Returns whether an in-progress EntityAIBase should continue executing
	 */
	public boolean shouldContinue() {
		return ((IEntityExtraData) wither).getPersistentData().getByte(Strings.Tags.CHARGE_ATTACK) > 0;
	}

	/**
	 * Reset the task's internal state. Called when this task is interrupted by another one
	 */
	public void stop() {
		this.target = null;
		//AttackFeature.setCharging(this.wither, false);
		this.wither.setVelocity(this.wither.getVelocity().multiply(0.02d, 0.02d, 0.02d));
		this.lastDistanceFromTarget = 0d;
		this.targetPos = null;

		for(Pair<ItemStack, BlockPos> pair : blocksToDrop) {
			Block.dropStack(this.wither.getWorld(), pair.getSecond(), pair.getFirst());
		}
	}

	ObjectArrayList<Pair<ItemStack, BlockPos>> blocksToDrop = new ObjectArrayList<>();

	/**
	 * Keep ticking a continuous task that has already been started
	 */
	public void tick() {
		byte chargeTick = ((IEntityExtraData) wither).getPersistentData().getByte(Strings.Tags.CHARGE_ATTACK);
		//Needed since stop() now gets called every other tick
		if (chargeTick <= 0) {
			this.stop();
			return;
		}

		if (chargeTick > AttackFeature.Consts.CHARGE_ATTACK_TICK_CHARGE)
			this.wither.setVelocity(Vec3d.ZERO);

		if (chargeTick == AttackFeature.Consts.CHARGE_ATTACK_TICK_CHARGE) {
			this.target = this.wither.getWorld().getClosestPlayer(this.wither.getX(), this.wither.getY(), this.wither.getZ(), 64d, true);
			if (target != null) {
				this.targetPos = this.target.getPos().add(0, -1.5d, 0);
				Vec3d forward = this.targetPos.subtract(this.wither.getPos()).normalize();
				this.targetPos = this.targetPos.add(forward.multiply(4d, 4d, 4d));
				this.lastDistanceFromTarget = this.targetPos.squaredDistanceTo(this.wither.getPos());
				this.wither.getWorld().playSound(null, BlockPos.ofFloored(this.targetPos), SoundEvents.ENTITY_WITHER_SPAWN, SoundCategory.HOSTILE, 4.0f, 2.0f);
			}
			else {
				AttackFeature.stopCharging(this.wither);
			}
		}
		else if (chargeTick < AttackFeature.Consts.CHARGE_ATTACK_TICK_CHARGE) {
			if (this.targetPos == null) {
				AttackFeature.stopCharging(this.wither);
				return;
			}
			//So it goes faster and faster
			double mult = 60d / chargeTick;
			Vec3d diff = this.targetPos.subtract(this.wither.getPos()).normalize().multiply(mult, mult, mult);
			this.wither.setVelocity(diff.x, diff.y * 0.5, diff.z);
			this.wither.getLookControl().lookAt(this.targetPos);
			Box axisAlignedBB = new Box(this.wither.getX() - 2, this.wither.getY() - 2, this.wither.getZ() - 2, this.wither.getX() + 2, this.wither.getY() + 6, this.wither.getZ() + 2);
			Stream<BlockPos> blocks = BlockPos.stream(axisAlignedBB);
			AtomicBoolean hasBrokenBlocks = new AtomicBoolean(false);
			blocks.forEach(blockPos -> {
				BlockState state = wither.getWorld().getBlockState(blockPos);
				// if (state.canEntityDestroy(wither.world, blockPos, wither) && net.minecraftforge.event.ForgeEventFactory.onEntityDestroyBlock(wither, blockPos, state) && !state.getBlock().equals(Blocks.AIR)) {
				BlockEntity tileentity = state.hasBlockEntity() ? this.wither.getWorld().getBlockEntity(blockPos) : null;

				var lootcontext$builder = new LootContextParameterSet.Builder((ServerWorld) this.wither.getWorld())
						.add(LootContextParameters.ORIGIN, Vec3d.ofCenter(blockPos))
						.add(LootContextParameters.TOOL, ItemStack.EMPTY)
						.addOptional(LootContextParameters.BLOCK_ENTITY, tileentity);
				state.getDroppedStacks(lootcontext$builder).forEach(itemStack -> {
					addBlockDrops(blocksToDrop, itemStack, blockPos);
				});

				wither.getWorld().setBlockState(blockPos, Blocks.AIR.getDefaultState());
				hasBrokenBlocks.set(true);
				// }
			});

			if (hasBrokenBlocks.get() && this.wither.age % 2 == 0)
				this.wither.getWorld().playSound(null, BlockPos.ofFloored(this.targetPos), SoundEvents.ENTITY_WITHER_BREAK_BLOCK, SoundCategory.HOSTILE, 1.0f, 0.75f);

			axisAlignedBB = axisAlignedBB.expand(1d);
			this.wither.getWorld().getNonSpectatingEntities(LivingEntity.class, axisAlignedBB).forEach(entity -> {
				if (entity == this.wither)
					return;
				entity.damage(new DamageSource(RegistryEntry.of(entity.getWorld().getDamageSources().registry.get(DamageTypes.WITHER)), this.wither), 16f);
				double d2 = entity.getX() - this.wither.getX();
				double d3 = entity.getZ() - this.wither.getZ();
				double d4 = Math.max(d2 * d2 + d3 * d3, 0.1D);
				entity.addVelocity(d2 / d4 * 20d, 0.7d, d3 / d4 * 20d);
			});
		}
		//If the wither's charging and is farther from the target point than the last tick OR is about to finish the invulnerability time then prevent the explosion and stop the attack
		if ((chargeTick < AttackFeature.Consts.CHARGE_ATTACK_TICK_CHARGE && (this.targetPos.squaredDistanceTo(this.wither.getPos()) - this.lastDistanceFromTarget > 16d || this.targetPos.squaredDistanceTo(this.wither.getPos()) < 4d)) || chargeTick == 1) {
			AttackFeature.stopCharging(this.wither);
		}
		if (this.targetPos != null)
			this.lastDistanceFromTarget = this.targetPos.squaredDistanceTo(this.wither.getPos());
	}

	@Override
	public boolean shouldRunEveryTick() {
		return true;
	}

	private static void addBlockDrops(ObjectArrayList<Pair<ItemStack, BlockPos>> p_46068_, ItemStack p_46069_, BlockPos p_46070_) {
		int i = p_46068_.size();

		for(int j = 0; j < i; ++j) {
			Pair<ItemStack, BlockPos> pair = p_46068_.get(j);
			ItemStack itemstack = pair.getFirst();
			if (ItemEntity.canMerge(itemstack, p_46069_)) {
				ItemStack itemstack1 = ItemEntity.merge(itemstack, p_46069_, 16);
				p_46068_.set(j, Pair.of(itemstack1, pair.getSecond()));
				if (p_46069_.isEmpty()) {
					return;
				}
			}
		}

		p_46068_.add(Pair.of(p_46069_, p_46070_));
	}
}