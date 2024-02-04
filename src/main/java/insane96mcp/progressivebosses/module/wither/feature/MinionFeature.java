package insane96mcp.progressivebosses.module.wither.feature;

import java.util.List;
import java.util.UUID;

import insane96mcp.progressivebosses.ProgressiveBosses;
import insane96mcp.progressivebosses.module.wither.entity.WitherMinion;
import insane96mcp.progressivebosses.utils.DummyEvent;
import insane96mcp.progressivebosses.utils.IEntityExtraData;
import insane96mcp.progressivebosses.utils.Label;
import insane96mcp.progressivebosses.utils.LabelConfigGroup;
import insane96mcp.progressivebosses.utils.LivingEntityEvents;
import insane96mcp.progressivebosses.utils.LivingEntityEvents.OnLivingDeathEvent;
import insane96mcp.progressivebosses.utils.LivingEntityEvents.OnLivingHurtEvent;
import insane96mcp.progressivebosses.utils.MCUtils;
import insane96mcp.progressivebosses.utils.RandomHelper;
import insane96mcp.progressivebosses.utils.Strings;
import me.lortseam.completeconfig.api.ConfigEntries;
import me.lortseam.completeconfig.api.ConfigEntry;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.boss.WitherEntity;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

@ConfigEntries(includeAll = true)
@Label(name = "Minions", description = "Wither will spawn deadly Minions")
public class MinionFeature implements LabelConfigGroup {

	@ConfigEntry(nameKey = "Minion at Difficulty", comment = "At which difficulty the Wither starts spawning Minions")
	@ConfigEntry.BoundedInteger(min = 0, max = Integer.MAX_VALUE)
	public int minionAtDifficulty = 1;

	@ConfigEntry(nameKey = "Bonus Minion Every Difficulty", comment = "As the Wither starts spawning Minions, every how much difficulty the Wither will spawn one more Minion")
	@ConfigEntry.BoundedInteger(min = 0, max = Integer.MAX_VALUE)
	public int bonusMinionEveryDifficulty = 1;

	@ConfigEntry(nameKey = "Max Minions Spawned", comment = "Maximum Minions spawned by the Wither")
	@ConfigEntry.BoundedInteger(min = 0, max = Integer.MAX_VALUE)
	public int maxSpawned = 6;

	@ConfigEntry(nameKey = "Max Minions Around", comment = "Maximum amount of Minions that can be around the Wither in a 16 block radius. After this number is reached the Wither will stop spawning minions. Set to 0 to disable this check")
	@ConfigEntry.BoundedInteger(min = 0, max = Integer.MAX_VALUE)
	public int maxAround = 18;

	@ConfigEntry(nameKey = "Minimum Cooldown", comment = "Minimum ticks (20 ticks = 1 seconds) after Minions can spwan.")
	@ConfigEntry.BoundedInteger(min = 0, max = Integer.MAX_VALUE)
	public int minCooldown = 400;

	@ConfigEntry(nameKey = "Maximum Cooldown", comment = "Maximum ticks (20 ticks = 1 seconds) after Minions can spwan.")
	@ConfigEntry.BoundedInteger(min = 0, max = Integer.MAX_VALUE)
	public int maxCooldown = 700;

	@ConfigEntry(nameKey = "Cooldown Multiplier Below Half Health", comment = "Min and Max cooldowns are multiplied by this value when the Wither drops below half health. Set to 1 to not change the cooldown when the wither's health drops below half.")
	@ConfigEntry.BoundedDouble(min = 0d, max = Double.MAX_VALUE)
	public double cooldownMultiplierBelowHalfHealth = 0.6d;

	@ConfigEntry(nameKey = "Bonus Movement Speed Per Difficulty", comment = "Percentage bonus speed per difficulty. (0.01 means 1%)")
	@ConfigEntry.BoundedDouble(min = 0d, max = Double.MAX_VALUE)
	public double bonusSpeedPerDifficulty = 0.03d;

	@ConfigEntry(nameKey = "Magic Damage Multiplier", comment = "Wither Minions will take magic damage multiplied by this value.")
	@ConfigEntry.BoundedDouble(min = 0, max = Double.MAX_VALUE)
	public double magicDamageMultiplier = 3.0d;

