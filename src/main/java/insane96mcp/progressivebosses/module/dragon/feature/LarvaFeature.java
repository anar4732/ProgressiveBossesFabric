package insane96mcp.progressivebosses.module.dragon.feature;

import insane96mcp.progressivebosses.ProgressiveBosses;
import insane96mcp.progressivebosses.module.dragon.entity.Larva;
import insane96mcp.progressivebosses.utils.*;
import me.lortseam.completeconfig.api.ConfigEntries;
import me.lortseam.completeconfig.api.ConfigEntry;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.EndPodiumFeature;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import java.util.List;

@ConfigEntries(includeAll = true)
@Label(name = "Larva", description = "Mini things that are just annoying.")
public class LarvaFeature implements LabelConfigGroup {

	@ConfigEntry(nameKey = "Larva at Difficulty", comment = "At which difficulty the Ender Dragon starts spawning Larvae")
	@ConfigEntry.BoundedInteger(min = 1, max = Integer.MAX_VALUE)
	public int larvaAtDifficulty = 1;

	@ConfigEntry(nameKey = "Bonus Larva Every Difficulty", comment = "As the Wither starts spawning Minions, every how much difficulty the Wither will spawn one more Minion")
	@ConfigEntry.BoundedInteger(min = 1, max = Integer.MAX_VALUE)
	public int bonusLarvaEveryDifficulty = 1;

	@ConfigEntry(nameKey = "Max Larvae Spawned", comment = "Maximum Larva spawned by the Ender Dragon")
	@ConfigEntry.BoundedInteger(min = 1, max = Integer.MAX_VALUE)
	public int maxSpawned = 7;

	@ConfigEntry(nameKey = "Minimum Cooldown", comment = "Minimum ticks (20 ticks = 1 seconds) after Minions can spawn.")
	@ConfigEntry.BoundedInteger(min = 1, max = Integer.MAX_VALUE)
	public int minCooldown = 800;

	@ConfigEntry(nameKey = "Maximum Cooldown", comment = "Maximum ticks (20 ticks = 1 seconds) after Minions can spawn.")
	@ConfigEntry.BoundedInteger(min = 1, max = Integer.MAX_VALUE)
	public int maxCooldown = 1400;

	@ConfigEntry(nameKey = "Reduced Dragon Damage", comment = "If true, Larvae will take only 10% damage from the Ender Dragon.")
	public boolean reducedDragonDamage = true;

	public LarvaFeature(LabelConfigGroup config) {
		config.addConfigContainer(this);
		ServerEntityEvents.ENTITY_LOAD.register((entity, world) -> this.onDragonSpawn(new DummyEvent(world, entity)));
		LivingEntityEvents.TICK.register((entity) -> this.update(new DummyEvent(entity.level, entity)));
	}

	public void onDragonSpawn(DummyEvent event) {
		if (event.getWorld().isClientSide)
			return;

		if (!(event.getEntity() instanceof EnderDragon dragon))
			return;

		CompoundTag dragonTags = ((IEntityExtraData) dragon).getPersistentData();

		int cooldown = (int) (RandomHelper.getInt(dragon.getRandom(), this.minCooldown, this.maxCooldown) * 0.5d);
		dragonTags.putInt(Strings.Tags.DRAGON_LARVA_COOLDOWN, cooldown);
	}

	public void update(DummyEvent event) {
		if (event.getEntity().level.isClientSide)
			return;

		if (!(event.getEntity() instanceof EnderDragon dragon))
			return;

		Level world = event.getEntity().level;

		CompoundTag dragonTags = ((IEntityExtraData) dragon).getPersistentData();

		float difficulty = dragonTags.getFloat(Strings.Tags.DIFFICULTY);
		if (difficulty < this.larvaAtDifficulty)
			return;

		if (dragon.getHealth() <= 0)
			return;

		int cooldown = dragonTags.getInt(Strings.Tags.DRAGON_LARVA_COOLDOWN);
		if (cooldown > 0) {
			dragonTags.putInt(Strings.Tags.DRAGON_LARVA_COOLDOWN, cooldown - 1);
			return;
		}

		//If there is no player in the main island don't spawn larvae
		BlockPos centerPodium = dragon.level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, EndPodiumFeature.END_PODIUM_LOCATION);
		AABB bb = new AABB(centerPodium).inflate(64d);
		List<ServerPlayer> players = world.getEntitiesOfClass(ServerPlayer.class, bb);

		if (players.isEmpty())
			return;

		int minCooldown = this.minCooldown;
		int maxCooldown = this.maxCooldown;

		cooldown = RandomHelper.getInt(world.random, minCooldown, maxCooldown);
		dragonTags.putInt(Strings.Tags.DRAGON_LARVA_COOLDOWN, cooldown - 1);

		int larvaSpawnedCount = 0;
		for (int i = this.larvaAtDifficulty; i <= difficulty; i += this.bonusLarvaEveryDifficulty) {
			float angle = world.random.nextFloat() * (float) Math.PI * 2f;
			float x = (float) Math.floor(Math.cos(angle) * 3.33f);
			float z = (float) Math.floor(Math.sin(angle) * 3.33f);
			int y = world.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, new BlockPos(x, 255, z)).getY();
			summonLarva(world, new Vec3(x + 0.5, y, z + 0.5), difficulty);
			larvaSpawnedCount++;
			if (larvaSpawnedCount >= this.maxSpawned)
				break;
		}
	}

	public Larva summonLarva(Level world, Vec3 pos, float difficulty) {
		Larva larva = new Larva(ProgressiveBosses.LARVA, world);
		CompoundTag minionTags = ((IEntityExtraData) larva).getPersistentData();

		minionTags.putBoolean("mobspropertiesrandomness:processed", true);
		//TODO Scaling health

		larva.setPos(pos.x, pos.y, pos.z);
		larva.setPersistenceRequired();

		MCUtils.applyModifier(larva, Attributes.ATTACK_DAMAGE, Strings.AttributeModifiers.ATTACK_DAMAGE_BONUS_UUID, Strings.AttributeModifiers.ATTACK_DAMAGE_BONUS, 0.35 * difficulty, AttributeModifier.Operation.ADDITION);
		// MCUtils.applyModifier(larva, ForgeMod.SWIM_SPEED.get(), Strings.AttributeModifiers.SWIM_SPEED_BONUS_UUID, Strings.AttributeModifiers.SWIM_SPEED_BONUS, 2.5d, EntityAttributeModifier.Operation.MULTIPLY_BASE);

		world.addFreshEntity(larva);
		return larva;
	}
}