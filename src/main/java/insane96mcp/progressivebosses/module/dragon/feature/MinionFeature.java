package insane96mcp.progressivebosses.module.dragon.feature;

import insane96mcp.progressivebosses.module.dragon.ai.DragonMinionAttackGoal;
import insane96mcp.progressivebosses.module.dragon.ai.PBNearestAttackableTargetGoal;
import insane96mcp.progressivebosses.utils.*;
import insane96mcp.progressivebosses.utils.LivingEntityEvents.OnLivingHurtEvent;
import me.lortseam.completeconfig.api.ConfigEntries;
import me.lortseam.completeconfig.api.ConfigEntry;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.monster.Shulker;
import net.minecraft.world.entity.projectile.ShulkerBullet;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.EndPodiumFeature;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import java.util.ArrayList;
import java.util.List;

@ConfigEntries(includeAll = true)
@Label(name = "Minions", description = "Shulkers that will make you float around.")
public class MinionFeature implements LabelConfigGroup {

	@ConfigEntry(nameKey = "Minion at Difficulty", comment = "At which difficulty the Ender Dragon starts spawning Minions")
	@ConfigEntry.BoundedInteger(min = 0, max = Integer.MAX_VALUE)
	public int minionAtDifficulty = 1;

	@ConfigEntry(nameKey = "Minimum Cooldown", comment = "Minimum ticks (20 ticks = 1 seconds) after Minions can spwan.")
	@ConfigEntry.BoundedInteger(min = 0, max = Integer.MAX_VALUE)
	public int minCooldown = 1400;

	@ConfigEntry(nameKey = "Maximum Cooldown", comment = "Maximum ticks (20 ticks = 1 seconds) after Minions can spwan.")
	@ConfigEntry.BoundedInteger(min = 0, max = Integer.MAX_VALUE)
	public int maxCooldown = 2000;

	@ConfigEntry(nameKey = "Cooldown Reduction", comment = "Percentage cooldown reduction per difficulty for the cooldown of Minion spawning.")
	@ConfigEntry.BoundedDouble(min = 0d, max = 1d)
	public double cooldownReduction = 0.05d;

	@ConfigEntry(nameKey = "Blinding Chance", comment = "Percentage chance per difficulty for a Minion to spawn as a Blinding Minion.")
	@ConfigEntry.BoundedDouble(min = 0d, max = 1d)
	public double blindingChance = 0.05d;

	@ConfigEntry(nameKey = "Blinding duration", comment = "Time (in ticks) for the bliding effect when hit by a blinding bullet.")
	@ConfigEntry.BoundedInteger(min = 0, max = 6000)
	public int blindingDuration = 150;

	@ConfigEntry(nameKey = "Reduced Dragon Damage", comment = "If true, Dragon Minions will take only 10% damage from the Ender Dragon.")
	public boolean reducedDragonDamage = true;

	public MinionFeature(LabelConfigGroup parent) {
		parent.addConfigContainer(this);
		if (this.minCooldown > this.maxCooldown)
			this.minCooldown = this.maxCooldown;
		ServerEntityEvents.ENTITY_LOAD.register((entity, world) -> this.onDragonSpawn(new DummyEvent(world, entity)));
		ServerEntityEvents.ENTITY_LOAD.register((entity, world) -> this.onShulkerSpawn(new DummyEvent(world, entity)));
		LivingEntityEvents.TICK.register((entity) -> this.update(new DummyEvent(entity.level, entity)));
		LivingEntityEvents.HURT.register((event) -> this.onMinionHurt(event));

	}

	public void onDragonSpawn(DummyEvent event) {
		if (event.getWorld().isClientSide)
			return;

		if (!(event.getEntity() instanceof EnderDragon dragon))
			return;

		CompoundTag dragonTags = ((IEntityExtraData) dragon).getPersistentData();

		int cooldown = (int) (RandomHelper.getInt(dragon.getRandom(), this.minCooldown, this.maxCooldown) * 0.5d);
		dragonTags.putInt(Strings.Tags.DRAGON_MINION_COOLDOWN, cooldown);
	}

	public void onShulkerSpawn(DummyEvent event) {
		if (event.getWorld().isClientSide)
			return;

		if (!(event.getEntity() instanceof Shulker shulker))
			return;

		CompoundTag tags = ((IEntityExtraData) shulker).getPersistentData();
		if (!tags.contains(Strings.Tags.DRAGON_MINION))
			return;

		setMinionAI(shulker);
	}