	@ConfigEntry(nameKey = "Kill Minions on Wither Death", comment = "Wither Minions will die when the Wither that spawned them dies.")
	public boolean killMinionOnWitherDeath = true;

	@ConfigEntry(nameKey = "Has Sword", comment = "Wither Minions will spawn with a Stone Sword")
	public boolean hasSword = true;

	@ConfigEntry(nameKey = "Bow Chance Over Half Health", comment = "Chance for the Wither Minion to spawn with a bow when Wither's above Half Health")
	@ConfigEntry.BoundedDouble(min = 0d, max = 1d)
	public double preHalfHealthBowChance = 0.6d;

	@ConfigEntry(nameKey = "Bow Chance Below Half Health", comment = "Chance for the Wither Minion to spawn with a bow when Wither's below Half Health")
	@ConfigEntry.BoundedDouble(min = 0d, max = 1d)
	public double halfHealthBowChance = 0.08d;

	@ConfigEntry(nameKey = "Power / Sharpness Chance", comment = "Chance (per difficulty) for the Wither Minion Sword / Bow to be enchanted with Sharpness / Power. Note that every 100% chance adds one guaranteed level of the enchantment, while the remaining chance dictates if one more level will be added.")
	@ConfigEntry.BoundedDouble(min = 0d, max = Double.MAX_VALUE)
	public double powerSharpnessChance = 0.6d;

	@ConfigEntry(nameKey = "Punch / Knockback Chance", comment = "Chance (per difficulty) for the Wither Minion Sword / Bow to be enchanted with Knockback / Punch. Note that every 100% chance adds one guaranteed level of the enchantment, while the remaining chance dictates if one more level will be added.")
	@ConfigEntry.BoundedDouble(min = 0d, max = Double.MAX_VALUE)
	public double punchKnockbackChance = 0.3d;

	public MinionFeature(LabelConfigGroup parent) {
		parent.addConfigContainer(this);
		ServerEntityEvents.ENTITY_LOAD.register((entity, world) -> this.onWitherSpawn(new DummyEvent(world, entity)));
		LivingEntityEvents.TICK.register((entity) -> this.update(new DummyEvent(entity.getWorld(), entity)));
		LivingEntityEvents.HURT.register((event) -> this.onMinionDamage(event));
		LivingEntityEvents.DEATH.register((event) -> this.onDeath(event));
	}

	public void onWitherSpawn(DummyEvent event) {
		if (event.getWorld().isClient)
			return;

		if (!(event.getEntity() instanceof WitherEntity wither))
			return;
		NbtCompound witherTags = ((IEntityExtraData) wither).getPersistentData();

		int cooldown = (int) (RandomHelper.getInt(wither.getWorld().random, this.minCooldown, this.maxCooldown) * this.cooldownMultiplierBelowHalfHealth);
		witherTags.putInt(Strings.Tags.WITHER_MINION_COOLDOWN, cooldown);
	}

