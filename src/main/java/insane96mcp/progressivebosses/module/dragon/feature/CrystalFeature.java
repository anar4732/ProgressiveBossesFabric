package insane96mcp.progressivebosses.module.dragon.feature;

import insane96mcp.progressivebosses.module.dragon.phase.CrystalRespawnPhase;
import insane96mcp.progressivebosses.utils.*;
import insane96mcp.progressivebosses.utils.LivingEntityEvents.OnLivingHurtEvent;
import me.lortseam.completeconfig.api.ConfigEntries;
import me.lortseam.completeconfig.api.ConfigEntry;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.enderdragon.phases.DragonPhaseInstance;
import net.minecraft.world.entity.boss.enderdragon.phases.EnderDragonPhase;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.IronBarsBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.EndPodiumFeature;
import net.minecraft.world.level.levelgen.feature.SpikeFeature;
import net.minecraft.world.phys.Vec3;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

@ConfigEntries(includeAll = true)
@Label(name = "Crystals", description = "Makes more Crystal spawn and with more cages.")
public class CrystalFeature implements LabelConfigGroup {

	@ConfigEntry(nameKey = "More Cages at Difficulty", comment = "At this difficulty cages will start to appear around other crystals too. -1 will disable this feature.")
	@ConfigEntry.BoundedInteger(min = 0, max = Integer.MAX_VALUE)
	public int moreCagesAtDifficulty = 1;

	@ConfigEntry(nameKey = "Max Bonus Cages", comment = "Max number of bonus cages that can spawn around the crystals.")
	@ConfigEntry.BoundedInteger(min = 0, max = 8)
	public int maxBonusCages = 6;

	@ConfigEntry(nameKey = "More Crystals at Difficulty", comment = "At this difficulty one crystal will start to appear inside obsidian towers. -1 will disable this feature.")
	@ConfigEntry.BoundedInteger(min = 0, max = Integer.MAX_VALUE)
	public int moreCrystalsAtDifficulty = 2;

	@ConfigEntry(nameKey = "More Crystals Step", comment = "Every how much difficulty one more crystal will be spawned inside towers")
	@ConfigEntry.BoundedInteger(min = 0, max = Integer.MAX_VALUE)
	public int moreCrystalsStep = 3;

	@ConfigEntry(nameKey = "More Crystals Max", comment = "Max number of bonus crystals that can spawn inside the towers.")
	@ConfigEntry.BoundedInteger(min = 0, max = 10)
	public int moreCrystalsMax = 3;

	@ConfigEntry(nameKey = "Enable crystal respawn", comment = "Everytime the dragon is hit (when below 50% of health) there's a chance to to trigger a Crystal respawn Phase. The chance is 0% when health >=50% and 100% when health <=30%, the health threshold decreases by 20% every time the dragon respawns crystals.")
	public boolean enableCrystalRespawn = true;

	@ConfigEntry(nameKey = "Crystal Respawn Per Difficulty", comment = "Difficulty multiplied by this number will output how many crystals will the dragon respawn.")
	@ConfigEntry.BoundedDouble(min = 0d, max = 10d)
	public double crystalRespawnPerDifficulty = 0.375d;

	@ConfigEntry(nameKey = "Explosion Immune", comment = "Crystals can no longer be destroyed by other explosions.")
	public boolean explosionImmune = true;

	public CrystalFeature(LabelConfigGroup parent) {
		parent.addConfigContainer(this);
		ServerEntityEvents.ENTITY_LOAD.register((entity, world) -> this.onSpawn(new DummyEvent(world, entity)));
		LivingEntityEvents.HURT.register((event) -> this.onDragonDamage(event));
	}

	@ConfigEntries.Exclude
	private static final List<EnderDragonPhase<? extends DragonPhaseInstance>> VALID_CRYSTAL_RESPAWN_PHASES = Arrays.asList(EnderDragonPhase.SITTING_SCANNING, EnderDragonPhase.SITTING_ATTACKING, EnderDragonPhase.SITTING_FLAMING, EnderDragonPhase.HOLDING_PATTERN, EnderDragonPhase.TAKEOFF, EnderDragonPhase.CHARGING_PLAYER, EnderDragonPhase.STRAFE_PLAYER);