	public void update(DummyEvent event) {
		if (event.getEntity().level.isClientSide)
			return;

		if (!(event.getEntity() instanceof EnderDragon dragon))
			return;

		Level world = event.getEntity().level;

		CompoundTag dragonTags = ((IEntityExtraData) dragon).getPersistentData();

		float difficulty = dragonTags.getFloat(Strings.Tags.DIFFICULTY);
		if (difficulty < this.minionAtDifficulty)
			return;

		if (dragon.getHealth() <= 0)
			return;

		int cooldown = dragonTags.getInt(Strings.Tags.DRAGON_MINION_COOLDOWN);
		if (cooldown > 0) {
			dragonTags.putInt(Strings.Tags.DRAGON_MINION_COOLDOWN, cooldown - 1);
			return;
		}

		//If there is no player in the main island don't spawn minions
		BlockPos centerPodium = dragon.level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, EndPodiumFeature.END_PODIUM_LOCATION);
		AABB bb = new AABB(centerPodium).inflate(64d);
		List<ServerPlayer> players = world.getEntitiesOfClass(ServerPlayer.class, bb);

		if (players.isEmpty())
			return;

		int minCooldown = this.minCooldown;
		int maxCooldown = this.maxCooldown;

		cooldown = RandomHelper.getInt(world.random, minCooldown, maxCooldown);
		cooldown *= 1 - this.cooldownReduction * difficulty;
		dragonTags.putInt(Strings.Tags.DRAGON_MINION_COOLDOWN, cooldown - 1);

		float angle = world.random.nextFloat() * (float) Math.PI * 2f;
		float x = (float) (Math.cos(angle) * (RandomHelper.getFloat(dragon.getRandom(), 16f, 45f)));
		float z = (float) (Math.sin(angle) * (RandomHelper.getFloat(dragon.getRandom(), 16f, 45f)));
		float y = world.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, new BlockPos(x, 255, z)).getY();
		summonMinion(world, new Vec3(x, y, z), difficulty);
	}

	private static void setMinionAI(Shulker shulker) {
		ArrayList<Goal> toRemove = new ArrayList<>();
		shulker.goalSelector.availableGoals.forEach(goal -> {
			if (goal.getGoal() instanceof Shulker.ShulkerAttackGoal)
				toRemove.add(goal.getGoal());
		});

		toRemove.forEach(shulker.goalSelector::removeGoal);
		shulker.goalSelector.addGoal(2, new DragonMinionAttackGoal(shulker, 70));

		toRemove.clear();
		shulker.targetSelector.availableGoals.forEach(goal -> {
			if (goal.getGoal() instanceof NearestAttackableTargetGoal)
				toRemove.add(goal.getGoal());
			if (goal.getGoal() instanceof HurtByTargetGoal)
				toRemove.add(goal.getGoal());
		});
		toRemove.forEach(shulker.targetSelector::removeGoal);

		shulker.targetSelector.addGoal(2, new PBNearestAttackableTargetGoal(shulker));
		shulker.targetSelector.addGoal(1, new HurtByTargetGoal(shulker, Shulker.class, EnderDragon.class));
	}

	public Shulker summonMinion(Level world, Vec3 pos, float difficulty) {
		Shulker shulker = new Shulker(EntityType.SHULKER, world);
		CompoundTag minionTags = ((IEntityExtraData) shulker).getPersistentData();
		minionTags.putBoolean(Strings.Tags.DRAGON_MINION, true);

		minionTags.putBoolean("mobspropertiesrandomness:processed", true);
		//TODO Scaling health

		boolean isBlindingMinion = world.getRandom().nextDouble() < this.blindingChance * difficulty;

		shulker.setPos(pos.x, pos.y, pos.z);
		shulker.setCustomName(MutableComponent.create(new TranslatableContents(Strings.Translatable.DRAGON_MINION)));
		shulker.lootTable = BuiltInLootTables.EMPTY;
		shulker.setPersistenceRequired();
		DragonMinionHelper.setMinionColor(shulker, isBlindingMinion);

		MCUtils.applyModifier(shulker, Attributes.FOLLOW_RANGE, Strings.AttributeModifiers.FOLLOW_RANGE_BONUS_UUID, Strings.AttributeModifiers.FOLLOW_RANGE_BONUS, 64, AttributeModifier.Operation.ADDITION);

		world.addFreshEntity(shulker);
		return shulker;
	}

	public void onMinionHurt(OnLivingHurtEvent event) {
		if (!this.reducedDragonDamage)
			return;

		if (!(event.getEntity() instanceof Shulker shulker))
			return;

		CompoundTag compoundNBT = ((IEntityExtraData) shulker).getPersistentData();
		if (!compoundNBT.contains(Strings.Tags.DRAGON_MINION))
			return;

		if (event.getSource().getDirectEntity() instanceof EnderDragon)
			event.setAmount(event.getAmount() * 0.1f);
	}

	public void onBulletTick(ShulkerBullet shulkerBulletEntity) {
		if (!shulkerBulletEntity.level.isClientSide && ((IEntityExtraData) shulkerBulletEntity).getPersistentData().getBoolean(Strings.Tags.BLINDNESS_BULLET)) {
			((ServerLevel)shulkerBulletEntity.level).sendParticles(ParticleTypes.ENTITY_EFFECT, shulkerBulletEntity.getX(), shulkerBulletEntity.getY(), shulkerBulletEntity.getZ(), 1, 0d, 0d, 0d, 0d);
		}
	}
}