	public void update(DummyEvent event) {
		if (event.getEntity().getWorld().isClient)
			return;

		if (!(event.getEntity() instanceof WitherEntity wither))
			return;

		World world = event.getEntity().getWorld();
		NbtCompound witherTags = ((IEntityExtraData) wither).getPersistentData();

		float difficulty = witherTags.getFloat(Strings.Tags.DIFFICULTY);
		if (difficulty < this.minionAtDifficulty)
			return;

		if (wither.getHealth() <= 0)
			return;

		if (wither.getInvulnerableTimer() > 0)
			return;

		int cooldown = witherTags.getInt(Strings.Tags.WITHER_MINION_COOLDOWN);
		if (cooldown > 0) {
			witherTags.putInt(Strings.Tags.WITHER_MINION_COOLDOWN, cooldown - 1);
			return;
		}

		//If there is no player in a radius from the wither, don't spawn minions
		int radius = 32;
		BlockPos pos1 = wither.getBlockPos().add(-radius, -radius, -radius);
		BlockPos pos2 = wither.getBlockPos().add(radius, radius, radius);
		Box bb = new Box(pos1, pos2);
		List<ServerPlayerEntity> players = world.getNonSpectatingEntities(ServerPlayerEntity.class, bb);

		if (players.isEmpty())
			return;

		List<WitherMinion> minionsInAABB = world.getNonSpectatingEntities(WitherMinion.class, wither.getBoundingBox().expand(16));
		int minionsCountInAABB = minionsInAABB.size();

		if (minionsCountInAABB >= this.maxAround)
			return;

		int minCooldown = this.minCooldown;
		int maxCooldown = this.maxCooldown;

		cooldown = RandomHelper.getInt(world.random, minCooldown, maxCooldown);
		if (wither.shouldRenderOverlay())
			cooldown *= this.cooldownMultiplierBelowHalfHealth;
		witherTags.putInt(Strings.Tags.WITHER_MINION_COOLDOWN, cooldown - 1);

		int minionSpawnedCount = 0;
		for (int i = this.minionAtDifficulty; i <= difficulty; i += this.bonusMinionEveryDifficulty) {

			int x = 0, y = 0, z = 0;
			//Tries to spawn the Minion up to 5 times
			for (int t = 0; t < 5; t++) {
				x = (int) (wither.getX() + (RandomHelper.getInt(world.random, -3, 3)));
				y = (int) (wither.getY() + 3);
				z = (int) (wither.getZ() + (RandomHelper.getInt(world.random, -3, 3)));

				y = getYSpawn(ProgressiveBosses.WITHER_MINION, new BlockPos(x, y, z), world, 8);
				if (y != -1)
					break;
			}
			if (y <= wither.getWorld().getBottomY())
				continue;

			WitherMinion witherMinion = summonMinion(world, new Vec3d(x + 0.5, y + 0.5, z + 0.5), difficulty, wither.shouldRenderOverlay());

			//No need since EntityJoinWorldEvent is triggered
			//setMinionAI(witherMinion);

			NbtList minionsList = witherTags.getList(Strings.Tags.MINIONS, NbtElement.COMPOUND_TYPE);
			NbtCompound uuid = new NbtCompound();
			uuid.putUuid("uuid", witherMinion.getUuid());
			minionsList.add(uuid);
			witherTags.put(Strings.Tags.MINIONS, minionsList);

			minionSpawnedCount++;
			if (minionSpawnedCount >= this.maxSpawned)
				break;

			minionsCountInAABB++;
			if (minionsCountInAABB >= this.maxAround)
				break;
		}
	}

	public void onMinionDamage(OnLivingHurtEvent event) {
		if (this.magicDamageMultiplier == 0d)
			return;

		if (!(event.getEntity() instanceof WitherMinion))
			return;

		//Handle Magic Damage
		if (event.getSource().isOf(DamageTypes.MAGIC) && event.getSource().isOf(DamageTypes.INDIRECT_MAGIC)) {
			event.setAmount((float) (event.getAmount() * this.magicDamageMultiplier));
		}
	}

	public void onDeath(OnLivingDeathEvent event) {
		if (event.getEntity().getWorld().isClient)
			return;

		if (!this.killMinionOnWitherDeath)
			return;

		if (!(event.getEntity() instanceof WitherEntity wither))
			return;
		
		ServerWorld world = (ServerWorld) wither.getWorld();

		NbtCompound tags = ((IEntityExtraData) wither).getPersistentData();
		NbtList minionsList = tags.getList(Strings.Tags.MINIONS, NbtElement.COMPOUND_TYPE);

		for (int i = 0; i < minionsList.size(); i++) {
			UUID uuid = minionsList.getCompound(i).getUuid("uuid");
			WitherMinion witherMinion = (WitherMinion) world.getEntity(uuid);
			if (witherMinion == null)
				continue;
			witherMinion.addStatusEffect(new StatusEffectInstance(StatusEffects.INSTANT_HEALTH, 10000, 0, false, false));
		}
	}