	public void onDragonDamage(OnLivingHurtEvent event) {
		if (!(event.getEntity() instanceof EnderDragon dragon))
			return;

		if (!this.enableCrystalRespawn)
			return;

		CompoundTag dragonTags = ((IEntityExtraData) dragon).getPersistentData();
		float difficulty = dragonTags.getFloat(Strings.Tags.DIFFICULTY);

		if (!VALID_CRYSTAL_RESPAWN_PHASES.contains(dragon.getPhaseManager().getCurrentPhase().getPhase()))
			return;

		float healthRatio = dragon.getHealth() / dragon.getMaxHealth();
		if (healthRatio >= 0.80d)
			return;

		byte crystalRespawn = dragonTags.getByte(Strings.Tags.CRYSTAL_RESPAWN);

		//The first time, the chance is 0% at >=80% health and 100% at <=60% health. The health threshold decreases by 35% every time the enderdragon respawns the crystals
		//On 0 Respawns: 0% chance at health >=  80% and 100% at health <=  20%
		//On 1 Respawn : 0% chance at health >=  45% and  75% at health =    0%
		//On 2 Respawns: 0% chance at health >=  10% and  17% at health =    0%
		float chance = getChanceAtValue(healthRatio, 0.80f - (crystalRespawn * 0.35f), 0.20f - (crystalRespawn * 0.35f));

		if (dragon.getRandom().nextFloat() > chance)
			return;

		dragonTags.putByte(Strings.Tags.CRYSTAL_RESPAWN, (byte) (crystalRespawn + 1));

		double crystalsRespawned = Mth.clamp(difficulty * this.crystalRespawnPerDifficulty, 0, SpikeFeature.NUMBER_OF_SPIKES);
		crystalsRespawned = Utils.getAmountWithDecimalChance(dragon.getRandom(), crystalsRespawned);
		if (crystalsRespawned == 0d)
			return;

		dragon.getPhaseManager().setPhase(CrystalRespawnPhase.getPhaseType());
		CrystalRespawnPhase phase = (CrystalRespawnPhase) dragon.getPhaseManager().getCurrentPhase();

		List<SpikeFeature.EndSpike> spikes = new ArrayList<>(SpikeFeature.getSpikesForLevel((ServerLevel)dragon.level));
		spikes.sort(Comparator.comparingInt(SpikeFeature.EndSpike::getRadius).reversed());
		for (int i = 0; i < crystalsRespawned; i++) {
			SpikeFeature.EndSpike targetSpike = spikes.get(i);
			phase.addCrystalRespawn(targetSpike);
		}
	}

	/**
	 * Returns a percentage value (0~1) based off a min and max value. when value >= max the chance is 0%, when value <= min the chance is 100%. In-between the threshold, chance scales accordingly
	 */
	private float getChanceAtValue(float value, float max, float min) {
		return Mth.clamp((max - min - (value - min)) / (max - min), 0f, 1f);
	}

	public void onSpawn(DummyEvent event) {
		if (event.getWorld().isClientSide)
			return;

		if (!(event.getEntity() instanceof EnderDragon dragon))
			return;

		CompoundTag dragonTags = ((IEntityExtraData) dragon).getPersistentData();
		float difficulty = dragonTags.getFloat(Strings.Tags.DIFFICULTY);

		crystalCages(dragon, difficulty);
		moreCrystals(dragon, difficulty);
	}

	private void crystalCages(EnderDragon dragon, float difficulty) {
		if (this.moreCagesAtDifficulty == -1 || this.maxBonusCages == 0)
			return;

		if (difficulty < moreCagesAtDifficulty)
			return;

		CompoundTag dragonTags = ((IEntityExtraData) dragon).getPersistentData();
		if (dragonTags.contains(Strings.Tags.CRYSTAL_CAGES))
			return;

		dragonTags.putBoolean(Strings.Tags.CRYSTAL_CAGES, true);

		List<EndCrystal> crystals = new ArrayList<>();

		//Order from smaller towers to bigger ones
		List<SpikeFeature.EndSpike> spikes = new ArrayList<>(SpikeFeature.getSpikesForLevel((ServerLevel) dragon.level));
		spikes.sort(Comparator.comparingInt(SpikeFeature.EndSpike::getRadius));

		for(SpikeFeature.EndSpike spike : spikes) {
			crystals.addAll(dragon.level.getEntitiesOfClass(EndCrystal.class, spike.getTopBoundingBox()));
		}

		//Remove all the crystals that already have cages around
		crystals.removeIf(c -> c.level.getBlockState(c.blockPosition().above(2)).getBlock() == Blocks.IRON_BARS);

		int crystalsInvolved = Math.round(difficulty - this.moreCagesAtDifficulty + 1);
		int cagesGenerated = 0;

		for (EndCrystal crystal : crystals) {
			generateCage(crystal.level, crystal.blockPosition());

			cagesGenerated++;
			if (cagesGenerated == crystalsInvolved || cagesGenerated == this.maxBonusCages)
				break;
		}
	}

