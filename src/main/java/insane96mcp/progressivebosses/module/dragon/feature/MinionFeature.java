package insane96mcp.progressivebosses.module.dragon.feature;

import java.util.ArrayList;
import java.util.List;

import insane96mcp.progressivebosses.module.dragon.ai.DragonMinionAttackGoal;
import insane96mcp.progressivebosses.module.dragon.ai.PBNearestAttackableTargetGoal;
import insane96mcp.progressivebosses.utils.DragonMinionHelper;
import insane96mcp.progressivebosses.utils.DummyEvent;
import insane96mcp.progressivebosses.utils.IEntityExtraData;
import insane96mcp.progressivebosses.utils.Label;
import insane96mcp.progressivebosses.utils.LabelConfigGroup;
import insane96mcp.progressivebosses.utils.LivingEntityEvents;
import insane96mcp.progressivebosses.utils.LivingEntityEvents.OnLivingHurtEvent;
import insane96mcp.progressivebosses.utils.MCUtils;
import insane96mcp.progressivebosses.utils.RandomHelper;
import insane96mcp.progressivebosses.utils.Strings;
import me.lortseam.completeconfig.api.ConfigEntries;
import me.lortseam.completeconfig.api.ConfigEntry;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.minecraft.block.EndPortalBlock;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ai.goal.ActiveTargetGoal;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.ai.goal.RevengeGoal;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.boss.dragon.EnderDragonEntity;
import net.minecraft.entity.mob.ShulkerEntity;
import net.minecraft.entity.projectile.ShulkerBulletEntity;
import net.minecraft.loot.LootTables;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.MutableText;
import net.minecraft.text.TranslatableTextContent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.Heightmap;
import net.minecraft.world.World;
import net.minecraft.world.gen.feature.EndGatewayFeature;
import net.minecraft.world.gen.feature.EndPortalFeature;

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
		LivingEntityEvents.TICK.register((entity) -> this.update(new DummyEvent(entity.getWorld(), entity)));
		LivingEntityEvents.HURT.register((event) -> this.onMinionHurt(event));

	}

	public void onDragonSpawn(DummyEvent event) {
		if (event.getWorld().isClient)
			return;

		if (!(event.getEntity() instanceof EnderDragonEntity dragon))
			return;

		NbtCompound dragonTags = ((IEntityExtraData) dragon).getPersistentData();

		int cooldown = (int) (RandomHelper.getInt(dragon.getRandom(), this.minCooldown, this.maxCooldown) * 0.5d);
		dragonTags.putInt(Strings.Tags.DRAGON_MINION_COOLDOWN, cooldown);
	}

	public void onShulkerSpawn(DummyEvent event) {
		if (event.getWorld().isClient)
			return;

		if (!(event.getEntity() instanceof ShulkerEntity shulker))
			return;

		NbtCompound tags = ((IEntityExtraData) shulker).getPersistentData();
		if (!tags.contains(Strings.Tags.DRAGON_MINION))
			return;

		setMinionAI(shulker);
	}

	public void update(DummyEvent event) {
		if (event.getEntity().getWorld().isClient)
			return;

		if (!(event.getEntity() instanceof EnderDragonEntity dragon))
			return;

		World world = event.getEntity().getWorld();

		NbtCompound dragonTags = ((IEntityExtraData) dragon).getPersistentData();

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
		BlockPos centerPodium = dragon.getWorld().getTopPosition(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, EndPortalFeature.offsetOrigin(BlockPos.ORIGIN));
		Box bb = new Box(centerPodium).expand(64d);
		List<ServerPlayerEntity> players = world.getNonSpectatingEntities(ServerPlayerEntity.class, bb);

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
		float y = world.getTopPosition(Heightmap.Type.MOTION_BLOCKING, new BlockPos((int) x, 255, (int) z)).getY();
		summonMinion(world, new Vec3d(x, y, z), difficulty);
	}

	private static void setMinionAI(ShulkerEntity shulker) {
		ArrayList<Goal> toRemove = new ArrayList<>();
		shulker.goalSelector.goals.forEach(goal -> {
			if (goal.getGoal() instanceof ShulkerEntity.ShootBulletGoal)
				toRemove.add(goal.getGoal());
		});

		toRemove.forEach(shulker.goalSelector::remove);
		shulker.goalSelector.add(2, new DragonMinionAttackGoal(shulker, 70));

		toRemove.clear();
		shulker.targetSelector.goals.forEach(goal -> {
			if (goal.getGoal() instanceof ActiveTargetGoal)
				toRemove.add(goal.getGoal());
			if (goal.getGoal() instanceof RevengeGoal)
				toRemove.add(goal.getGoal());
		});
		toRemove.forEach(shulker.targetSelector::remove);

		shulker.targetSelector.add(2, new PBNearestAttackableTargetGoal(shulker));
		shulker.targetSelector.add(1, new RevengeGoal(shulker, ShulkerEntity.class, EnderDragonEntity.class));
	}

	public ShulkerEntity summonMinion(World world, Vec3d pos, float difficulty) {
		ShulkerEntity shulker = new ShulkerEntity(EntityType.SHULKER, world);
		NbtCompound minionTags = ((IEntityExtraData) shulker).getPersistentData();
		minionTags.putBoolean(Strings.Tags.DRAGON_MINION, true);

		minionTags.putBoolean("mobspropertiesrandomness:processed", true);
		//TODO Scaling health

		boolean isBlindingMinion = world.getRandom().nextDouble() < this.blindingChance * difficulty;

		shulker.setPosition(pos.x, pos.y, pos.z);
		shulker.setCustomName(MutableText.of(new TranslatableTextContent(Strings.Translatable.DRAGON_MINION, "translate error at DRAGON_MINION", new Object[]{})));
		shulker.lootTable = LootTables.EMPTY;
		shulker.setPersistent();
		DragonMinionHelper.setMinionColor(shulker, isBlindingMinion);

		MCUtils.applyModifier(shulker, EntityAttributes.GENERIC_FOLLOW_RANGE, Strings.AttributeModifiers.FOLLOW_RANGE_BONUS_UUID, Strings.AttributeModifiers.FOLLOW_RANGE_BONUS, 64, EntityAttributeModifier.Operation.ADDITION);

		world.spawnEntity(shulker);
		return shulker;
	}

	public void onMinionHurt(OnLivingHurtEvent event) {
		if (!this.reducedDragonDamage)
			return;

		if (!(event.getEntity() instanceof ShulkerEntity shulker))
			return;

		NbtCompound compoundNBT = ((IEntityExtraData) shulker).getPersistentData();
		if (!compoundNBT.contains(Strings.Tags.DRAGON_MINION))
			return;

		if (event.getSource().getSource() instanceof EnderDragonEntity)
			event.setAmount(event.getAmount() * 0.1f);
	}

	public void onBulletTick(ShulkerBulletEntity shulkerBulletEntity) {
		if (!shulkerBulletEntity.getWorld().isClient && ((IEntityExtraData) shulkerBulletEntity).getPersistentData().getBoolean(Strings.Tags.BLINDNESS_BULLET)) {
			((ServerWorld)shulkerBulletEntity.getWorld()).spawnParticles(ParticleTypes.ENTITY_EFFECT, shulkerBulletEntity.getX(), shulkerBulletEntity.getY(), shulkerBulletEntity.getZ(), 1, 0d, 0d, 0d, 0d);
		}
	}
}