	private void setEquipment(WitherMinion witherMinion, float difficulty, boolean isCharged) {
		witherMinion.setEquipmentDropChance(EquipmentSlot.MAINHAND, Float.MIN_VALUE);

		int powerSharpnessLevel = (int) (this.powerSharpnessChance * difficulty);
		if (RandomHelper.getDouble(witherMinion.getWorld().getRandom(), 0d, 1d) < (this.powerSharpnessChance * difficulty) - powerSharpnessLevel)
			powerSharpnessLevel++;

		int punchKnockbackLevel = (int) (this.punchKnockbackChance * difficulty);
		if (RandomHelper.getDouble(witherMinion.getWorld().getRandom(), 0d, 1d) < (this.punchKnockbackChance * difficulty) - punchKnockbackLevel)
			punchKnockbackLevel++;

		ItemStack sword = new ItemStack(Items.STONE_SWORD);
		if (powerSharpnessLevel > 0)
			sword.addEnchantment(Enchantments.SHARPNESS, powerSharpnessLevel);
		if (punchKnockbackLevel > 0)
			sword.addEnchantment(Enchantments.KNOCKBACK, punchKnockbackLevel);
		if (this.hasSword)
			witherMinion.equipStack(EquipmentSlot.MAINHAND, sword);

		ItemStack bow = new ItemStack(Items.BOW);
		if (powerSharpnessLevel > 0)
			bow.addEnchantment(Enchantments.POWER, powerSharpnessLevel);
		if (punchKnockbackLevel > 0)
			bow.addEnchantment(Enchantments.POWER, punchKnockbackLevel);
		if (isCharged) {
			if (RandomHelper.getDouble(witherMinion.getWorld().getRandom(), 0d, 1d) < this.halfHealthBowChance) {
				witherMinion.equipStack(EquipmentSlot.MAINHAND, bow);
			}
		}
		else {
			if (RandomHelper.getDouble(witherMinion.getWorld().getRandom(), 0d, 1d) < this.preHalfHealthBowChance) {
				witherMinion.equipStack(EquipmentSlot.MAINHAND, bow);
			}
		}
	}

	/**
	 * Returns -1 when no spawn spots are found, otherwise the Y coord
	 */
	private static int getYSpawn(EntityType<WitherMinion> entityType, BlockPos pos, World world, int minRelativeY) {
		int height = (int) Math.ceil(entityType.getHeight());
		int fittingYPos = -1;
		for (int y = pos.getY(); y > pos.getY() - minRelativeY; y--) {
			boolean viable = true;
			BlockPos p = new BlockPos(pos.getX(), y, pos.getZ());
			for (int i = 0; i < height; i++) {
				if (world.getBlockState(p.up(i)).blocksMovement()) {
					viable = false;
					break;
				}
			}
			if (!viable)
				continue;
			fittingYPos = y;
			if (!world.getBlockState(p.down()).blocksMovement())
				continue;
			return y;
		}
		return fittingYPos;
	}

	public WitherMinion summonMinion(World world, Vec3d pos, float difficulty, boolean isCharged) {
		WitherMinion witherMinion = new WitherMinion(ProgressiveBosses.WITHER_MINION, world);
		NbtCompound minionTags = ((IEntityExtraData) witherMinion).getPersistentData();

		minionTags.putBoolean("mobspropertiesrandomness:processed", true);
		//TODO Scaling health

		witherMinion.setPosition(pos.x, pos.y, pos.z);
		//witherMinion.setCustomName(new TranslationTextComponent(Strings.Translatable.WITHER_MINION));
		setEquipment(witherMinion, difficulty, isCharged);
		witherMinion.setEquipmentDropChance(EquipmentSlot.MAINHAND, -0.04f);
		//witherMinion.deathLootTable = LootTables.EMPTY;
		witherMinion.setPersistent();

		double speedBonus = this.bonusSpeedPerDifficulty * difficulty;
		MCUtils.applyModifier(witherMinion, EntityAttributes.GENERIC_MOVEMENT_SPEED, Strings.AttributeModifiers.MOVEMENT_SPEED_BONUS_UUID, Strings.AttributeModifiers.MOVEMENT_SPEED_BONUS, speedBonus, EntityAttributeModifier.Operation.MULTIPLY_BASE);
		MCUtils.applyModifier(witherMinion, EntityAttributes.GENERIC_FOLLOW_RANGE, Strings.AttributeModifiers.FOLLOW_RANGE_BONUS_UUID, Strings.AttributeModifiers.FOLLOW_RANGE_BONUS, 16, EntityAttributeModifier.Operation.ADDITION);
		// MCUtils.applyModifier(witherMinion, ForgeMod.SWIM_SPEED.get(), Strings.AttributeModifiers.SWIM_SPEED_BONUS_UUID, Strings.AttributeModifiers.SWIM_SPEED_BONUS, 2d, EntityAttributeModifier.Operation.MULTIPLY_BASE);

		world.spawnEntity(witherMinion);
		return witherMinion;
	}
}