	private void moreCrystals(EnderDragon dragon, float difficulty) {
		if (this.moreCrystalsAtDifficulty == -1 || this.moreCrystalsMax == 0)
			return;

		if (difficulty < this.moreCrystalsAtDifficulty)
			return;

		CompoundTag dragonTags = ((IEntityExtraData) dragon).getPersistentData();
		if (dragonTags.contains(Strings.Tags.MORE_CRYSTALS))
			return;

		dragonTags.putBoolean(Strings.Tags.MORE_CRYSTALS, true);

		List<EndCrystal> crystals = new ArrayList<>();

		//Order from smaller towers to bigger ones
		List<SpikeFeature.EndSpike> spikes = new ArrayList<>(SpikeFeature.getSpikesForLevel((ServerLevel) dragon.level));
		spikes.sort(Comparator.comparingInt(SpikeFeature.EndSpike::getRadius));

		for(SpikeFeature.EndSpike spike : spikes) {
			crystals.addAll(dragon.level.getEntitiesOfClass(EndCrystal.class, spike.getTopBoundingBox(), EndCrystal::showsBottom));
		}

		int crystalsMax = (int) Math.ceil((difficulty + 1 - this.moreCrystalsAtDifficulty) / this.moreCrystalsStep);
		if (crystalsMax <= 0)
			return;
		int crystalSpawned = 0;

		for (EndCrystal crystal : crystals) {
			generateCrystalInTower(dragon.level, crystal.getX(), crystal.getY(), crystal.getZ());

			crystalSpawned++;
			if (crystalSpawned == crystalsMax || crystalSpawned == this.moreCrystalsMax)
				break;
		}
	}

	public boolean onDamageFromExplosion(EndCrystal enderCrystalEntity, DamageSource source) {
		if (!this.isEnabled())
			return false;

		if (!this.explosionImmune)
			return false;

		return source.isExplosive();
	}

	@ConfigEntries.Exclude
	private static final ResourceLocation ENDERGETIC_CRYSTAL_HOLDER_RL = new ResourceLocation("endergetic:crystal_holder");

	public static EndCrystal generateCrystalInTower(Level world, double x, double y, double z) {
		Vec3 centerPodium = Vec3.atBottomCenterOf(world.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, EndPodiumFeature.END_PODIUM_LOCATION));

		int spawnY = (int) (y - RandomHelper.getInt(world.getRandom(), 12, 24));
		if (spawnY < centerPodium.y())
			spawnY = (int) centerPodium.y();
		BlockPos crystalPos = new BlockPos(x, spawnY, z);

		Stream<BlockPos> blocks = BlockPos.betweenClosedStream(crystalPos.offset(-1, -1, -1), crystalPos.offset(1, 1, 1));

		blocks.forEach(pos -> world.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState()));

		BlockState baseBlockState = Blocks.BEDROCK.defaultBlockState();
		// if (ModList.get().isLoaded("endergetic"))
			if (BuiltInRegistries.BLOCK.containsKey(ENDERGETIC_CRYSTAL_HOLDER_RL))
				baseBlockState = BuiltInRegistries.BLOCK.get(ENDERGETIC_CRYSTAL_HOLDER_RL).defaultBlockState();
			// else
				// LogHelper.warn("The Endergetic Expansion is loaded but the %s block was not registered", ENDERGETIC_CRYSTAL_HOLDER_RL);
		world.setBlockAndUpdate(crystalPos.offset(0, -1, 0), baseBlockState);

		world.explode(null, crystalPos.getX() + .5f, crystalPos.getY(), crystalPos.getZ() + .5, 5f, Level.ExplosionInteraction.MOB);

		EndCrystal crystal = new EndCrystal(world, crystalPos.getX() + .5, crystalPos.getY(), crystalPos.getZ() + .5);
		world.addFreshEntity(crystal);

		return crystal;
	}

	public static void generateCage(Level world, BlockPos pos) {
		//Shamelessly copied from Vanilla Code
		BlockPos.MutableBlockPos blockpos$mutable = new BlockPos.MutableBlockPos();
		for(int k = -2; k <= 2; ++k) {
			for(int l = -2; l <= 2; ++l) {
				for(int i1 = 0; i1 <= 3; ++i1) {
					boolean flag = Mth.abs(k) == 2;
					boolean flag1 = Mth.abs(l) == 2;
					boolean flag2 = i1 == 3;
					if (flag || flag1 || flag2) {
						boolean flag3 = k == -2 || k == 2 || flag2;
						boolean flag4 = l == -2 || l == 2 || flag2;
						BlockState blockstate = Blocks.IRON_BARS.defaultBlockState().setValue(IronBarsBlock.NORTH, flag3 && l != -2).setValue(IronBarsBlock.SOUTH, flag3 && l != 2).setValue(IronBarsBlock.WEST, flag4 && k != -2).setValue(IronBarsBlock.EAST, flag4 && k != 2);
						world.setBlockAndUpdate(blockpos$mutable.set(pos.getX() + k, pos.getY() - 1 + i1, pos.getZ() + l), blockstate);
					}
				}
			}
		}
	}
}