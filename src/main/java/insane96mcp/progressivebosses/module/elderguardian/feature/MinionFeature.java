package insane96mcp.progressivebosses.module.elderguardian.feature;

import java.util.ArrayList;
import java.util.List;

import insane96mcp.progressivebosses.module.elderguardian.ai.ElderMinionNearestAttackableTargetGoal;
import insane96mcp.progressivebosses.utils.DummyEvent;
import insane96mcp.progressivebosses.utils.IEntityExtraData;
import insane96mcp.progressivebosses.utils.Label;
import insane96mcp.progressivebosses.utils.LabelConfigGroup;
import insane96mcp.progressivebosses.utils.LivingEntityEvents;
import insane96mcp.progressivebosses.utils.MCUtils;
import insane96mcp.progressivebosses.utils.Strings;
import me.lortseam.completeconfig.api.ConfigEntries;
import me.lortseam.completeconfig.api.ConfigEntry;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ai.goal.ActiveTargetGoal;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.ai.goal.PrioritizedGoal;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.mob.ElderGuardianEntity;
import net.minecraft.entity.mob.GuardianEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.loot.LootTables;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.MutableText;
import net.minecraft.text.TranslatableTextContent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

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
		LivingEntityEvents.TICK.register((entity) -> this.update(new DummyEvent(entity.getWorld(), entity)));
	}

	public void onElderGuardianSpawn(DummyEvent event) {
		if (event.getWorld().isClient)
			return;

		if (!(event.getEntity() instanceof ElderGuardianEntity))
			return;

		ElderGuardianEntity elderGuardian = (ElderGuardianEntity) event.getEntity();

		NbtCompound witherTags = ((IEntityExtraData) elderGuardian).getPersistentData();

		witherTags.putInt(Strings.Tags.ELDER_MINION_COOLDOWN, this.baseCooldown);
	}

	public void update(DummyEvent event) {
		if (event.getEntity().getWorld().isClient)
			return;

		if (!(event.getEntity() instanceof ElderGuardianEntity))
			return;

		World world = event.getEntity().getWorld();

		ElderGuardianEntity elderGuardian = (ElderGuardianEntity) event.getEntity();
		NbtCompound elderGuardianTags = ((IEntityExtraData) elderGuardian).getPersistentData();

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
		BlockPos pos1 = elderGuardian.getBlockPos().add(-radius, -radius, -radius);
		BlockPos pos2 = elderGuardian.getBlockPos().add(radius, radius, radius);
		Box bb = new Box(pos1, pos2);
		List<ServerPlayerEntity> players = world.getNonSpectatingEntities(ServerPlayerEntity.class, bb);

		if (players.isEmpty())
			return;

		List<GuardianEntity> minionsInAABB = world.getEntitiesByClass(GuardianEntity.class, elderGuardian.getBoundingBox().expand(12), entity -> ((IEntityExtraData) entity).getPersistentData().contains(Strings.Tags.ELDER_MINION));
		int minionsCountInAABB = minionsInAABB.size();

		if (minionsCountInAABB >= 5)
			return;

		summonMinion(world, new Vec3d(elderGuardian.getX(), elderGuardian.getY(), elderGuardian.getZ()));
	}

	public GuardianEntity summonMinion(World world, Vec3d pos) {
		GuardianEntity elderMinion = new GuardianEntity(EntityType.GUARDIAN, world);
		NbtCompound minionTags = ((IEntityExtraData) elderMinion).getPersistentData();

		minionTags.putBoolean("mobspropertiesrandomness:processed", true);
		//TODO Scaling health

		minionTags.putBoolean(Strings.Tags.ELDER_MINION, true);

		elderMinion.setPosition(pos.x, pos.y, pos.z);
		elderMinion.setCustomName(MutableText.of(new TranslatableTextContent(Strings.Translatable.ELDER_MINION, "translate error at ELDER_MINION", new Object[]{})));
		elderMinion.lootTable = LootTables.EMPTY;

		MCUtils.applyModifier(elderMinion, EntityAttributes.GENERIC_MOVEMENT_SPEED, Strings.AttributeModifiers.SWIM_SPEED_BONUS_UUID, Strings.AttributeModifiers.SWIM_SPEED_BONUS, 2d, EntityAttributeModifier.Operation.MULTIPLY_BASE);

		ArrayList<Goal> goalsToRemove = new ArrayList<>();
		for (PrioritizedGoal prioritizedGoal : elderMinion.targetSelector.goals) {
			if (!(prioritizedGoal.getGoal() instanceof ActiveTargetGoal))
				continue;

			goalsToRemove.add(prioritizedGoal.getGoal());
		}

		goalsToRemove.forEach(elderMinion.goalSelector::remove);
		elderMinion.targetSelector.add(1, new ElderMinionNearestAttackableTargetGoal<>(elderMinion, PlayerEntity.class, true));

		world.spawnEntity(elderMinion);
		return elderMinion;
	}
}
