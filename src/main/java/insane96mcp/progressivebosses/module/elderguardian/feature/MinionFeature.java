package insane96mcp.progressivebosses.module.elderguardian.feature;

import insane96mcp.progressivebosses.module.elderguardian.ai.ElderMinionNearestAttackableTargetGoal;
import insane96mcp.progressivebosses.utils.*;
import me.lortseam.completeconfig.api.ConfigEntries;
import me.lortseam.completeconfig.api.ConfigEntry;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.WrappedGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.ElderGuardian;
import net.minecraft.world.entity.monster.Guardian;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import java.util.ArrayList;
import java.util.List;

@ConfigEntries(includeAll = true)
@Label(name = "Minions", description = "Elder Guardians will spawn Elder Minions.")
public class MinionFeature implements LabelConfigGroup {

	@ConfigEntry(nameKey = "Base Cooldown", comment = "Elder Guardians will spawn Elder Minions every this tick value (20 ticks = 1 sec).")
	@ConfigEntry.BoundedInteger(min = 0, max = Integer.MAX_VALUE)
	public int baseCooldown = 200;

	@ConfigEntry(nameKey = "Cooldown Reduction per Missing Elder", comment = "The base cooldown is reduced by this value for each missing Elder Guardian.")
	@ConfigEntry.BoundedInteger(min = 0, max = Integer.MAX_VALUE)
	public int cooldownReductionPerMissingGuardian = 60;

	public MinionFeature(LabelConfigGroup parent) {
		parent.addConfigContainer(this);
		ServerEntityEvents.ENTITY_LOAD.register((entity, world) -> this.onElderGuardianSpawn(new DummyEvent(world, entity)));
		LivingEntityEvents.TICK.register((entity) -> this.update(new DummyEvent(entity.level, entity)));
	}

	public void onElderGuardianSpawn(DummyEvent event) {
		if (event.getWorld().isClientSide)
			return;

		if (!(event.getEntity() instanceof ElderGuardian))
			return;

		ElderGuardian elderGuardian = (ElderGuardian) event.getEntity();

		CompoundTag witherTags = ((IEntityExtraData) elderGuardian).getPersistentData();

		witherTags.putInt(Strings.Tags.ELDER_MINION_COOLDOWN, this.baseCooldown);
	}

	public void update(DummyEvent event) {
		if (event.getEntity().level.isClientSide)
			return;

		if (!(event.getEntity() instanceof ElderGuardian))
			return;

		Level world = event.getEntity().level;

		ElderGuardian elderGuardian = (ElderGuardian) event.getEntity();
		CompoundTag elderGuardianTags = ((IEntityExtraData) elderGuardian).getPersistentData();

		if (elderGuardian.getHealth() <= 0)
			return;
		int cooldown = elderGuardianTags.getInt(Strings.Tags.ELDER_MINION_COOLDOWN);
		if (cooldown > 0) {
			elderGuardianTags.putInt(Strings.Tags.ELDER_MINION_COOLDOWN, cooldown - 1);
			return;
		}
		elderGuardianTags.putInt(Strings.Tags.ELDER_MINION_COOLDOWN, this.baseCooldown - (this.cooldownReductionPerMissingGuardian * BaseFeature.getDeadElderGuardians(elderGuardian)));

		//If there is no player in a radius from the elderGuardian, don't spawn minions
		int radius = 24;
		BlockPos pos1 = elderGuardian.blockPosition().offset(-radius, -radius, -radius);
		BlockPos pos2 = elderGuardian.blockPosition().offset(radius, radius, radius);
		AABB bb = new AABB(pos1, pos2);
		List<ServerPlayer> players = world.getEntitiesOfClass(ServerPlayer.class, bb);

		if (players.isEmpty())
			return;

		List<Guardian> minionsInAABB = world.getEntitiesOfClass(Guardian.class, elderGuardian.getBoundingBox().inflate(12), entity -> ((IEntityExtraData) entity).getPersistentData().contains(Strings.Tags.ELDER_MINION));
		int minionsCountInAABB = minionsInAABB.size();

		if (minionsCountInAABB >= 5)
			return;

		summonMinion(world, new Vec3(elderGuardian.getX(), elderGuardian.getY(), elderGuardian.getZ()));
	}

	public Guardian summonMinion(Level world, Vec3 pos) {
		Guardian elderMinion = new Guardian(EntityType.GUARDIAN, world);
		CompoundTag minionTags = ((IEntityExtraData) elderMinion).getPersistentData();

		minionTags.putBoolean("mobspropertiesrandomness:processed", true);
		//TODO Scaling health

		minionTags.putBoolean(Strings.Tags.ELDER_MINION, true);

		elderMinion.setPos(pos.x, pos.y, pos.z);
		elderMinion.setCustomName(MutableComponent.create(new TranslatableContents(Strings.Translatable.ELDER_MINION)));
		elderMinion.lootTable = BuiltInLootTables.EMPTY;

		MCUtils.applyModifier(elderMinion, Attributes.MOVEMENT_SPEED, Strings.AttributeModifiers.SWIM_SPEED_BONUS_UUID, Strings.AttributeModifiers.SWIM_SPEED_BONUS, 2d, AttributeModifier.Operation.MULTIPLY_BASE);

		ArrayList<Goal> goalsToRemove = new ArrayList<>();
		for (WrappedGoal prioritizedGoal : elderMinion.targetSelector.availableGoals) {
			if (!(prioritizedGoal.getGoal() instanceof NearestAttackableTargetGoal))
				continue;

			goalsToRemove.add(prioritizedGoal.getGoal());
		}

		goalsToRemove.forEach(elderMinion.goalSelector::removeGoal);
		elderMinion.targetSelector.addGoal(1, new ElderMinionNearestAttackableTargetGoal<>(elderMinion, Player.class, true));

		world.addFreshEntity(elderMinion);
		return elderMinion;
	}
}